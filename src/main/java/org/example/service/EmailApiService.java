package org.example.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.config.AppConfig;
import org.example.model.Mood;

import java.util.Locale;
import java.util.Properties;

public class EmailApiService {
    public EmailResult sendMoodAlertToAdmin(Mood mood, String moodCategory) {
        String adminEmail = firstEnv("MAIL_ADMIN_TO", "ADMIN_EMAIL");
        if (adminEmail == null) {
            return EmailResult.skipped("MAIL_ADMIN_TO or ADMIN_EMAIL is not configured.");
        }
        return sendEmailToAdmin(mood, moodCategory, adminEmail);
    }

    public EmailResult sendEmailToAdmin(Mood mood, String moodCategory, String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return EmailResult.skipped("Admin email is not configured.");
        }
        String subject = "MindCare mood alert: " + moodCategory;
        String html = """
                <h2>MindCare mood alert</h2>
                <p>A student submitted a mood that needs admin review.</p>
                <ul>
                  <li><strong>Mood:</strong> %s</li>
                  <li><strong>Date:</strong> %s</li>
                  <li><strong>Stress:</strong> %s</li>
                  <li><strong>Energy:</strong> %s</li>
                </ul>
                <p><strong>Note:</strong></p>
                <p>%s</p>
                <p>Please open the admin dashboard to review the case.</p>
                """.formatted(
                escapeHtml(valueOrEmpty(mood.getMoodType())),
                escapeHtml(String.valueOf(mood.getMoodDate())),
                escapeHtml(formatLevel(mood.getStressLevel())),
                escapeHtml(formatLevel(mood.getEnergyLevel())),
                escapeHtml(valueOrEmpty(mood.getNote()))
        );

        return sendEmail(adminEmail, subject, html);
    }

    public EmailResult sendSupportEmailToStudent(Mood mood, String adminMessage) {
        String studentEmail = firstEnv("MAIL_STUDENT_TO", "STUDENT_EMAIL");
        if (studentEmail == null) {
            return EmailResult.skipped("MAIL_STUDENT_TO or STUDENT_EMAIL is not configured.");
        }
        return sendSupportEmailToStudent(mood, adminMessage, studentEmail);
    }

    public EmailResult sendSupportEmailToStudent(Mood mood, String adminMessage, String studentEmail) {
        if (studentEmail == null || studentEmail.isBlank()) {
            return EmailResult.skipped("Student email is not configured.");
        }
        String suggestions = adminMessage == null || adminMessage.isBlank()
                ? defaultSupportMessage(mood)
                : adminMessage.trim();
        String subject = "MindCare support suggestions";
        String html = """
                <h2>MindCare support message</h2>
                <p>An admin reviewed your mood entry and shared these suggestions:</p>
                <blockquote>%s</blockquote>
                <p><strong>Related mood:</strong> %s on %s</p>
                <p>If you feel unsafe or overwhelmed, please contact a trusted person, campus support, or emergency services immediately.</p>
                """.formatted(
                escapeHtml(suggestions).replace("\n", "<br>"),
                escapeHtml(valueOrEmpty(mood.getMoodType())),
                escapeHtml(String.valueOf(mood.getMoodDate()))
        );

        return sendEmail(studentEmail, subject, html);
    }

    private EmailResult sendEmail(String to, String subject, String html) {
        String host = firstEnv("SMTP_HOST", "MAIL_HOST");
        String port = firstEnv("SMTP_PORT", "MAIL_PORT");
        String username = firstEnv("SMTP_USERNAME", "MAIL_USERNAME");
        String password = firstEnv("SMTP_PASSWORD", "MAIL_PASSWORD");
        String from = firstEnv("MAIL_FROM", "SMTP_FROM");

        if (host == null || port == null || username == null || password == null || from == null) {
            return EmailResult.skipped("SMTP mail is not configured. Set SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, and MAIL_FROM.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "25000");
        props.put("mail.smtp.writetimeout", "25000");

        String startTls = firstEnv("SMTP_STARTTLS", "MAIL_STARTTLS");
        String ssl = firstEnv("SMTP_SSL", "MAIL_SSL");
        props.put("mail.smtp.starttls.enable", startTls == null ? "true" : startTls);
        if ("true".equalsIgnoreCase(ssl)) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
            return EmailResult.sent("Email sent to " + to + ".");
        } catch (MessagingException e) {
            return EmailResult.failed("SMTP email error: " + compact(e.getMessage()));
        }
    }

    private String defaultSupportMessage(Mood mood) {
        String lower = mood == null || mood.getMoodType() == null ? "" : mood.getMoodType().toLowerCase(Locale.ROOT);
        if (lower.contains("sad") || lower.contains("depress") || lower.contains("anxious") || lower.contains("stress")) {
            return "Please consider talking to a therapist, counselor, or trusted support person. Start with one small action today: rest, write down what feels heavy, and ask for help if the feeling continues.";
        }
        if (lower.contains("happy") || lower.contains("good") || lower.contains("grateful")) {
            return "Your mood looks positive. Try to notice what helped today and repeat one part of it tomorrow.";
        }
        return "Please take a few minutes to reflect on what you need next. If this feeling repeats, consider reaching out to a trusted person or professional support.";
    }

    private String firstEnv(String... names) {
        return AppConfig.first(names);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatLevel(Integer level) {
        return level == null ? "-" : String.valueOf(level);
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record EmailResult(boolean sent, boolean skipped, String message) {
        private static EmailResult sent(String message) {
            return new EmailResult(true, false, message);
        }

        private static EmailResult skipped(String message) {
            return new EmailResult(false, true, message);
        }

        private static EmailResult failed(String message) {
            return new EmailResult(false, false, message);
        }
    }
}
