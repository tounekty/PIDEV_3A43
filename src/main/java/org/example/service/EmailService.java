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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final boolean auth;
    private final boolean startTls;
    private final boolean sslEnabled;
    private final String sslTrust;

    public EmailService() {
        this(
                readSetting(new String[]{"SMTP_HOST", "MAIL_SMTP_HOST"}, "mail.smtp.host", "smtp.gmail.com"),
                readIntSetting(new String[]{"SMTP_PORT", "MAIL_SMTP_PORT"}, "mail.smtp.port", 465),
                readSetting(new String[]{"SMTP_USERNAME", "SMTP_USER", "MAIL_SMTP_USERNAME"}, "mail.smtp.username", ""),
                readSetting(new String[]{"SMTP_PASSWORD", "MAIL_SMTP_PASSWORD"}, "mail.smtp.password", ""),
                readSetting(new String[]{"SMTP_FROM", "MAIL_FROM"}, "mail.from", ""),
                readBooleanSetting(new String[]{"SMTP_AUTH", "MAIL_SMTP_AUTH"}, "mail.smtp.auth", true),
                readBooleanSetting(new String[]{"SMTP_STARTTLS", "MAIL_SMTP_STARTTLS"}, "mail.smtp.starttls.enable", false),
                readBooleanSetting(new String[]{"SMTP_SSL_ENABLE", "MAIL_SMTP_SSL_ENABLE"}, "mail.smtp.ssl.enable", true),
                readSetting(new String[]{"SMTP_SSL_TRUST", "MAIL_SMTP_SSL_TRUST"}, "mail.smtp.ssl.trust", "smtp.gmail.com")
        );
    }

    public EmailService(String host, int port, String username, String password, String fromAddress,
                        boolean auth, boolean startTls, boolean sslEnabled, String sslTrust) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.fromAddress = (fromAddress == null || fromAddress.isBlank()) ? username : fromAddress;
        this.auth = auth;
        this.startTls = startTls;
        this.sslEnabled = sslEnabled;
        this.sslTrust = sslTrust;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank()
                && fromAddress != null && !fromAddress.isBlank()
                && port > 0
                && (!auth || (username != null && !username.isBlank() && password != null && !password.isBlank()));
    }

    public void sendAccountVerificationEmail(String to, String username, String code, LocalDateTime expiresAt) throws IOException {
        String subject = "MindCare account confirmation";
        String body = "Hello " + username + ",\n\n"
                + "Your MindCare account is ready, but it is not active yet.\n"
                + "Use this confirmation code in the app to activate it:\n\n"
                + code + "\n\n"
                + "This token expires at: " + expiresAt.format(DATE_TIME_FORMATTER) + "\n\n"
                + "If you did not create this account, you can ignore this email.";
        sendMail(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String username, String code, LocalDateTime expiresAt) throws IOException {
        String subject = "MindCare password reset";
        String body = "Hello " + username + ",\n\n"
                + "We received a password reset request for your MindCare account.\n"
                + "Use this reset code in the app to set a new password:\n\n"
                + code + "\n\n"
                + "This code expires at: " + expiresAt.format(DATE_TIME_FORMATTER) + "\n\n"
                + "If you did not request this reset, you can ignore this email.";
        sendMail(to, subject, body);
    }

    public void sendMentionNotification(User recipient, String subject, String body) throws MessagingException {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            throw new MessagingException("Destinataire invalide pour l'email de mention.");
        }
        try {
            sendMail(recipient.getEmail().trim(), subject, body);
        } catch (IOException e) {
            throw new MessagingException("Echec d'envoi de l'email de mention: " + e.getMessage(), e);
        }
    }

    public void sendPlainTextEmail(String to, String subject, String body) throws IOException {
        if (to == null || to.isBlank()) {
            throw new IOException("Recipient email is required.");
        }
        sendMail(to.trim(), subject == null ? "" : subject, body == null ? "" : body);
    }

    private void sendMail(String to, String subject, String body) throws IOException {
        if (!isConfigured()) {
            throw new IOException("SMTP non configure. Verifiez SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD et SMTP_FROM.");
        }

        IOException firstError = null;
        try {
            sendUsingConfig(to, subject, body, port, startTls, sslEnabled);
            return;
        } catch (IOException ex) {
            firstError = ex;
        }

        boolean isGmail = host != null && host.trim().equalsIgnoreCase("smtp.gmail.com");
        boolean tried465Ssl = port == 465 && sslEnabled;
        if (isGmail && tried465Ssl) {
            try {
                sendUsingConfig(to, subject, body, 587, true, false);
                return;
            } catch (IOException fallbackError) {
                throw new IOException(firstError.getMessage() + " | Fallback 587 STARTTLS failed: " + fallbackError.getMessage(), fallbackError);
            }
        }

        throw firstError;
    }

    private void sendUsingConfig(String to, String subject, String body, int effectivePort,
                                 boolean effectiveStartTls, boolean effectiveSslEnabled) throws IOException {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(effectivePort));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(effectiveStartTls));
        props.put("mail.smtp.ssl.enable", String.valueOf(effectiveSslEnabled));
        props.put("mail.smtp.ssl.trust", (sslTrust == null || sslTrust.isBlank()) ? host : sslTrust);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session;
        if (auth) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IOException("SMTP error from " + host + " (port " + effectivePort + "): " + e.getMessage(), e);
        }
    }

    private static String readSetting(String[] envKeys, String propertyKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            for (String envKey : envKeys) {
                value = System.getenv(envKey);
                if (value != null && !value.isBlank()) {
                    break;
                }
            }
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int readIntSetting(String[] envKeys, String propertyKey, int defaultValue) {
        String value = readSetting(envKeys, propertyKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean readBooleanSetting(String[] envKeys, String propertyKey, boolean defaultValue) {
        String value = readSetting(envKeys, propertyKey, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
}
