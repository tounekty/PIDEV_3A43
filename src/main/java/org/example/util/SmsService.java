package org.example.util;

/**
 * Stub SMS service — SMS sending is disabled in this build (no Twilio dependency).
 * Replace with a real implementation to enable SMS notifications.
 */
public class SmsService {
    public void sendSMS(String toPhone, String message) {
        System.out.println("[SmsService] SMS (stub) to " + toPhone + ": " + message);
    }
}
