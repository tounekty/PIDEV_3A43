package org.example.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🌦️ Service météo intelligent.
 *
 * - Aujourd'hui / passé   → wttr.in  (météo actuelle, sans clé)
 * - Jusqu'à J+16          → Open-Meteo (prévisions réelles, sans clé)
 * - Au-delà de J+16       → wttr.in actuel (meilleure approximation)
 *
 * Cache 30 min par (ville + date).
 */
public class WeatherService {

    // Coordonnées de Tunis (utilisées pour Open-Meteo)
    private static final double LAT_TUNIS = 36.8;
    private static final double LON_TUNIS = 10.18;

    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;

    // Cache : "ville|date" → CacheEntry
    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final String condition;
        final int    tempC;
        final long   timestamp;
        CacheEntry(String condition, int tempC) {
            this.condition = condition;
            this.tempC     = tempC;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Condition météo pour la ville à la date donnée (ou aujourd'hui si null). */
    public String getWeatherCondition(String city) {
        return getWeatherCondition(city, LocalDate.now());
    }

    public String getWeatherCondition(String city, LocalDate date) {
        CacheEntry e = getEntry(city, date);
        return e != null ? e.condition : "Unknown";
    }

    /** Température en °C pour la ville à la date donnée. */
    public int getTemperature(String city) {
        return getTemperature(city, LocalDate.now());
    }

    public int getTemperature(String city, LocalDate date) {
        CacheEntry e = getEntry(city, date);
        return e != null ? e.tempC : 0;
    }

    /** Affichage complet : emoji + condition + température. */
    public String getWeatherDisplay(String city, LocalDate date) {
        CacheEntry e = getEntry(city, date);
        if (e == null || e.condition.equals("Unknown")) return "Météo indisponible";
        return toEmoji(e.condition) + "  " + e.condition + "  " + e.tempC + "°C";
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private CacheEntry getEntry(String city, LocalDate date) {
        if (city == null || city.isBlank()) return null;
        if (date == null) date = LocalDate.now();

        String key = city.toLowerCase() + "|" + date;
        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) return cached;

        long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), date);

        CacheEntry entry;
        if (daysAhead >= 1 && daysAhead <= 16) {
            // Prévision future → Open-Meteo
            entry = fetchOpenMeteo(date);
        } else {
            // Aujourd'hui ou passé ou trop loin → wttr.in actuel
            entry = fetchWttr(city);
        }

        if (entry != null) cache.put(key, entry);
        return entry;
    }

    /** Prévision Open-Meteo pour Tunis à une date précise (jusqu'à J+16). */
    private CacheEntry fetchOpenMeteo(LocalDate targetDate) {
        try {
            String urlStr = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + LAT_TUNIS
                    + "&longitude=" + LON_TUNIS
                    + "&daily=weathercode,temperature_2m_max,temperature_2m_min"
                    + "&timezone=Africa%2FTunis"
                    + "&forecast_days=16";

            String body = httpGet(urlStr);
            if (body == null) return null;

            JsonObject json  = JsonParser.parseString(body).getAsJsonObject();
            JsonObject daily = json.getAsJsonObject("daily");
            JsonArray  times = daily.getAsJsonArray("time");
            JsonArray  codes = daily.getAsJsonArray("weathercode");
            JsonArray  maxTs = daily.getAsJsonArray("temperature_2m_max");
            JsonArray  minTs = daily.getAsJsonArray("temperature_2m_min");

            String target = targetDate.toString(); // "2026-05-03"
            for (int i = 0; i < times.size(); i++) {
                if (target.equals(times.get(i).getAsString())) {
                    int    code  = codes.get(i).getAsInt();
                    double maxT  = maxTs.get(i).getAsDouble();
                    double minT  = minTs.get(i).getAsDouble();
                    int    avgT  = (int) Math.round((maxT + minT) / 2.0);
                    String cond  = wmoCodeToCondition(code);
                    System.out.println("[WeatherService] Open-Meteo " + target + " → " + cond + " " + avgT + "°C");
                    return new CacheEntry(cond, avgT);
                }
            }
            System.err.println("[WeatherService] Date " + target + " non trouvée dans Open-Meteo");
        } catch (Exception e) {
            System.err.println("[WeatherService] Open-Meteo erreur: " + e.getMessage());
        }
        return null;
    }

    /** Météo actuelle via wttr.in (aujourd'hui). */
    private CacheEntry fetchWttr(String city) {
        try {
            String urlStr = "https://wttr.in/"
                    + java.net.URLEncoder.encode(city, "UTF-8")
                    + "?format=j1";

            String body = httpGet(urlStr);
            if (body == null) return null;

            JsonObject json    = JsonParser.parseString(body).getAsJsonObject();
            JsonObject current = json.getAsJsonArray("current_condition")
                    .get(0).getAsJsonObject();

            int    tempC = current.get("temp_C").getAsInt();
            String desc  = current.getAsJsonArray("weatherDesc")
                    .get(0).getAsJsonObject().get("value").getAsString();
            String cond  = normalizeWttrDesc(desc);

            System.out.println("[WeatherService] wttr.in " + city + " → " + cond + " " + tempC + "°C");
            return new CacheEntry(cond, tempC);

        } catch (Exception e) {
            System.err.println("[WeatherService] wttr.in erreur: " + e.getMessage());
            return null;
        }
    }

    /** Appel HTTP GET simple, retourne le body ou null. */
    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "MindCareEvents/1.0");
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ── WMO weather code → condition normalisée ───────────────────────────────
    // https://open-meteo.com/en/docs#weathervariables

    private static String wmoCodeToCondition(int code) {
        if (code == 0)                          return "Clear";
        if (code == 1 || code == 2)             return "Clouds";
        if (code == 3)                          return "Clouds";
        if (code >= 45 && code <= 48)           return "Mist";
        if (code >= 51 && code <= 57)           return "Drizzle";
        if (code >= 61 && code <= 67)           return "Rain";
        if (code >= 71 && code <= 77)           return "Snow";
        if (code >= 80 && code <= 82)           return "Rain";
        if (code == 85 || code == 86)           return "Snow";
        if (code >= 95 && code <= 99)           return "Thunderstorm";
        return "Clouds";
    }

    // ── wttr.in description → condition normalisée ────────────────────────────

    private static String normalizeWttrDesc(String desc) {
        if (desc == null) return "Unknown";
        String d = desc.toLowerCase();
        if (d.contains("thunder") || d.contains("storm"))       return "Thunderstorm";
        if (d.contains("drizzle") || d.contains("light rain"))  return "Drizzle";
        if (d.contains("rain") || d.contains("shower"))         return "Rain";
        if (d.contains("snow") || d.contains("blizzard")
                || d.contains("sleet") || d.contains("ice"))    return "Snow";
        if (d.contains("fog") || d.contains("mist"))            return "Mist";
        if (d.contains("haze") || d.contains("smoke")
                || d.contains("dust") || d.contains("sand"))    return "Haze";
        if (d.contains("overcast") || d.contains("cloudy"))     return "Clouds";
        if (d.contains("partly") || d.contains("cloud"))        return "Clouds";
        if (d.contains("sunny") || d.contains("clear"))         return "Clear";
        return "Clouds";
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static String toEmoji(String condition) {
        if (condition == null) return "🌡️";
        return switch (condition) {
            case "Clear"        -> "☀️";
            case "Clouds"       -> "☁️";
            case "Rain"         -> "🌧️";
            case "Drizzle"      -> "🌦️";
            case "Thunderstorm" -> "⛈️";
            case "Snow"         -> "❄️";
            case "Mist", "Haze", "Fog" -> "🌫️";
            default             -> "🌡️";
        };
    }

    public static boolean isIndoor(String category) {
        if (category == null) return true;
        String c = category.toLowerCase();
        return c.contains("yoga") || c.contains("wellness") || c.contains("meditation")
                || c.contains("conference") || c.contains("atelier")
                || c.contains("workshop") || c.contains("indoor")
                || c.contains("cours") || c.contains("formation");
    }

    public static boolean isOutdoor(String category) {
        if (category == null) return false;
        String c = category.toLowerCase();
        return c.contains("sport") || c.contains("outdoor")
                || c.contains("randonnée") || c.contains("course")
                || c.contains("festival") || c.contains("pique-nique");
    }
}
