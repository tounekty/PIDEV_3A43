package org.example.service;

import jakarta.mail.MessagingException;
import org.example.model.ForumMessage;
import org.example.model.ForumSubject;
import org.example.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionNotificationService {
    // Pattern that captures @mentions more reliably
    // Captures @ followed by 2-50 alphanumeric/dot/dash/underscore chars
    // Uses word boundaries and simple lookahead/lookbehind
    // Pattern that captures @emails or @names
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})\\b");

    private final AuthService authService;
    private final EmailService emailService;

    public MentionNotificationService() {
        this(new AuthService(), new EmailService());
    }

    MentionNotificationService(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    public MentionNotificationResult notifySubjectMentions(ForumSubject subject, User author) throws SQLException {
        Set<String> emails = extractMentionedEmails(subject == null ? null : subject.getTitre(),
                subject == null ? null : subject.getDescription());
        List<User> mentionedUsers = findRecipients(emails, author);
        if (mentionedUsers.isEmpty()) {
            return new MentionNotificationResult(0, 0, 0, emailService.isConfigured());
        }

        String subjectTitle = subject == null ? "Forum MindCare" : subject.getTitre();
        String body = "Bonjour,\n\n"
                + displayAuthor(author) + " vous a mentionné dans un sujet du forum MindCare.\n\n"
            + "Sujet: " + safe(subjectTitle) + "\n"
                + "Description: " + safe(subject == null ? null : subject.getDescription()) + "\n\n"
                + "Ouvrez l'application MindCare, puis le forum, pour voir le sujet.\n";

        return sendToRecipients(mentionedUsers, "[MindCare] Mention dans un sujet: " + safe(subjectTitle), body);
    }

    public MentionNotificationResult notifyMessageMentions(ForumSubject subject, ForumMessage message, User author) throws SQLException {
        Set<String> emails = extractMentionedEmails(message == null ? null : message.getContenu());
        List<User> mentionedUsers = findRecipients(emails, author);
        if (mentionedUsers.isEmpty()) {
            return new MentionNotificationResult(0, 0, 0, emailService.isConfigured());
        }

        String subjectTitle = subject == null ? "Sujet du forum" : subject.getTitre();
        String body = "Bonjour,\n\n"
                + displayAuthor(author) + " vous a mentionné dans un commentaire du forum MindCare.\n\n"
            + "Sujet: " + safe(subjectTitle) + "\n"
                + "Commentaire: " + safe(message == null ? null : message.getContenu()) + "\n\n"
                + "Ouvrez l'application MindCare, puis le forum, pour voir le commentaire.\n";

        return sendToRecipients(mentionedUsers, "[MindCare] Mention dans un commentaire: " + safe(subjectTitle), body);
    }

    public Set<String> extractMentionedEmails(String... texts) {
        Set<String> emails = new LinkedHashSet<>();
        if (texts == null) {
            return emails;
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = MENTION_PATTERN.matcher(text);
            while (matcher.find()) {
                emails.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
        }
        return emails;
    }

    private List<User> findRecipients(Set<String> emails, User author) throws SQLException {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        List<User> users = authService.findActiveUsersByEmails(emails);
        if (author == null) {
            return users;
        }
        return users.stream()
                .filter(user -> user.getId() != author.getId())
                .toList();
    }

    private MentionNotificationResult sendToRecipients(List<User> recipients, String mailSubject, String body) {
        if (!emailService.isConfigured()) {
            return new MentionNotificationResult(recipients.size(), 0, recipients.size(), false);
        }

        int sent = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (User recipient : recipients) {
            try {
                emailService.sendMentionNotification(recipient, mailSubject, body);
                sent++;
            } catch (MessagingException e) {
                failed++;
                errors.add(recipient.getEmail() + ": " + e.getMessage());
                System.err.println("Mention email failed for " + recipient.getEmail() + ": " + e.getMessage());
            }
        }
        return new MentionNotificationResult(recipients.size(), sent, failed, true, errors);
    }

    private String displayAuthor(User author) {
        if (author == null || author.getFullName() == null || author.getFullName().isBlank()) {
            return "Un utilisateur";
        }
        return author.getFullName();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
