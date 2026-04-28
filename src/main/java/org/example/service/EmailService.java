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
    private final boolean startTlsEnabled;
    private final boolean startTlsRequired;
    private final boolean sslEnabled;
    private final String sslTrust;
    private final String connectionTimeoutMs;
    private final String readTimeoutMs;
    private final String writeTimeoutMs;

    public EmailService() {
        this.host = firstConfigured("SMTP_HOST", "mail.smtp.host");
        this.port = firstConfigured("SMTP_PORT", "mail.smtp.port", "587");
        this.username = firstConfiguredAny(
                new String[]{"SMTP_USERNAME", "SMTP_USER"},
                new String[]{"mail.smtp.user"},
                null
        );
        this.password = firstConfigured("SMTP_PASSWORD", "mail.smtp.password");
        this.from = firstConfigured("SMTP_FROM", "mail.smtp.from", username);
        this.authEnabled = Boolean.parseBoolean(firstConfigured("SMTP_AUTH", "mail.smtp.auth", username != null && !username.isBlank() ? "true" : "false"));
        this.startTlsEnabled = Boolean.parseBoolean(firstConfigured("SMTP_STARTTLS", "mail.smtp.starttls.enable", "true"));
        this.startTlsRequired = Boolean.parseBoolean(firstConfigured("SMTP_STARTTLS_REQUIRED", "mail.smtp.starttls.required", "false"));
        this.sslEnabled = Boolean.parseBoolean(firstConfigured("SMTP_SSL_ENABLE", "mail.smtp.ssl.enable", "465".equals(port) ? "true" : "false"));
        this.sslTrust = firstConfigured("SMTP_SSL_TRUST", "mail.smtp.ssl.trust", host);
        this.connectionTimeoutMs = firstConfigured("SMTP_CONNECTION_TIMEOUT", "mail.smtp.connectiontimeout", "10000");
        this.readTimeoutMs = firstConfigured("SMTP_READ_TIMEOUT", "mail.smtp.timeout", "10000");
        this.writeTimeoutMs = firstConfigured("SMTP_WRITE_TIMEOUT", "mail.smtp.writetimeout", "10000");
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
        if (authEnabled && (username == null || username.isBlank() || password == null || password.isBlank())) {
            throw new MessagingException("SMTP auth is enabled but SMTP_USERNAME/SMTP_USER or SMTP_PASSWORD is missing.");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", String.valueOf(authEnabled));
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));
        properties.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        properties.put("mail.smtp.ssl.enable", String.valueOf(sslEnabled));
        properties.put("mail.smtp.ssl.trust", sslTrust);
        properties.put("mail.smtp.connectiontimeout", connectionTimeoutMs);
        properties.put("mail.smtp.timeout", readTimeoutMs);
        properties.put("mail.smtp.writetimeout", writeTimeoutMs);
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

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

    private String firstConfiguredAny(String[] envNames, String[] propertyNames, String defaultValue) {
        if (envNames != null) {
            for (String envName : envNames) {
                String envValue = System.getenv(envName);
                if (envValue != null && !envValue.isBlank()) {
                    return envValue.trim();
                }
            }
        }

        if (propertyNames != null) {
            for (String propertyName : propertyNames) {
                String propertyValue = System.getProperty(propertyName);
                if (propertyValue != null && !propertyValue.isBlank()) {
                    return propertyValue.trim();
                }
            }
        }

        return defaultValue;
    }
}
