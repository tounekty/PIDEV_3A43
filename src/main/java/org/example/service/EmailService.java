package org.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final String host;
    private final int port;
    private final String fromAddress;

    public EmailService() {
        this(
                readSetting("MAIL_SMTP_HOST", "mail.smtp.host", "localhost"),
                readIntSetting("MAIL_SMTP_PORT", "mail.smtp.port", 1025),
                readSetting("MAIL_FROM", "mail.from", "no-reply@mindcare.local")
        );
    }

    public EmailService(String host, int port, String fromAddress) {
        this.host = host;
        this.port = port;
        this.fromAddress = fromAddress;
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

    private void sendMail(String to, String subject, String body) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(5000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.US_ASCII)) {
                expectCode(reader.readLine(), 220);
                send(writer, reader, "HELO localhost", 250);
                send(writer, reader, "MAIL FROM:<" + fromAddress + ">", 250);
                send(writer, reader, "RCPT TO:<" + to + ">", 250);
                send(writer, reader, "DATA", 354);

                writer.print("From: " + fromAddress + "\r\n");
                writer.print("To: " + to + "\r\n");
                writer.print("Subject: " + subject + "\r\n");
                writer.print("Content-Type: text/plain; charset=UTF-8\r\n");
                writer.print("\r\n");
                for (String line : body.split("\\R", -1)) {
                    writer.print(escapeDot(line) + "\r\n");
                }
                writer.print(".\r\n");
                writer.flush();

                expectCode(reader.readLine(), 250);
                send(writer, reader, "QUIT", 221);
            }
        }
    }

    private void send(PrintWriter writer, BufferedReader reader, String command, int expectedCode) throws IOException {
        writer.print(command + "\r\n");
        writer.flush();
        expectCode(reader.readLine(), expectedCode);
    }

    private void expectCode(String response, int expectedCode) throws IOException {
        if (response == null || !response.startsWith(String.valueOf(expectedCode))) {
            throw new IOException("SMTP error from " + host + ": " + response);
        }
    }

    private String escapeDot(String line) {
        return line.startsWith(".") ? "." + line : line;
    }

    private static String readSetting(String envKey, String propertyKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int readIntSetting(String envKey, String propertyKey, int defaultValue) {
        String value = readSetting(envKey, propertyKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}