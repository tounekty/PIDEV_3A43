package org.example.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.*;
import jakarta.mail.util.ByteArrayDataSource;

import org.example.config.ConfigurationManager;
import org.example.event.Event;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 📧 Smart Email Service — Smart Ticket Digital
 *
 * Hiérarchie visuelle :
 *   1. Nom de l'événement + Badge confirmé
 *   2. QR Code (élément principal)
 *   3. Date / Lieu / Catégorie
 *   4. Bouton Google Maps (pleine largeur)
 */
public class EmailService {

    private final String smtpHost;
    private final int    smtpPort;
    private final String senderEmail;
    private final String senderPass;
    private final String senderName;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final QRCodeService qrCodeService = new QRCodeService();

    public EmailService() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        this.smtpHost    = config.getMailSmtpHost();
        this.smtpPort    = config.getMailSmtpPort();
        this.senderEmail = config.getMailUsername();
        this.senderPass  = config.getMailPassword();
        this.senderName  = config.getMailFromName();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    // Champ temporaire pour passer les bytes QR de buildEmail à sendEmail
    private byte[] lastQrImageBytes = null;

    public void sendReservationConfirmation(String toEmail, String studentName,
                                            Event event, String status,
                                            Integer waitlistPos,
                                            int reservationId, int userId,
                                            int confirmedCount) {
        try {
            String subject = buildSubject(status, event);
            lastQrImageBytes = null;
            String body = buildEmail(studentName, event, status, waitlistPos,
                                     reservationId, userId, confirmedCount);
            sendEmailWithQR(toEmail, subject, body, lastQrImageBytes);
        } catch (Exception e) {
            System.err.println("[EmailService] " + e.getMessage());
        }
    }

    public void sendReservationConfirmation(String toEmail, String studentName,
                                            Event event, String status,
                                            Integer waitlistPos) {
        sendReservationConfirmation(toEmail, studentName, event, status, waitlistPos, 0, 0, 0);
    }

    public void sendRawEmail(String toEmail, String subject, String htmlBody) {
        try { sendEmail(toEmail, subject, htmlBody); }
        catch (Exception e) { System.err.println("[EmailService] raw: " + e.getMessage()); }
    }

    // ── Email builder ─────────────────────────────────────────────────────────

    private String buildEmail(String studentName, Event event, String status,
                               Integer waitlistPos, int reservationId,
                               int userId, int confirmedCount) {

        boolean isConfirmed = "CONFIRMED".equals(status);
        String dateStr = event.getDateEvent() != null ? event.getDateEvent().format(FMT) : "—";
        String lieu    = event.getLieu()     != null ? event.getLieu()     : "—";
        String cat     = event.getCategorie()!= null ? event.getCategorie(): "général";
        String titre   = event.getTitre()    != null ? event.getTitre()    : "Événement";

        // ── QR Code (élément principal) ──
        String qrHtml = "";
        byte[] qrImageBytes = null;
        if (isConfirmed && reservationId > 0 && userId > 0 && event.getDateEvent() != null) {
            try {
                // Générer le QR payload (petit et simple)
                String payload = qrCodeService.generateQRPayload(
                        reservationId, userId, event.getId(), event.getDateEvent());

                // Générer le QR localement via ZXing (sans internet)
                String ticketUrl = qrCodeService.generateTicketUrl(payload);
                qrImageBytes = qrCodeService.generateQRBytes(ticketUrl, 280);

                if (qrImageBytes == null || qrImageBytes.length == 0) {
                    // Fallback: quickchart.io si ZXing échoue
                    String httpUrl = qrCodeService.generateTicketHttpUrl(payload);
                    String qrImageUrl = qrCodeService.generateQRImageUrl(httpUrl, 280);
                    java.net.URL qrUrl = new java.net.URL(qrImageUrl);
                    java.io.InputStream is = qrUrl.openStream();
                    qrImageBytes = is.readAllBytes();
                    is.close();
                }

                if (qrImageBytes != null && qrImageBytes.length > 0) {
                    lastQrImageBytes = qrImageBytes;
                }
            } catch (Exception e) {
                System.err.println("[EmailService] QR generation error: " + e.getMessage());
                qrImageBytes = null;
            }
            
            qrHtml =
                "<tr><td style='padding:0 20px 48px;'>" +
                "<div style='background:white;border-radius:24px;padding:36px 24px 32px;" +
                "text-align:center;box-shadow:0 12px 48px rgba(108,78,255,0.15);" +
                "border:1px solid #f0f0f8;'>" +
                "<p style='margin:0 0 8px;font-size:12px;font-weight:800;color:#6c4eff;" +
                "letter-spacing:4px;text-transform:uppercase;'>🎟️ Votre billet</p>" +
                "<p style='margin:0 0 28px;font-size:13px;color:#b0b9c3;'>Réservation #" + reservationId + "</p>" +
                "<div style='display:inline-block;background:#f9f9fc;padding:20px;" +
                "border-radius:20px;box-shadow:0 6px 32px rgba(0,0,0,0.1);" +
                "border:2px solid #ede9fe;'>" +
                (qrImageBytes != null && qrImageBytes.length > 0
                    ? "<img src='cid:qrcode' width='270' height='270' style='display:block;border-radius:12px;'/>"
                    : "<div style='width:270px;height:270px;background:#f0f0f0;border-radius:12px;display:flex;align-items:center;justify-content:center;'><span style='color:#999;font-size:14px;'>QR généré</span></div>") +
                "</div>" +
                "<p style='margin:28px 0 0;font-size:14px;color:#6c4eff;font-weight:700;" +
                "letter-spacing:0.5px;'>Scannez pour voir votre billet</p>" +
                "<p style='margin:4px 0 0;font-size:12px;color:#9ca3af;'>Fonctionne sur tous les réseaux</p>" +
                "</div></td></tr>";
        }

        // ── Status badge ──
        String badge = isConfirmed
            ? "<span style='display:inline-block;background:#d1fae5;color:#065f46;" +
              "padding:8px 22px;border-radius:999px;font-size:13px;font-weight:700;" +
              "letter-spacing:0.5px;'>✅ Réservation confirmée</span>"
            : "<span style='display:inline-block;background:#fef3c7;color:#92400e;" +
              "padding:8px 22px;border-radius:999px;font-size:13px;font-weight:700;'>" +
              "⏳ Liste d'attente #" + (waitlistPos != null ? waitlistPos : 1) + "</span>";

        // ── Maps URL ──
        String mapsUrl = "https://www.google.com/maps/search/?api=1&query="
            + java.net.URLEncoder.encode(lieu, java.nio.charset.StandardCharsets.UTF_8);

        // ── Tip ──
        String tip = buildCategoryTip(cat);

        return
            "<!DOCTYPE html><html lang='fr'><head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap' rel='stylesheet'>" +
            "<style>" +
            "@keyframes fadeIn{from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:translateY(0)}}" +
            "@keyframes slideUp{from{opacity:0;transform:translateY(24px)}to{opacity:1;transform:translateY(0)}}" +
            ".cta:hover{background:linear-gradient(135deg,#5a3de8,#7c5cfc)!important;" +
            "box-shadow:0 12px 32px rgba(108,78,255,0.5)!important;transform:translateY(-2px)!important;}" +
            "</style>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:#f9f9fc;font-family:Inter,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f9f9fc;padding:40px 16px;'>" +
            "<tr><td align='center'>" +
            "<table width='540' cellpadding='0' cellspacing='0' " +
            "style='background:white;border-radius:28px;overflow:hidden;" +
            "box-shadow:0 16px 56px rgba(108,78,255,0.12);animation:fadeIn 0.5s ease;'>" +
            "<tr><td style='background:linear-gradient(135deg,#1E40AF 0%,#2563EB 50%,#0EA5E9 100%);" +
            "padding:48px 36px 40px;text-align:center;" +
            "box-shadow:inset 0 -4px 20px rgba(0,0,0,0.1);'>" +
            "<p style='margin:0 0 12px;font-size:11px;font-weight:800;color:rgba(255,255,255,0.9);" +
            "letter-spacing:5px;text-transform:uppercase;'>🎪 Votre Événement</p>" +
            "<h1 style='margin:0 0 10px;font-size:32px;font-weight:900;color:white;" +
            "line-height:1.2;word-wrap:break-word;" +
            "text-shadow:0 2px 16px rgba(0,0,0,0.2);'>" + titre + "</h1>" +
            "<p style='margin:0 0 22px;font-size:15px;color:rgba(255,255,255,0.92);" +
            "font-weight:600;letter-spacing:0.5px;'>" + lieu + "</p>" +
            "<div style='text-align:center;'>" + badge + "</div>" +
            "</td></tr>" +
            "<tr><td style='padding:32px 36px 16px;'>" +
            "<p style='margin:0;font-size:16px;color:#4b5563;line-height:1.8;'>" +
            "Bonjour <strong style='color:#1a1a2e;font-weight:700;'>" + studentName + "</strong>,</p>" +
            "<p style='margin:8px 0 0;font-size:15px;color:#6b7280;line-height:1.8;'>" +
            "Votre place est confirmée. Découvrez votre billet numérique ci-dessous.</p>" +
            "</td></tr>" +
            qrHtml +
            "<tr><td style='padding:0 28px 40px;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'>" +
            "<tr>" +
            infoCard("📅", dateStr) +
            infoCard("📍", lieu) +
            infoCard("🏷️", cat) +
            "</tr>" +
            "</table></td></tr>" +
            "<tr><td style='padding:0 28px 28px;'>" +
            "<div style='background:#f3fffe;border-radius:16px;padding:18px 20px;" +
            "border:1px solid #d1f5f0;border-left:4px solid #10b981;'>" +
            "<p style='margin:0;font-size:14px;color:#065f46;line-height:1.8;'>" +
            "<strong style='color:#10b981;font-weight:700;'>💡 Conseil :</strong> " + tip + "</p>" +
            "</div></td></tr>" +
            "<tr><td style='padding:0 28px 40px;animation:slideUp 0.7s ease;'>" +
            "<a href='" + mapsUrl + "' target='_blank' class='cta' " +
            "style='display:block;background:linear-gradient(135deg,#6c4eff,#7c5cfc);" +
            "color:white;text-decoration:none;text-align:center;padding:18px 24px;" +
            "border-radius:16px;font-size:16px;font-weight:700;letter-spacing:0.4px;" +
            "box-shadow:0 8px 24px rgba(108,78,255,0.35);transition:all 0.2s ease;'>" +
            "📍 Ouvrir l'itinéraire</a>" +
            "</td></tr>" +
            "<tr><td style='background:#f9f9fc;padding:24px 36px;text-align:center;" +
            "border-top:1px solid #ede9fe;'>" +
            "<p style='margin:0;font-size:12px;color:#9ca3af;line-height:1.9;'>" +
            "Email automatique · <strong style='color:#6c4eff;font-weight:700;'>MindCare Events</strong> 2026<br>" +
            "<span style='font-size:11px;'>Cet email contient votre billet numérique</span></p>" +
            "</td></tr>" +
            "</table></td></tr></table></body></html>";
    }

    private String infoCard(String icon, String value) {
        return
            "<td style='width:33%;padding:6px;vertical-align:top;'>" +
            "<div style='background:white;border-radius:16px;padding:18px 14px;" +
            "text-align:center;border:1px solid #e5e7eb;background:linear-gradient(135deg,#fafafa,#ffffff);" +
            "box-shadow:0 2px 12px rgba(108,78,255,0.06);transition:all 0.2s;'>" +
            "<div style='font-size:28px;margin-bottom:8px;'>" + icon + "</div>" +
            "<p style='margin:0;font-size:13px;color:#1a1a2e;font-weight:600;" +
            "overflow:hidden;text-overflow:ellipsis;white-space:nowrap;line-height:1.5;'>" +
            value + "</p>" +
            "</div></td>";
    }

    // ── Ticket page HTML (ouverte quand le QR est scanné) ────────────────────

    private String buildTicketPageHtml(int resId, String name, String titre,
                                        String date, String lieu, String cat) {
        return "<!DOCTYPE html><html><head><meta charset=UTF-8>"
            + "<meta name=viewport content='width=device-width,initial-scale=1,maximum-scale=1'>"
            + "<style>"
            + "body{margin:0;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;"
            + "background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);"
            + "min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}"
            + ".card{background:#fff;border-radius:24px;max-width:380px;width:100%;"
            + "box-shadow:0 20px 60px rgba(0,0,0,.25);overflow:hidden}"
            + ".top{background:linear-gradient(135deg,#2563EB,#1d4ed8);padding:28px 24px 24px;text-align:center}"
            + ".brain{font-size:32px;margin-bottom:4px}"
            + ".brand{font-size:22px;font-weight:900;color:#fff;margin:0}"
            + ".sub{font-size:13px;color:rgba(255,255,255,.8);margin:4px 0 16px}"
            + ".badge{display:inline-flex;align-items:center;gap:6px;background:#22c55e;"
            + "color:#fff;padding:8px 20px;border-radius:999px;font-size:13px;font-weight:800;"
            + "letter-spacing:.5px}"
            + ".body{padding:24px}"
            + ".titre{font-size:24px;font-weight:900;color:#111;margin:0 0 20px}"
            + ".row{display:flex;align-items:flex-start;gap:14px;padding:14px 0;"
            + "border-bottom:1px solid #f1f5f9}"
            + ".row:last-of-type{border:none}"
            + ".ic{font-size:22px;width:28px;flex-shrink:0;margin-top:2px}"
            + ".lbl{font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;letter-spacing:.5px}"
            + ".val{font-size:15px;color:#1e293b;font-weight:700;margin-top:2px}"
            + ".sep{border:none;border-top:2px dashed #e2e8f0;margin:4px 0}"
            + ".holder{background:#f8fafc;border-radius:14px;padding:16px 18px;margin-top:4px}"
            + ".hlbl{font-size:10px;font-weight:800;color:#94a3b8;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:8px}"
            + ".hname{display:flex;align-items:center;gap:8px;font-size:18px;font-weight:900;color:#1e293b}"
            + ".hnum{font-size:12px;color:#94a3b8;margin-top:4px}"
            + ".foot{background:#f8fafc;padding:14px;text-align:center;"
            + "font-size:11px;color:#94a3b8;border-top:1px solid #f1f5f9}"
            + "</style></head><body>"
            + "<div class=card>"
            + "<div class=top>"
            + "<div class=brain>🧠</div>"
            + "<p class=brand>MindCare Events</p>"
            + "<p class=sub>Ticket de réservation numérique</p>"
            + "<div class=badge>&#10003; &nbsp;TICKET VALIDE</div>"
            + "</div>"
            + "<div class=body>"
            + "<p class=titre>" + escapeHtml(titre) + "</p>"
            + "<div class=row><div class=ic>📅</div><div><div class=lbl>Date &amp; Heure</div>"
            + "<div class=val>" + escapeHtml(date) + "</div></div></div>"
            + "<div class=row><div class=ic>📍</div><div><div class=lbl>Lieu</div>"
            + "<div class=val>" + escapeHtml(lieu) + "</div></div></div>"
            + "<div class=row><div class=ic>🏷</div><div><div class=lbl>Catégorie</div>"
            + "<div class=val>" + escapeHtml(cat) + "</div></div></div>"
            + "<hr class=sep>"
            + "<div class=holder>"
            + "<div class=hlbl>Titulaire du billet</div>"
            + "<div class=hname>&#128100; &nbsp;" + escapeHtml(name) + "</div>"
            + "<div class=hnum>Réservation #" + resId + "</div>"
            + "</div>"
            + "</div>"
            + "<div class=foot>Présentez ce ticket à l'entrée de l'événement · MindCare Events 2026</div>"
            + "</div></body></html>";
    }

    // ── Mini ticket HTML (compact pour QR code) ──────────────────────────────

    private String buildMiniTicketHtml(int resId, String name, String titre,
                                        String date, String lieu, String cat) {
        return "<!DOCTYPE html><html><head><meta charset=UTF-8>"
            + "<meta name=viewport content='width=device-width,initial-scale=1,maximum-scale=1'>"
            + "<style>"
            + "body{margin:0;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;"
            + "background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);"
            + "min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}"
            + ".card{background:#fff;border-radius:24px;max-width:380px;width:100%;"
            + "box-shadow:0 20px 60px rgba(0,0,0,.25);overflow:hidden}"
            + ".top{background:linear-gradient(135deg,#2563EB,#1d4ed8);padding:28px 24px 24px;text-align:center}"
            + ".brain{font-size:32px;margin-bottom:4px}"
            + ".brand{font-size:22px;font-weight:900;color:#fff;margin:0}"
            + ".sub{font-size:13px;color:rgba(255,255,255,.8);margin:4px 0 16px}"
            + ".badge{display:inline-flex;align-items:center;gap:6px;background:#22c55e;"
            + "color:#fff;padding:8px 20px;border-radius:999px;font-size:13px;font-weight:800;"
            + "letter-spacing:.5px}"
            + ".body{padding:24px}"
            + ".titre{font-size:24px;font-weight:900;color:#111;margin:0 0 20px}"
            + ".row{display:flex;align-items:flex-start;gap:14px;padding:14px 0;"
            + "border-bottom:1px solid #f1f5f9}"
            + ".row:last-of-type{border:none}"
            + ".ic{font-size:22px;width:28px;flex-shrink:0;margin-top:2px}"
            + ".lbl{font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;letter-spacing:.5px}"
            + ".val{font-size:15px;color:#1e293b;font-weight:700;margin-top:2px}"
            + ".sep{border:none;border-top:2px dashed #e2e8f0;margin:4px 0}"
            + ".holder{background:#f8fafc;border-radius:14px;padding:16px 18px;margin-top:4px}"
            + ".hlbl{font-size:10px;font-weight:800;color:#94a3b8;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:8px}"
            + ".hname{display:flex;align-items:center;gap:8px;font-size:18px;font-weight:900;color:#1e293b}"
            + ".hnum{font-size:12px;color:#94a3b8;margin-top:4px}"
            + ".foot{background:#f8fafc;padding:14px;text-align:center;"
            + "font-size:11px;color:#94a3b8;border-top:1px solid #f1f5f9}"
            + "</style></head><body>"
            + "<div class=card>"
            + "<div class=top>"
            + "<div class=brain>🧠</div>"
            + "<p class=brand>MindCare Events</p>"
            + "<p class=sub>Ticket de réservation numérique</p>"
            + "<div class=badge>✓ &nbsp;TICKET VALIDE</div>"
            + "</div>"
            + "<div class=body>"
            + "<p class=titre>" + escapeHtml(titre) + "</p>"
            + "<div class=row><div class=ic>📅</div><div><div class=lbl>Date &amp; Heure</div>"
            + "<div class=val>" + escapeHtml(date) + "</div></div></div>"
            + "<div class=row><div class=ic>📍</div><div><div class=lbl>Lieu</div>"
            + "<div class=val>" + escapeHtml(lieu) + "</div></div></div>"
            + "<div class=row><div class=ic>🏷</div><div><div class=lbl>Catégorie</div>"
            + "<div class=val>" + escapeHtml(cat) + "</div></div></div>"
            + "<hr class=sep>"
            + "<div class=holder>"
            + "<div class=hlbl>Titulaire du billet</div>"
            + "<div class=hname>👤 &nbsp;" + escapeHtml(name) + "</div>"
            + "<div class=hnum>Réservation #" + resId + "</div>"
            + "</div>"
            + "</div>"
            + "<div class=foot>Présentez ce ticket à l'entrée de l'événement · MindCare Events 2026</div>"
            + "</div></body></html>";
    }

    // ── Standalone ticket HTML (encodé dans le QR) ───────────────────────────

    private String buildStandaloneTicketHtml(int reservationId, String name,
                                              String titre, String date,
                                              String lieu, String cat) {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>Billet MindCare</title>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box;}" +
            "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;" +
            "background:linear-gradient(135deg,#667eea,#764ba2);min-height:100vh;" +
            "display:flex;align-items:center;justify-content:center;padding:20px;}" +
            ".ticket{background:white;border-radius:24px;max-width:380px;width:100%;" +
            "overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,0.3);}" +
            ".header{background:linear-gradient(135deg,#1E40AF,#2563EB,#0EA5E9);" +
            "padding:28px 24px;text-align:center;}" +
            ".header .brand{font-size:11px;font-weight:800;color:rgba(255,255,255,0.8);" +
            "letter-spacing:4px;text-transform:uppercase;margin-bottom:8px;}" +
            ".header h1{font-size:22px;font-weight:900;color:white;line-height:1.3;}" +
            ".badge{display:inline-block;background:#d1fae5;color:#065f46;" +
            "padding:6px 18px;border-radius:999px;font-size:12px;font-weight:700;" +
            "margin-top:12px;}" +
            ".body{padding:24px;}" +
            ".res-num{text-align:center;margin-bottom:20px;}" +
            ".res-num .label{font-size:11px;color:#9ca3af;font-weight:600;letter-spacing:2px;text-transform:uppercase;}" +
            ".res-num .num{font-size:32px;font-weight:900;color:#1E40AF;}" +
            ".info-row{display:flex;align-items:center;gap:12px;padding:10px 0;" +
            "border-bottom:1px solid #f3f4f6;}" +
            ".info-row:last-child{border-bottom:none;}" +
            ".info-icon{font-size:20px;width:32px;text-align:center;}" +
            ".info-text .info-label{font-size:10px;color:#9ca3af;font-weight:600;text-transform:uppercase;}" +
            ".info-text .info-value{font-size:14px;color:#111827;font-weight:700;}" +
            ".footer{background:#f9fafb;padding:16px 24px;text-align:center;" +
            "border-top:1px solid #f3f4f6;}" +
            ".footer p{font-size:11px;color:#9ca3af;}" +
            ".footer strong{color:#6c4eff;}" +
            ".divider{border:none;border-top:2px dashed #e5e7eb;margin:16px 0;}" +
            "</style></head><body>" +
            "<div class='ticket'>" +
            "<div class='header'>" +
            "<div class='brand'>🎪 MindCare Events</div>" +
            "<h1>" + escapeHtml(titre) + "</h1>" +
            "<div class='badge'>✅ Réservation confirmée</div>" +
            "</div>" +
            "<div class='body'>" +
            "<div class='res-num'>" +
            "<div class='label'>Numéro de réservation</div>" +
            "<div class='num'>#" + reservationId + "</div>" +
            "</div>" +
            "<hr class='divider'>" +
            "<div class='info-row'><div class='info-icon'>👤</div>" +
            "<div class='info-text'><div class='info-label'>Participant</div>" +
            "<div class='info-value'>" + escapeHtml(name) + "</div></div></div>" +
            "<div class='info-row'><div class='info-icon'>📅</div>" +
            "<div class='info-text'><div class='info-label'>Date</div>" +
            "<div class='info-value'>" + escapeHtml(date) + "</div></div></div>" +
            "<div class='info-row'><div class='info-icon'>📍</div>" +
            "<div class='info-text'><div class='info-label'>Lieu</div>" +
            "<div class='info-value'>" + escapeHtml(lieu) + "</div></div></div>" +
            "<div class='info-row'><div class='info-icon'>🏷️</div>" +
            "<div class='info-text'><div class='info-label'>Catégorie</div>" +
            "<div class='info-value'>" + escapeHtml(cat) + "</div></div></div>" +
            "</div>" +
            "<div class='footer'>" +
            "<p>Billet numérique · <strong>MindCare Events</strong> 2026</p>" +
            "<p style='margin-top:4px;'>Présentez ce billet à l'entrée</p>" +
            "</div>" +
            "</div>" +
            "</body></html>";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildSubject(String status, Event event) {
        return "WAITLISTED".equals(status)
                ? "⏳ Liste d'attente — " + event.getTitre()
                : "✅ Confirmation — " + event.getTitre() + " 🎟️";
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
                    "Soyez à l'heure et prêt(e) à participer ! 🎯";
        };
    }

    // ── SMTP sender ───────────────────────────────────────────────────────────

    private void sendEmailWithQR(String toEmail, String subject, String htmlBody,
                                  byte[] qrBytes) throws Exception {
        if (senderEmail == null || senderEmail.isEmpty())
            throw new Exception("Sender email not configured");
        if (toEmail == null || toEmail.isEmpty())
            throw new Exception("Recipient email is empty");

        Properties props = new Properties();
        props.put("mail.smtp.host",             smtpHost);
        props.put("mail.smtp.port",             "465");
        props.put("mail.smtp.auth",             "true");
        props.put("mail.smtp.ssl.enable",       "true");
        props.put("mail.smtp.ssl.trust",        smtpHost);
        props.put("mail.smtp.connectiontimeout","10000");
        props.put("mail.smtp.timeout",          "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPass);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail, senderName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        if (qrBytes != null && qrBytes.length > 0) {
            // Email multipart avec image QR en pièce jointe inline (CID)
            MimeMultipart multipart = new MimeMultipart("related");

            // Partie HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Partie image QR (CID)
            MimeBodyPart imgPart = new MimeBodyPart();
            imgPart.setDataHandler(new DataHandler(new ByteArrayDataSource(qrBytes, "image/png")));
            imgPart.setHeader("Content-ID", "<qrcode>");
            imgPart.setDisposition(MimeBodyPart.INLINE);
            imgPart.setFileName("qrcode.png");
            multipart.addBodyPart(imgPart);

            message.setContent(multipart);
        } else {
            message.setContent(htmlBody, "text/html; charset=UTF-8");
        }

        Transport.send(message);
        System.out.println("[EmailService] Email envoyé à " + toEmail);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) throws Exception {
        if (senderEmail == null || senderEmail.isEmpty()) {
            throw new Exception("Sender email not configured");
        }
        if (senderPass == null || senderPass.isEmpty()) {
            throw new Exception("Sender password not configured");
        }
        if (toEmail == null || toEmail.isEmpty()) {
            throw new Exception("Recipient email is empty");
        }

        // Port 465 SSL — port 587 bloqué sur certains réseaux
        Properties props = new Properties();
        props.put("mail.smtp.host",             smtpHost);
        props.put("mail.smtp.port",             "465");
        props.put("mail.smtp.auth",             "true");
        props.put("mail.smtp.ssl.enable",       "true");
        props.put("mail.smtp.ssl.trust",        smtpHost);
        props.put("mail.smtp.connectiontimeout","10000");
        props.put("mail.smtp.timeout",          "10000");
        props.put("mail.smtp.writetimeout",     "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPass);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail, senderName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
        System.out.println("[EmailService] Email envoyé à " + toEmail);
    }
}
