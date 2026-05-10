package com.mindcare.services;

import com.mindcare.model.Appointment;
import com.mindcare.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppointmentNotificationService {

    private static final Logger logger = Logger.getLogger(AppointmentNotificationService.class.getName());
    private static final DateTimeFormatter EMAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.FRENCH);

    private final AppointmentService appointmentService;
    private final MailService mailService;
    private final ZoomMeetingService zoomMeetingService;

    public AppointmentNotificationService(AppointmentService appointmentService) {
        this(appointmentService, new MailService(), new ZoomMeetingService());
    }

    AppointmentNotificationService(AppointmentService appointmentService, MailService mailService, 
                                    ZoomMeetingService zoomMeetingService) {
        this.appointmentService = appointmentService;
        this.mailService = mailService;
        this.zoomMeetingService = zoomMeetingService;
    }

    public void notifyNewAppointment(Appointment appointment) {
        sendAsync(() -> sendPendingApprovalEmail(appointment));
    }

    public void notifyAppointmentAccepted(Appointment appointment) {
        sendAsync(() -> sendAcceptanceEmail(appointment));
    }

    public void notifyAppointmentRejected(Appointment appointment) {
        sendAsync(() -> sendRejectionEmail(appointment));
    }

    public void notifyAppointmentCancelledByPsychologist(Appointment appointment) {
        sendAsync(() -> sendCancelledByPsychologistEmail(appointment));
    }

    public void notifyAppointmentCancelledByStudent(Appointment appointment) {
        sendAsync(() -> sendCancelledByStudentEmail(appointment));
    }

    public void notifyAppointmentReturnedToPending(Appointment appointment) {
        sendAsync(() -> sendPendingApprovalEmail(appointment));
    }

    public void notifyAppointmentTimeChangedByPsychologist(Appointment previous, Appointment current) {
        if (!hasDateTimeChanged(previous, current)) {
            return;
        }
        sendAsync(() -> sendTimeChangedByPsychologistEmail(previous, current));
    }

    public void notifyEditedAppointment(Appointment previous, Appointment current) {
        String previousStatus = normalizeStatus(previous == null ? null : previous.getStatus());
        String currentStatus = normalizeStatus(current == null ? null : current.getStatus());

        if (currentStatus.isBlank() || currentStatus.equals(previousStatus)) {
            return;
        }

        switch (currentStatus) {
            case "pending" -> notifyAppointmentReturnedToPending(current);
            case "accepted" -> notifyAppointmentAccepted(current);
            case "rejected" -> notifyAppointmentRejected(current);
            case "cancelled" -> notifyAppointmentCancelledByPsychologist(current);
            default -> {
            }
        }
    }

    private void sendAsync(Runnable task) {
        CompletableFuture.runAsync(task).exceptionally(exception -> {
            System.err.println("[AppointmentNotificationService] " + exception.getMessage());
            return null;
        });
    }

    private void sendPendingApprovalEmail(Appointment appointment) {
        if (appointment == null || appointment.getPsyId() == null) {
            return;
        }

        User psychologue = loadUser(appointment.getPsyId());
        if (psychologue == null || isBlank(psychologue.getEmail())) {
            return;
        }

        User student = appointment.getStudentId() == null ? null : loadUser(appointment.getStudentId());
        String subject = "Nouveau rendez-vous en attente d'approbation";
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(displayName(psychologue)).append(",\n\n");
        body.append("Un nouveau rendez-vous attend votre approbation.\n\n");
        appendAppointmentSummary(body, appointment, student, psychologue);
        body.append("\nMerci.\n");

        mailService.sendTextEmail(psychologue.getEmail(), subject, body.toString());
    }

    private void sendAcceptanceEmail(Appointment appointment) {
        if (appointment == null) {
            return;
        }

        User student = appointment.getStudentId() == null ? null : loadUser(appointment.getStudentId());
        User psychologue = appointment.getPsyId() == null ? null : loadUser(appointment.getPsyId());
        String zoomLink = isOnline(appointment) ? zoomMeetingService.createJoinUrl(appointment).orElse(null) : null;

        String subject = "Rendez-vous accepté";
        if (student != null && !isBlank(student.getEmail())) {
            mailService.sendTextEmail(student.getEmail(), subject, buildAcceptanceBody(appointment, student, student, psychologue, zoomLink));
        }
        if (psychologue != null && !isBlank(psychologue.getEmail())) {
            mailService.sendTextEmail(psychologue.getEmail(), subject, buildAcceptanceBody(appointment, psychologue, student, psychologue, zoomLink));
        }
    }

    private void sendRejectionEmail(Appointment appointment) {
        if (appointment == null || appointment.getStudentId() == null) {
            return;
        }

        User student = loadUser(appointment.getStudentId());
        User psychologue = appointment.getPsyId() == null ? null : loadUser(appointment.getPsyId());
        if (student == null || isBlank(student.getEmail())) {
            return;
        }

        String subject = "Rendez-vous refusé";
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(displayName(student)).append(",\n\n");
        body.append("Votre rendez-vous a été refusé.\n\n");
        appendAppointmentSummary(body, appointment, student, psychologue);
        body.append("\nMerci.\n");

        mailService.sendTextEmail(student.getEmail(), subject, body.toString());
    }

    private void sendCancelledByPsychologistEmail(Appointment appointment) {
        if (appointment == null || appointment.getStudentId() == null) {
            return;
        }

        User student = loadUser(appointment.getStudentId());
        User psychologue = appointment.getPsyId() == null ? null : loadUser(appointment.getPsyId());
        if (student == null || isBlank(student.getEmail())) {
            return;
        }

        String subject = "Rendez-vous annulé par le psychologue";
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(displayName(student)).append(",\n\n");
        body.append("Le psychologue a annulé le rendez-vous.\n\n");
        appendAppointmentSummary(body, appointment, student, psychologue);
        body.append("\nMerci.\n");

        mailService.sendTextEmail(student.getEmail(), subject, body.toString());
    }

    private void sendCancelledByStudentEmail(Appointment appointment) {
        if (appointment == null || appointment.getPsyId() == null) {
            return;
        }

        User psychologue = loadUser(appointment.getPsyId());
        User student = appointment.getStudentId() == null ? null : loadUser(appointment.getStudentId());
        if (psychologue == null || isBlank(psychologue.getEmail())) {
            return;
        }

        String subject = "Rendez-vous annulé par l'étudiant";
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(displayName(psychologue)).append(",\n\n");
        body.append("L'étudiant a annulé le rendez-vous.\n\n");
        appendAppointmentSummary(body, appointment, student, psychologue);
        body.append("\nMerci.\n");

        mailService.sendTextEmail(psychologue.getEmail(), subject, body.toString());
    }

    private void sendTimeChangedByPsychologistEmail(Appointment previous, Appointment current) {
        if (current == null || current.getStudentId() == null) {
            return;
        }

        User student = loadUser(current.getStudentId());
        User psychologue = current.getPsyId() == null ? null : loadUser(current.getPsyId());
        if (student == null || isBlank(student.getEmail())) {
            return;
        }

        String subject = "Changement d'horaire de rendez-vous";
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(displayName(student)).append(",\n\n");
        body.append("Le psychologue a modifie l'horaire de votre rendez-vous.\n\n");
        body.append("Ancienne date: ").append(formatDateTime(previous)).append("\n");
        body.append("Nouvelle date: ").append(formatDateTime(current)).append("\n\n");
        appendAppointmentSummary(body, current, student, psychologue);
        body.append("\nMerci.\n");

        mailService.sendTextEmail(student.getEmail(), subject, body.toString());
    }

    private String buildAcceptanceBody(Appointment appointment, User recipient, User student, User psychologue, String zoomLink) {
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(displayName(recipient)).append(",\n\n");
        body.append("Votre rendez-vous a été accepté.\n\n");
        appendAppointmentSummary(body, appointment, student, psychologue);
        if (zoomLink != null && !zoomLink.isBlank()) {
            body.append("\nLien Zoom: ").append(zoomLink).append("\n");
        }
        body.append("\nMerci.\n");
        return body.toString();
    }

    private void appendAppointmentSummary(StringBuilder body, Appointment appointment, User student, User psychologue) {
        body.append("Date: ").append(formatDateTime(appointment)).append("\n");
        body.append("Lieu: ").append(emptySafe(appointment.getLocation())).append("\n");
        body.append("Statut: ").append(emptySafe(appointment.getStatus())).append("\n");
        body.append("Étudiant: ").append(displayName(student)).append("\n");
        body.append("Psychologue: ").append(displayName(psychologue)).append("\n");
        if (!isBlank(appointment.getDescription())) {
            body.append("Description: ").append(appointment.getDescription().trim()).append("\n");
        }
    }

    private String formatDateTime(Appointment appointment) {
        if (appointment == null || appointment.getDateTime() == null) {
            return "N/A";
        }
        return appointment.getDateTime().format(EMAIL_DATE_FORMAT);
    }

    private String displayName(User user) {
        if (user == null) {
            return "N/A";
        }

        String fullName = (emptySafe(user.getFirstName()) + " " + emptySafe(user.getLastName())).trim();
        return fullName.isBlank() ? "N/A" : fullName;
    }

    private User loadUser(Integer userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        return appointmentService.findUserById(userId);
    }

    private boolean isOnline(Appointment appointment) {
        return appointment != null && isOnline(appointment.getLocation());
    }

    private boolean isOnline(String location) {
        return location != null && location.trim().equalsIgnoreCase("online");
    }

    private boolean hasDateTimeChanged(Appointment previous, Appointment current) {
        LocalDateTime previousDateTime = previous == null ? null : previous.getDateTime();
        LocalDateTime currentDateTime = current == null ? null : current.getDateTime();
        if (previousDateTime == null && currentDateTime == null) {
            return false;
        }
        if (previousDateTime == null || currentDateTime == null) {
            return true;
        }
        return !previousDateTime.equals(currentDateTime);
    }

    private String normalizeStatus(String status) {
        return emptySafe(status).trim().toLowerCase(Locale.ROOT);
    }

    private String emptySafe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}