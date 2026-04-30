package org.example.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.event.Event;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de géocodage et d'analyse de lieu.
 * - Dictionnaire étendu de lieux tunisiens (correspondance partielle)
 * - Géocodage Nominatim en fallback
 * - Offset aléatoire pour éviter la superposition de marqueurs
 */
public class MapService {

    private static final double CENTER_LAT = 36.8190;
    private static final double CENTER_LNG = 10.1658;

    // ── Dictionnaire étendu de lieux connus en Tunisie ────────────────────────
    private static final Map<String, double[]> KNOWN_PLACES = new java.util.LinkedHashMap<>();
    static {
        // Tunis centre
        KNOWN_PLACES.put("tunis centre",        new double[]{36.8190, 10.1658});
        KNOWN_PLACES.put("centre ville",        new double[]{36.8190, 10.1658});
        KNOWN_PLACES.put("centre-ville",        new double[]{36.8190, 10.1658});
        KNOWN_PLACES.put("tunis",               new double[]{36.8190, 10.1658});
        KNOWN_PLACES.put("medina",              new double[]{36.7985, 10.1700});
        KNOWN_PLACES.put("médina",              new double[]{36.7985, 10.1700});
        KNOWN_PLACES.put("bab bhar",            new double[]{36.7990, 10.1780});
        KNOWN_PLACES.put("bab souika",          new double[]{36.8060, 10.1680});
        KNOWN_PLACES.put("bab laassal",         new double[]{36.8020, 10.1620});
        KNOWN_PLACES.put("bab el khadra",       new double[]{36.8100, 10.1590});
        KNOWN_PLACES.put("lafayette",           new double[]{36.8230, 10.1780});
        KNOWN_PLACES.put("el menzah",           new double[]{36.8450, 10.1900});
        KNOWN_PLACES.put("menzah",              new double[]{36.8450, 10.1900});
        KNOWN_PLACES.put("el manar",            new double[]{36.8380, 10.1980});
        KNOWN_PLACES.put("manar",               new double[]{36.8380, 10.1980});
        KNOWN_PLACES.put("el omrane",           new double[]{36.8200, 10.1500});
        KNOWN_PLACES.put("omrane",              new double[]{36.8200, 10.1500});
        KNOWN_PLACES.put("el kram",             new double[]{36.8350, 10.2600});
        KNOWN_PLACES.put("kram",                new double[]{36.8350, 10.2600});
        KNOWN_PLACES.put("el aouina",           new double[]{36.8510, 10.2270});
        KNOWN_PLACES.put("aouina",              new double[]{36.8510, 10.2270});
        // Lac
        KNOWN_PLACES.put("lac 2",               new double[]{36.8320, 10.2280});
        KNOWN_PLACES.put("lac 1",               new double[]{36.8280, 10.2100});
        KNOWN_PLACES.put("lac",                 new double[]{36.8300, 10.2200});
        KNOWN_PLACES.put("berges du lac",       new double[]{36.8320, 10.2280});
        // Banlieues nord
        KNOWN_PLACES.put("ariana",              new double[]{36.8625, 10.1956});
        KNOWN_PLACES.put("riadh el andalous",   new double[]{36.8700, 10.1800});
        KNOWN_PLACES.put("soukra",              new double[]{36.8900, 10.2100});
        KNOWN_PLACES.put("la marsa",            new double[]{36.8780, 10.3250});
        KNOWN_PLACES.put("marsa",               new double[]{36.8780, 10.3250});
        KNOWN_PLACES.put("sidi bou said",       new double[]{36.8700, 10.3420});
        KNOWN_PLACES.put("carthage",            new double[]{36.8528, 10.3233});
        KNOWN_PLACES.put("gammarth",            new double[]{36.9100, 10.3000});
        KNOWN_PLACES.put("ain zaghouan",        new double[]{36.8600, 10.2000});
        // Banlieues ouest
        KNOWN_PLACES.put("bardo",               new double[]{36.8090, 10.1340});
        KNOWN_PLACES.put("manouba",             new double[]{36.8100, 10.0980});
        KNOWN_PLACES.put("den den",             new double[]{36.8200, 10.1200});
        KNOWN_PLACES.put("oued ellil",          new double[]{36.8300, 10.0600});
        // Banlieues sud
        KNOWN_PLACES.put("ben arous",           new double[]{36.7530, 10.2280});
        KNOWN_PLACES.put("rades",               new double[]{36.7700, 10.2700});
        KNOWN_PLACES.put("hammam lif",          new double[]{36.7300, 10.3300});
        KNOWN_PLACES.put("hammam chatt",        new double[]{36.7100, 10.3600});
        KNOWN_PLACES.put("megrine",             new double[]{36.7700, 10.2200});
        KNOWN_PLACES.put("fouchana",            new double[]{36.7000, 10.1700});
        KNOWN_PLACES.put("mohamedia",           new double[]{36.6800, 10.1600});
        // Universités / campus
        KNOWN_PLACES.put("esprit",              new double[]{36.8990, 10.1880});
        KNOWN_PLACES.put("université",          new double[]{36.8190, 10.1658});
        KNOWN_PLACES.put("campus",              new double[]{36.8990, 10.1880});
        KNOWN_PLACES.put("iset",                new double[]{36.8200, 10.1700});
        KNOWN_PLACES.put("enit",                new double[]{36.8400, 10.1600});
        KNOWN_PLACES.put("fst",                 new double[]{36.8380, 10.1980});
        KNOWN_PLACES.put("fseg",                new double[]{36.8190, 10.1658});
        KNOWN_PLACES.put("ihec",                new double[]{36.8450, 10.1900});
        KNOWN_PLACES.put("isit",                new double[]{36.8200, 10.1700});
        KNOWN_PLACES.put("sup'com",             new double[]{36.8990, 10.1880});
        KNOWN_PLACES.put("supcom",              new double[]{36.8990, 10.1880});
        // Autres villes
        KNOWN_PLACES.put("hammamet",            new double[]{36.4000, 10.6167});
        KNOWN_PLACES.put("nabeul",              new double[]{36.4500, 10.7333});
        KNOWN_PLACES.put("sousse",              new double[]{35.8333, 10.6333});
        KNOWN_PLACES.put("monastir",            new double[]{35.7643, 10.8113});
        KNOWN_PLACES.put("sfax",                new double[]{34.7400, 10.7600});
        KNOWN_PLACES.put("gabes",               new double[]{33.8833, 10.0833});
        KNOWN_PLACES.put("bizerte",             new double[]{37.2744, 9.8739});
        KNOWN_PLACES.put("kairouan",            new double[]{35.6781, 10.0963});
        KNOWN_PLACES.put("gafsa",               new double[]{34.4250, 8.7842});
        KNOWN_PLACES.put("tozeur",              new double[]{33.9197, 8.1335});
        KNOWN_PLACES.put("djerba",              new double[]{33.8075, 10.8451});
        KNOWN_PLACES.put("zarzis",              new double[]{33.5033, 11.1119});
        KNOWN_PLACES.put("tabarka",             new double[]{36.9544, 8.7581});
        KNOWN_PLACES.put("zaghouan",            new double[]{36.4028, 10.1428});
        KNOWN_PLACES.put("beja",                new double[]{36.7256, 9.1817});
        KNOWN_PLACES.put("jendouba",            new double[]{36.5011, 8.7803});
        KNOWN_PLACES.put("siliana",             new double[]{36.0844, 9.3706});
        KNOWN_PLACES.put("kasserine",           new double[]{35.1722, 8.8306});
        KNOWN_PLACES.put("sidi bouzid",         new double[]{35.0381, 9.4858});
        KNOWN_PLACES.put("medenine",            new double[]{33.3547, 10.5053});
        KNOWN_PLACES.put("tataouine",           new double[]{32.9211, 10.4511});
        KNOWN_PLACES.put("kebili",              new double[]{33.7042, 8.9694});
    }

    // Cache géocodage (partagé entre instances)
    private static final Map<String, double[]> geoCache = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────────

    public double[] geocode(String address) {
        if (address == null || address.isBlank()) return new double[]{CENTER_LAT, CENTER_LNG};
        String key = address.toLowerCase().trim();

        // Cache
        if (geoCache.containsKey(key)) return geoCache.get(key);

        // 1. Correspondance exacte dans le dictionnaire
        if (KNOWN_PLACES.containsKey(key)) {
            double[] c = KNOWN_PLACES.get(key);
            geoCache.put(key, c);
            return c;
        }

        // 2. Correspondance partielle (le lieu contient un mot-clé connu)
        //    Trier par longueur décroissante pour préférer les correspondances les plus précises
        String bestKey = null;
        int bestLen = 0;
        for (String k : KNOWN_PLACES.keySet()) {
            if (key.contains(k) && k.length() > bestLen) {
                bestKey = k;
                bestLen = k.length();
            }
        }
        if (bestKey != null) {
            double[] c = KNOWN_PLACES.get(bestKey);
            geoCache.put(key, c);
            return c;
        }

        // 3. Nominatim (réseau) avec timeout court
        double[] coords = nominatimGeocode(address);
        if (coords != null && isInTunisia(coords[0], coords[1])) {
            geoCache.put(key, coords);
            return coords;
        }

        // 4. Fallback : centre de Tunis + offset aléatoire pour éviter superposition
        double[] fallback = {
            CENTER_LAT + (Math.random() - 0.5) * 0.03,
            CENTER_LNG + (Math.random() - 0.5) * 0.03
        };
        geoCache.put(key, fallback);
        System.out.println("[MapService] Fallback Tunis pour: " + address);
        return fallback;
    }

    public double getLocationScore(double lat, double lng) {
        double distKm = haversineKm(lat, lng, CENTER_LAT, CENTER_LNG);
        if (distKm <= 1)  return 100;
        if (distKm <= 3)  return 85;
        if (distKm <= 5)  return 70;
        if (distKm <= 8)  return 55;
        if (distKm <= 12) return 40;
        if (distKm <= 20) return 25;
        return 10;
    }

    public String getLocationLabel(double score) {
        if (score >= 85) return "🔥 Lieu central — forte participation attendue";
        if (score >= 70) return "✔ Bon emplacement — accessible";
        if (score >= 50) return "ℹ Lieu correct — légèrement excentré";
        if (score >= 30) return "⚠ Lieu éloigné — peut réduire la participation";
        return "❌ Lieu très éloigné — impact négatif sur la participation";
    }

    public String buildMarkersJson(java.util.List<Event> events,
                                   java.util.Map<Integer, Integer> resCounts) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Event ev : events) {
            if (ev.getLieu() == null || ev.getLieu().isBlank()) continue;
            double[] coords = geocode(ev.getLieu());
            double score    = getLocationScore(coords[0], coords[1]);
            int reserved    = resCounts != null ? resCounts.getOrDefault(ev.getId(), 0) : 0;
            int remaining   = Math.max(ev.getCapacite() - reserved, 0);
            String color    = score >= 70 ? "#22c55e" : score >= 40 ? "#f59e0b" : "#ef4444";
            String dateStr  = ev.getDateEvent() != null
                    ? ev.getDateEvent().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "—";
            String cat = ev.getCategorie() != null ? ev.getCategorie() : "général";

            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"lat\":").append(coords[0]).append(",")
              .append("\"lng\":").append(coords[1]).append(",")
              .append("\"titre\":\"").append(escape(ev.getTitre())).append("\",")
              .append("\"lieu\":\"").append(escape(ev.getLieu())).append("\",")
              .append("\"date\":\"").append(dateStr).append("\",")
              .append("\"categorie\":\"").append(escape(cat)).append("\",")
              .append("\"capacite\":").append(ev.getCapacite()).append(",")
              .append("\"reserved\":").append(reserved).append(",")
              .append("\"remaining\":").append(remaining).append(",")
              .append("\"score\":").append((int) score).append(",")
              .append("\"color\":\"").append(color).append("\",")
              .append("\"id\":").append(ev.getId())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double[] nominatimGeocode(String address) {
        try {
            String query = address.toLowerCase().contains("tunis") ? address
                         : address + ", Tunisie";
            String urlStr = "https://nominatim.openstreetmap.org/search?q="
                    + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&format=json&limit=1&countrycodes=tn";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "MindCareEvents/1.0 (student project)");

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            JsonArray arr = JsonParser.parseString(sb.toString()).getAsJsonArray();
            if (arr.isEmpty()) return null;

            JsonObject obj = arr.get(0).getAsJsonObject();
            double lat = obj.get("lat").getAsDouble();
            double lng = obj.get("lon").getAsDouble();
            return new double[]{lat, lng};

        } catch (Exception e) {
            System.err.println("[MapService] Nominatim: " + e.getMessage());
            return null;
        }
    }

    private boolean isInTunisia(double lat, double lng) {
        return lat >= 30.0 && lat <= 38.0 && lng >= 7.0 && lng <= 13.0;
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", "").replace("'", "\\'");
    }
}
