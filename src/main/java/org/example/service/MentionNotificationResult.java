package org.example.service;

import java.util.List;

public class MentionNotificationResult {
    private final int mentionedUsers;
    private final int emailsSent;
    private final int emailsFailed;
    private final boolean mailConfigured;
    private final List<String> errors;

    public MentionNotificationResult(int mentionedUsers, int emailsSent, int emailsFailed, boolean mailConfigured) {
        this(mentionedUsers, emailsSent, emailsFailed, mailConfigured, List.of());
    }

    public MentionNotificationResult(int mentionedUsers, int emailsSent, int emailsFailed, boolean mailConfigured, List<String> errors) {
        this.mentionedUsers = mentionedUsers;
        this.emailsSent = emailsSent;
        this.emailsFailed = emailsFailed;
        this.mailConfigured = mailConfigured;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public int getMentionedUsers() {
        return mentionedUsers;
    }

    public int getEmailsSent() {
        return emailsSent;
    }

    public int getEmailsFailed() {
        return emailsFailed;
    }

    public boolean isMailConfigured() {
        return mailConfigured;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFirstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
}
