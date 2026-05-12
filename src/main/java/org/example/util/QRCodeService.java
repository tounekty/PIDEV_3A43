package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 🔐 QR Code Service — génère des tokens sécurisés pour les billets numériques.
 *
 * Le QR code contient :
 *   - reservation_id
 *   - user_id
 *   - event_id
 *   - expiration (date de l'événement + 2h)
 *   - hash sécurisé (SHA-256)
 *
 * Pas de bibliothèque externe — le QR est rendu via Google Charts API
 * dans le corps HTML de l'email (lien image).
 */
public class QRCodeService {

    private static final String SECRET_SALT = "MindCare2026SecureTicket";

    /**
     * Génère le contenu texte du QR code (payload sécurisé).
     */
    public String generateQRPayload(int reservationId, int userId, int eventId,
                                     LocalDateTime eventDateTime) {
        // Use simple numeric timestamp to avoid any formatting issues
        long epochMinutes = eventDateTime.plusHours(2)
                .toEpochSecond(java.time.ZoneOffset.UTC) / 60;
        String expiration = String.valueOf(epochMinutes);

        String raw = reservationId + "|" + userId + "|" + eventId + "|" + expiration;
        String hash = sha256(raw + SECRET_SALT).substring(0, 12).toUpperCase();

        return raw + "|" + hash;
    }

    /**
     * Validates only the hash — ignores expiration.
     * Use this for demo/test events that may be in the past.
     */
    public boolean validateHashOnly(String payload) {
        try {
            String[] parts = payload.split("\\|");
            if (parts.length != 5) return false;
            String raw = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
            String expectedHash = sha256(raw + SECRET_SALT).substring(0, 12).toUpperCase();
            return expectedHash.equals(parts[4]);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates a QR payload — checks hash AND expiration.
     */
    public boolean validateQRPayload(String payload) {
        try {
            String[] parts = payload.split("\\|");
            if (parts.length != 5) return false;

            String raw = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
            String expectedHash = sha256(raw + SECRET_SALT).substring(0, 12).toUpperCase();

            if (!expectedHash.equals(parts[4])) return false;

            // Check expiration using epoch minutes
            long epochMinutes = Long.parseLong(parts[3]);
            long nowMinutes = java.time.Instant.now().getEpochSecond() / 60;
            return nowMinutes <= epochMinutes;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Génère le contenu du QR code.
     * On encode directement le payload (pas une URL HTTP) pour que le QR
     * fonctionne sans réseau — même depuis 4G ou hors WiFi.
     * Format: "MINDCARE:reservationId|userId|eventId|expiration|HASH"
     */
    public String generateTicketUrl(String payload) {
        // Encoder le payload en Base64 URL-safe pour un QR compact et lisible
        String base64Token = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "MINDCARE:" + base64Token;
    }

    /**
     * Génère l'URL HTTP du ticket (pour accès depuis navigateur sur WiFi local).
     * Utiliser generateTicketUrl() pour le QR code — cette méthode est pour l'email.
     */
    public String generateTicketHttpUrl(String payload) {
        try {
            String host = getLocalIpAddress();
            String base64Token = java.util.Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return "http://" + host + ":8080/ticket?token=" + base64Token;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Génère l'image QR en bytes PNG via ZXing (local, sans service externe).
     * Encode n'importe quel contenu — URL, texte, data URL HTML.
     */
    public byte[] generateQRBytes(String content, int size) {
        try {
            com.google.zxing.qrcode.QRCodeWriter writer =
                    new com.google.zxing.qrcode.QRCodeWriter();
            java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION,
                    com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 1);
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");

            com.google.zxing.common.BitMatrix matrix =
                    writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("[QRCodeService] ZXing error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère un data URI PNG du QR code via ZXing (100% local, sans internet).
     * Retourne "data:image/png;base64,..." utilisable directement dans <img src=...>
     * ou dans JavaFX Image(new ByteArrayInputStream(...)).
     */
    public String generateQRDataUrl(String content, int size) {
        byte[] bytes = generateQRBytes(content, size);
        if (bytes == null) return "";
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Génère l'URL d'une image QR code via quickchart.io (fallback externe).
     * Utiliser generateQRDataUrl() en priorité pour éviter la dépendance réseau.
     */
    public String generateQRImageUrl(String content, int size) {
        try {
            String encoded = java.net.URLEncoder.encode(content, StandardCharsets.UTF_8);
            return "https://quickchart.io/qr?text=" + encoded
                    + "&size=" + size
                    + "&format=png"
                    + "&margin=2"
                    + "&ecLevel=M";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Détecte automatiquement l'adresse IP locale du PC (WiFi/Ethernet).
     * Évite localhost qui n'est pas accessible depuis un téléphone.
     * Accepte n'importe quelle IPv4 valide (192.168.x.x, 10.x.x.x, 172.16.x.x, etc.)
     */
    private String getLocalIpAddress() {
        try {
            String preferred192 = null;  // Préférer 192.168.x.x
            String fallbackIp = null;    // Fallback: n'importe quelle IPv4

            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                String name = iface.getDisplayName().toLowerCase();
                // Exclure les interfaces virtuelles/VPN
                if (name.contains("vmware") || name.contains("virtual") || 
                    name.contains("hyper-v") || name.contains("vnic") ||
                    name.contains("docker") || name.contains("veth")) continue;

                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        
                        // Préférer 192.168.x.x
                        if (ip.startsWith("192.168.")) {
                            return ip;  // Retourner immédiatement si trouvé
                        }
                        
                        // Fallback: accepter toute autre IP privée valide
                        if (fallbackIp == null && !ip.startsWith("127.")) {
                            fallbackIp = ip;
                        }
                    }
                }
            }

            // Retourner le fallback si trouvé
            if (fallbackIp != null) {
                return fallbackIp;
            }
        } catch (Exception ignored) {}
        
        // Dernier recours: localhost (ne fonctionne que sur la machine locale)
        return "localhost";
    }

    /**
     * Returns the port used by the ticket server (same as API server).
     */
    public static int getTicketPort() { return 8080; }

    /**
     * Encode le payload en Base64 pour stockage compact.
     */
    public String encodePayload(String payload) {
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Décode un payload Base64.
     */
    public String decodePayload(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
