package org.example.util;

import org.example.config.DatabaseConnection;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ⏰ Notification Scheduler — rappels automatiques par email.
 *
 * Fonctionnalités :
 *   - Rappel 24h avant l'événement
 *   - Rappel 1h avant l'événement
 *   - Email de feedback après l'événement
 *
 * Lecture seule sur la DB — n'écrit rien.
 * Tourne en arrière-plan dans un thread daemon.
 */
public class NotificationScheduler {

    private final EmailService      emailService      = new EmailService();
    private final EventService      eventService      = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final QRCodeService     qrCodeService     = new QRCodeService();
    private final SmsService        smsService        = new SmsService();

    private ScheduledExecutorService scheduler;
    private boolean running = false;

    /**
     * Envoi manuel de rappels 24h — appelé par l'admin depuis l'interface.
     * Envoie un email + SMS à tous les participants confirmés de l'événement.
     * @return nombre de participants notifiés
     */
    public int sendManualReminder24h(org.example.event.Event event) {
        try {
            List<ReservationRecord> participants = reservationService.getParticipantsByEvent(event.getId());
            int count = 0;
            for (ReservationRecord r : participants) {
                String email = getUserEmail(r.getUserId());
                String phone = getUserPhone(r.getUserId());
                long hoursUntil = event.getDateEvent() != null
                        ? ChronoUnit.HOURS.between(LocalDateTime.now(), event.getDateEvent()) : 24;

                String subject = "⏰ Rappel 24h — " + event.getTitre();
                String body    = buildReminderEmail(r.getUsername(), event, "24h", hoursUntil);

                if (email != null && !email.isBlank()) {
                    new Thread(() -> emailService.sendRawEmail(email, subject, body),
                            "admin-reminder-email-" + r.getId()).start();
                    count++;
                }
                if (phone != null && !phone.isBlank()) {
                    String smsText = buildSmsText(r.getUsername(), event, r.getId());
                    new Thread(() -> smsService.sendSMS(phone, smsText),
                            "admin-reminder-sms-" + r.getId()).start();
                }
            }
            System.out.println("[NotificationScheduler] Rappels manuels envoyés pour: "
                    + event.getTitre() + " (" + count + " participants)");
            return count;
        } catch (Exception e) {
            System.err.println("[NotificationScheduler] sendManualReminder24h: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Démarre le scheduler — vérifie toutes les 30 minutes.
     */
    public void start() {
        if (running) return;
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-scheduler");
            t.setDaemon(true); // ne bloque pas la fermeture de l'app
            return t;
        });
        // Run immediately then every 30 minutes
        scheduler.scheduleAtFixedRate(this::checkAndSendReminders, 0, 30, TimeUnit.MINUTES);
        System.out.println("[NotificationScheduler] Démarré — vérification toutes les 30 min.");
    }

    /**
     * Arrête le scheduler proprement.
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            running = false;
        }
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private void checkAndSendReminders() {
        try {
            List<Event> upcomingEvents = eventService.getUpcomingEvents();
            LocalDateTime now = LocalDateTime.now();

            for (Event event : upcomingEvents) {
                LocalDateTime eventTime = event.getDateEvent();
                long hoursUntil = ChronoUnit.HOURS.between(now, eventTime);

                // 24h reminder window: between 24h and 23h before
                if (hoursUntil >= 23 && hoursUntil <= 24) {
                    sendRemindersForEvent(event, "24h", hoursUntil);
                }

                // 1h reminder window: between 1h and 50min before
                if (hoursUntil == 1) {
                    sendRemindersForEvent(event, "1h", hoursUntil);
                }

                // Post-event feedback: 2h after event ended
                long hoursSince = ChronoUnit.HOURS.between(eventTime, now);
                if (hoursSince >= 2 && hoursSince <= 3) {
                    sendFeedbackRequests(event);
                }
            }
        } catch (Exception e) {
            System.err.println("[NotificationScheduler] Erreur: " + e.getMessage());
        }
    }

    private void sendRemindersForEvent(Event event, String timing, long hoursUntil) {
        try {
            List<ReservationRecord> participants = reservationService.getParticipantsByEvent(event.getId());
            for (ReservationRecord r : participants) {
                String email = getUserEmail(r.getUserId());
                String phone = getUserPhone(r.getUserId());

                String subject = "⏰ Rappel " + timing + " — " + event.getTitre();
                String body = buildReminderEmail(r.getUsername(), event, timing, hoursUntil);

                // Email (existant)
                if (email != null && !email.isBlank()) {
                    new Thread(() -> emailService.sendRawEmail(email, subject, body),
                            "reminder-email-" + r.getId()).start();
                }

                // SMS Twilio — uniquement rappel 24h avec numéro de réservation
                if ("24h".equals(timing) && phone != null && !phone.isBlank()) {
                    String smsText = buildSmsText(r.getUsername(), event, r.getId());
                    new Thread(() -> smsService.sendSMS(phone, smsText),
                            "reminder-sms-" + r.getId()).start();
                }
            }
            System.out.println("[NotificationScheduler] Rappels " + timing
                    + " envoyés pour: " + event.getTitre());
        } catch (Exception e) {
            System.err.println("[NotificationScheduler] Rappel " + timing + ": " + e.getMessage());
        }
    }

    private void sendFeedbackRequests(Event event) {
        try {
            List<ReservationRecord> participants = reservationService.getParticipantsByEvent(event.getId());
            for (ReservationRecord r : participants) {
                String email = getUserEmail(r.getUserId());
                if (email == null || email.isBlank()) continue;

                String subject = "⭐ Donnez votre avis — " + event.getTitre();
                String body = buildFeedbackEmail(r.getUsername(), event);

                new Thread(() -> emailService.sendRawEmail(email, subject, body),
                        "feedback-" + r.getId()).start();
            }
        } catch (Exception e) {
            System.err.println("[NotificationScheduler] Feedback: " + e.getMessage());
        }
    }

    // ── Email templates ───────────────────────────────────────────────────────

    private String buildReminderEmail(String username, Event event, String timing, long hours) {
        String urgency = "1h".equals(timing)
                ? "🔴 Dans 1 heure !"
                : "🟡 Dans 24 heures";

        String tip = buildCategoryTip(event.getCategorie());

        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:30px;">
                  <div style="max-width:560px;margin:auto;background:white;border-radius:18px;overflow:hidden;
                              box-shadow:0 4px 24px rgba(46,94,166,0.12);">
                    <div style="background:linear-gradient(to right,#f59e0b,#fbbf24);padding:24px 32px;">
                      <h1 style="color:white;margin:0;font-size:20px;">⏰ Rappel — %s</h1>
                      <p style="color:rgba(255,255,255,0.9);margin:6px 0 0;">%s</p>
                    </div>
                    <div style="padding:28px 32px;">
                      <p style="color:#0f2942;font-size:16px;">Bonjour <strong>%s</strong>,</p>
                      <p style="color:#415a78;">Votre événement approche !</p>
                      <div style="background:#f8fbff;border:1px solid #d7e7ff;border-radius:12px;padding:18px;margin:16px 0;">
                        <h2 style="color:#0f2942;margin:0 0 12px;font-size:18px;">%s</h2>
                        <p style="margin:4px 0;color:#415a78;">📅 %s</p>
                        <p style="margin:4px 0;color:#415a78;">📍 %s</p>
                      </div>
                      <div style="background:#fffbeb;border-left:4px solid #f59e0b;padding:14px;border-radius:8px;margin:16px 0;">
                        <strong style="color:#92400e;">💡 Conseil :</strong>
                        <p style="color:#78350f;margin:4px 0;">%s</p>
                      </div>
                    </div>
                    <div style="background:#f0f4ff;padding:16px 32px;text-align:center;color:#9ab0cc;font-size:12px;">
                      MindCare Events — Rappel automatique
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                event.getTitre(), urgency, username,
                event.getTitre(),
                event.getDateEvent() != null ? event.getDateEvent().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—",
                event.getLieu() != null ? event.getLieu() : "—",
                tip
        );
    }

    private String buildFeedbackEmail(String username, Event event) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:30px;">
                  <div style="max-width:560px;margin:auto;background:white;border-radius:18px;overflow:hidden;
                              box-shadow:0 4px 24px rgba(46,94,166,0.12);">
                    <div style="background:linear-gradient(to right,#8b5cf6,#a78bfa);padding:24px 32px;">
                      <h1 style="color:white;margin:0;font-size:20px;">⭐ Comment s'est passé l'événement ?</h1>
                    </div>
                    <div style="padding:28px 32px;">
                      <p style="color:#0f2942;font-size:16px;">Bonjour <strong>%s</strong>,</p>
                      <p style="color:#415a78;">Nous espérons que vous avez apprécié <strong>%s</strong> !</p>
                      <p style="color:#415a78;">Votre avis nous aide à améliorer nos événements.</p>
                      <div style="text-align:center;margin:24px 0;">
                        <p style="color:#415a78;font-size:14px;">Notez votre expérience :</p>
                        <p style="font-size:32px;margin:8px 0;">⭐⭐⭐⭐⭐</p>
                        <p style="color:#9ab0cc;font-size:12px;">Connectez-vous à l'application pour laisser votre avis</p>
                      </div>
                    </div>
                    <div style="background:#f0f4ff;padding:16px 32px;text-align:center;color:#9ab0cc;font-size:12px;">
                      MindCare Events — Feedback automatique
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(username, event.getTitre());
    }

    private String buildCategoryTip(String category) {
        if (category == null) return "Soyez à l'heure et prêt(e) !";
        return switch (category.toLowerCase()) {
            case "yoga", "wellness", "meditation" ->
                    "Portez des vêtements confortables et apportez votre tapis 🧘";
            case "sport" ->
                    "Hydratez-vous bien avant l'événement 💧";
            case "conference" ->
                    "Arrivez 15 minutes en avance pour vous installer ⏱";
            case "atelier", "workshop" ->
                    "Préparez votre PC et vos outils 💻";
            default ->
                    "Soyez à l'heure et prêt(e) à participer !";
        };
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private String getUserEmail(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return username.contains("@") ? username : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificationScheduler] getUserEmail: " + e.getMessage());
        }
        return null;
    }

    private String getUserPhone(int userId) {
        // Cherche un champ phone dans users si disponible
        String sql = "SELECT phone FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("phone");
                }
            }
        } catch (SQLException e) {
            // Colonne phone inexistante — normal si pas encore ajoutée
        }
        return null;
    }

    private String buildSmsText(String username, Event event, int reservationId) {
        String dateStr = event.getDateEvent() != null
                ? event.getDateEvent().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—";
        String lieu = event.getLieu() != null ? event.getLieu() : "—";
        String titre = event.getTitre() != null ? event.getTitre() : "Evenement";

        String msg = "MindCare Events\n" +
                "Bonjour " + username + "!\n" +
                "Rappel 24h: " + titre + "\n" +
                "Date: " + dateStr + "\n" +
                "Lieu: " + lieu + "\n" +
                "Reservation #" + reservationId + "\n" +
                "A demain!";

        // Limiter à 160 caractères
        return msg.length() > 160 ? msg.substring(0, 157) + "..." : msg;
    }
}
