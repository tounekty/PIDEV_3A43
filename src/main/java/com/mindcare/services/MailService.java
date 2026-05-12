package com.mindcare.services;

import com.mindcare.utils.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MailService {

    private final Session session;
    private final String fromAddress;

    public MailService() {
        this(
            AppConfig.get("mail.smtp.host", "MAIL_SMTP_HOST", "localhost"),
            Integer.parseInt(AppConfig.get("mail.smtp.port", "MAIL_SMTP_PORT", "1025")),
            AppConfig.get("mail.smtp.username", "MAIL_SMTP_USERNAME", ""),
            AppConfig.get("mail.smtp.password", "MAIL_SMTP_PASSWORD", ""),
            AppConfig.get("mail.from", "MAIL_FROM", "noreply@mindcare.local"),
            Boolean.parseBoolean(AppConfig.get("mail.smtp.auth", "MAIL_SMTP_AUTH", "false")),
            Boolean.parseBoolean(AppConfig.get("mail.smtp.starttls.enable", "MAIL_SMTP_STARTTLS", "false"))
        );
    }

    public MailService(String host, int port, String username, String password, String fromAddress, boolean auth, boolean startTls) {
        this.fromAddress = fromAddress == null || fromAddress.isBlank() ? "noreply@mindcare.local" : fromAddress.trim();

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.auth", String.valueOf(auth));
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        properties.put("mail.smtp.ssl.trust", host);

        if (auth) {
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
            this.session = Session.getInstance(properties, authenticator);
        } else {
            this.session = Session.getInstance(properties);
        }
    }

    public void sendTextEmail(String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
            message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());
            message.setText(body == null ? "" : body, StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Unable to send email to " + recipient, exception);
        }
    }

}