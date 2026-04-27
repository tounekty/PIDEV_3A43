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
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}._-])@([\\p{L}\\p{N}._-]{2,100})");

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
        Set<String> usernames = extractMentionedUsernames(subject == null ? null : subject.getTitre(),
                subject == null ? null : subject.getDescription());
        List<User> mentionedUsers = findRecipients(usernames, author);
        if (mentionedUsers.isEmpty()) {
            return new MentionNotificationResult(0, 0, 0, emailService.isConfigured());
        }

        String subjectTitle = subject == null ? "Forum MindCare" : subject.getTitre();
        String body = "Bonjour,\n\n"
                + displayAuthor(author) + " vous a mentionne dans un sujet du forum MindCare.\n\n"
                + "Sujet #" + (subject == null ? "" : subject.getId()) + ": " + safe(subjectTitle) + "\n"
                + "Description: " + safe(subject == null ? null : subject.getDescription()) + "\n\n"
                + "Ouvrez l'application MindCare, puis le forum, pour voir le sujet.\n";

        return sendToRecipients(mentionedUsers, "[MindCare] Mention dans un sujet: " + safe(subjectTitle), body);
    }

    public MentionNotificationResult notifyMessageMentions(ForumSubject subject, ForumMessage message, User author) throws SQLException {
        Set<String> usernames = extractMentionedUsernames(message == null ? null : message.getContenu());
        List<User> mentionedUsers = findRecipients(usernames, author);
        if (mentionedUsers.isEmpty()) {
            return new MentionNotificationResult(0, 0, 0, emailService.isConfigured());
        }

        String subjectTitle = subject == null ? "Sujet du forum" : subject.getTitre();
        String body = "Bonjour,\n\n"
                + displayAuthor(author) + " vous a mentionne dans un commentaire du forum MindCare.\n\n"
                + "Sujet #" + (subject == null ? "" : subject.getId()) + ": " + safe(subjectTitle) + "\n"
                + "Commentaire #" + (message == null ? "" : message.getId()) + "\n"
                + "Commentaire: " + safe(message == null ? null : message.getContenu()) + "\n\n"
                + "Ouvrez l'application MindCare, puis le forum, pour voir le commentaire.\n";

        return sendToRecipients(mentionedUsers, "[MindCare] Mention dans un commentaire: " + safe(subjectTitle), body);
    }

    public Set<String> extractMentionedUsernames(String... texts) {
        Set<String> usernames = new LinkedHashSet<>();
        if (texts == null) {
            return usernames;
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = MENTION_PATTERN.matcher(text);
            while (matcher.find()) {
                usernames.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
        }
        return usernames;
    }

    private List<User> findRecipients(Set<String> usernames, User author) throws SQLException {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        List<User> users = authService.findActiveUsersByUsernames(usernames);
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
                errors.add(recipient.getUsername() + ": " + e.getMessage());
                System.err.println("Mention email failed for " + recipient.getUsername() + ": " + e.getMessage());
            }
        }
        return new MentionNotificationResult(recipients.size(), sent, failed, true, errors);
    }

    private String displayAuthor(User author) {
        if (author == null || author.getUsername() == null || author.getUsername().isBlank()) {
            return "Un utilisateur";
        }
        return author.getUsername();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
