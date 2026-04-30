package org.example.util;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * 📱 Service SMS via Twilio API.
 *
 * Envoie des SMS de rappel aux participants avant leurs événements.
 * Format numéro : +216XXXXXXXX (Tunisie)
 */
public class SmsService {

    // Use environment variables or config file for production
    private static final String ACCOUNT_SID  = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN   = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER  = System.getenv("TWILIO_FROM_NUMBER");

    static {
        if (ACCOUNT_SID != null && AUTH_TOKEN != null) {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        }
    }

    /**
     * Envoie un SMS à un numéro de téléphone.
     *
     * @param phone   numéro au format international (+216XXXXXXXX)
     * @param message texte du SMS (max 160 caractères)
     * @return true si envoyé avec succès
     */
    public boolean sendSMS(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            System.err.println("[SmsService] Numéro manquant");
            return false;
        }
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            System.err.println("[SmsService] Numéro invalide: " + phone);
            return false;
        }
        try {
            Message msg = Message.creator(
                    new PhoneNumber(normalized),
                    new PhoneNumber(FROM_NUMBER),
                    message
            ).create();

            System.out.println("[SmsService] SMS envoyé à " + normalized
                    + " — SID: " + msg.getSid()
                    + " — Status: " + msg.getStatus());
            return true;

        } catch (Exception e) {
            System.err.println("[SmsService] Erreur: " + e.getMessage());
            return false;
        }
    }

    /**
     * Normalise le numéro au format international.
     */
    private String normalizePhone(String phone) {
        String p = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
        if (p.startsWith("+"))   return p;
        if (p.startsWith("216")) return "+" + p;
        if (p.length() == 8)     return "+216" + p;
        return null;
    }
}
