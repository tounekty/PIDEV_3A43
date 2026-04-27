package org.example.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.model.User;

import java.util.Properties;

public class EmailService {
    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean authEnabled;

    public EmailService() {
        this.host = firstConfigured("SMTP_HOST", "mail.smtp.host");
        this.port = firstConfigured("SMTP_PORT", "mail.smtp.port", "587");
        this.username = firstConfigured("SMTP_USERNAME", "mail.smtp.user");
        this.password = firstConfigured("SMTP_PASSWORD", "mail.smtp.password");
        this.from = firstConfigured("SMTP_FROM", "mail.smtp.from", username);
        this.authEnabled = Boolean.parseBoolean(firstConfigured("SMTP_AUTH", "mail.smtp.auth", username != null && !username.isBlank() ? "true" : "false"));
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && from != null && !from.isBlank();
    }

    public void send(String recipientEmail, String subject, String body) throws MessagingException {
        if (!isConfigured()) {
            throw new MessagingException("SMTP is not configured.");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new MessagingException("Recipient email is empty.");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", String.valueOf(authEnabled));
        properties.put("mail.smtp.starttls.enable", firstConfigured("SMTP_STARTTLS", "mail.smtp.starttls.enable", "true"));

        Session session = authEnabled
                ? Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                })
                : Session.getInstance(properties);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }

    public void sendMentionNotification(User recipient, String subject, String body) throws MessagingException {
        if (recipient == null) {
            throw new MessagingException("Recipient user is empty.");
        }
        send(recipient.getEmail(), subject, body);
    }

    private String firstConfigured(String envName, String propertyName) {
        return firstConfigured(envName, propertyName, null);
    }

    private String firstConfigured(String envName, String propertyName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return defaultValue;
    }
}
