package org.example.service;

import org.example.model.Mood;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class MoodSupportEmailService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    public MoodSupportEmailService() {
        this.emailService = new EmailService();
    }

    public void sendSupportEmailToStudent(String studentEmail, Mood mood, String adminMessage) throws IOException {
        if (studentEmail == null || studentEmail.isBlank()) {
            throw new IOException("Student email is required.");
        }
        if (mood == null) {
            throw new IOException("Mood is required.");
        }

        String subject = "MindCare support suggestions";
        String message = adminMessage == null || adminMessage.isBlank() ? defaultSupportMessage(mood) : adminMessage.trim();

        String body = "MindCare support message\n\n"
                + "An admin reviewed your mood entry and shared these suggestions:\n\n"
                + message + "\n\n"
                + "Related mood: " + safe(mood.getMoodType())
                + " on " + (mood.getMoodDate() == null ? "" : mood.getMoodDate().format(DATE_FMT))
                + "\n\n"
                + "If you feel unsafe or overwhelmed, contact a trusted person, campus support, or emergency services.";

        emailService.sendPlainTextEmail(studentEmail.trim(), subject, body);
    }

    private String defaultSupportMessage(Mood mood) {
        String lower = mood.getMoodType() == null ? "" : mood.getMoodType().toLowerCase();
        if (lower.contains("sad") || lower.contains("depress") || lower.contains("anxious") || lower.contains("stress")) {
            return "Please consider talking to a therapist, counselor, or trusted support person. Start with one small action today: rest, write down what feels heavy, and ask for help if the feeling continues.";
        }
        if (lower.contains("happy") || lower.contains("good") || lower.contains("grateful")) {
            return "Your mood looks positive. Try to notice what helped today and repeat one part of it tomorrow.";
        }
        return "Please take a few minutes to reflect on what you need next. If this feeling repeats, consider reaching out to a trusted person or professional support.";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
