package org.example.event;

import org.example.config.DatabaseConnection;
import org.example.util.WeatherService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * VERSION 3 — Service de prédiction intelligente avec météo.
 *
 * Calcule la probabilité de succès d'un événement selon la formule :
 *   score = (historical * 0.35) + (availability * 0.25)
 *         + (category  * 0.20) + (day         * 0.10)
 *         + (weather   * 0.10)
 *
 * Lecture seule — ne modifie ni la base ni les services existants.
 */
public class PredictionService {

    private static final String DEFAULT_CITY = "Tunis";

    private final WeatherService weatherService = new WeatherService();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Prédit le succès d'un événement pour un créneau donné.
     *
     * @param category catégorie de l'événement (ex: "yoga", "sport")
     * @param date     date prévue
     * @param time     heure prévue
     * @return PredictionResult avec probabilité, participants estimés et explication
     */
    public PredictionResult predict(String category, LocalDate date, LocalTime time) {
        return predict(category, date, time, DEFAULT_CITY);
    }

    /**
     * Prédit le succès avec une ville spécifique pour la météo.
     */
    public PredictionResult predict(String category, LocalDate date, LocalTime time, String city) {

        // ── 1. Calcul des 5 facteurs (chacun entre 0.0 et 1.0) ──
        double historical    = getHistoricalScore(category, date, time);
        double availability  = getAvailabilityScore(date, time);
        double categoryScore = getCategoryScore(category, time);
        double dayScore      = getDayScore(date);

        // Météo — uniquement pour les dates proches (≤ 16 jours)
        String weather = "Unknown";
        double weatherScore = 0.7; // valeur neutre par défaut
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (daysUntil >= 0 && daysUntil <= 16) {
            weather      = weatherService.getWeatherCondition(city, date);
            weatherScore = getWeatherScore(category, weather);
        }

        // ── 2. Score pondéré (somme = 1.0) ──
        double finalScore = (historical    * 0.35)
                          + (availability  * 0.25)
                          + (categoryScore * 0.20)
                          + (dayScore      * 0.10)
                          + (weatherScore  * 0.10);

        // ── 3. Estimation des participants ──
        double avgCapacity = getAverageCapacity(category);
        double expectedParticipants = avgCapacity * finalScore;

        // ── 4. Explication lisible ──
        String explanation = generateExplanation(
                historical, availability, categoryScore, dayScore,
                weatherScore, weather, category, date, time);

        return new PredictionResult(finalScore, expectedParticipants, explanation);
    }

    // ── Factor methods ────────────────────────────────────────────────────────

    /**
     * Score historique (0–1) : taux de remplissage moyen des événements
     * de la même catégorie à la même heure.
     */
    double getHistoricalScore(String category, LocalDate date, LocalTime time) {
        String catFilter = category != null ? " AND e.categorie = ?" : "";
        String sql = "SELECT AVG(fill_rate) AS avg_fill FROM (" +
                "  SELECT e.id, " +
                "    CASE WHEN e.capacite > 0 THEN 1.0 * COUNT(r.id) / e.capacite ELSE 0 END AS fill_rate" +
                "  FROM event e" +
                "  LEFT JOIN reservation_event r" +
                "         ON r.event_id = e.id" +
                "        AND (r.status = 'CONFIRMED' OR r.status IS NULL OR r.status = '')" +
                "  WHERE HOUR(e.date_event) = ?" + catFilter +
                "  GROUP BY e.id, e.capacite" +
                ") AS sub";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, time.getHour());
            if (category != null) stmt.setString(2, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double rate = rs.getDouble("avg_fill");
                    return Double.isNaN(rate) ? 0.5 : Math.min(1.0, rate);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PredictionService] historicalScore: " + e.getMessage());
        }
        return 0.5;
    }

    /**
     * Score de disponibilité (0–1) : pénalise les créneaux déjà chargés.
     */
    double getAvailabilityScore(LocalDate date, LocalTime time) {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM event
                WHERE DATE(date_event) = ?
                  AND ABS(HOUR(date_event) - ?) <= 1
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setInt(2, time.getHour());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int conflicts = rs.getInt("cnt");
                    return Math.max(0.1, 1.0 - (conflicts * 0.25));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PredictionService] availabilityScore: " + e.getMessage());
        }
        return 0.8;
    }

    /**
     * Score de pertinence catégorie/heure (0–1).
     */
    double getCategoryScore(String category, LocalTime time) {
        if (category == null) return 0.6;
        int hour = time.getHour();
        String cat = category.toLowerCase();
        return switch (cat) {
            case "yoga", "wellness", "meditation" ->
                    (hour >= 17 && hour <= 20) ? 0.92 :
                    (hour >= 7  && hour <= 9)  ? 0.80 : 0.55;
            case "sport" ->
                    (hour >= 16 && hour <= 19) ? 0.90 :
                    (hour >= 8  && hour <= 10) ? 0.75 : 0.50;
            case "conference", "atelier" ->
                    (hour >= 9  && hour <= 12) ? 0.88 :
                    (hour >= 14 && hour <= 16) ? 0.80 : 0.55;
            default ->
                    (hour >= 14 && hour <= 19) ? 0.70 : 0.60;
        };
    }

    /**
     * Score jour de semaine (0–1).
     */
    double getDayScore(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return switch (day) {
            case MONDAY    -> 0.45;
            case TUESDAY   -> 0.60;
            case WEDNESDAY -> 0.65;
            case THURSDAY  -> 0.70;
            case FRIDAY    -> 0.88;
            case SATURDAY  -> 0.85;
            case SUNDAY    -> 0.60;
        };
    }

    /**
     * Score météo (0–1) : favorise les événements indoor par temps de pluie
     * et les événements outdoor par beau temps.
     *
     * @param category catégorie de l'événement
     * @param weather  condition météo (ex: "Rain", "Clear", "Clouds")
     */
    double getWeatherScore(String category, String weather) {
        if (weather == null || weather.equalsIgnoreCase("Unknown")) return 0.7;

        boolean indoor  = WeatherService.isIndoor(category);
        boolean outdoor = WeatherService.isOutdoor(category);

        return switch (weather) {
            case "Rain", "Drizzle", "Thunderstorm" ->
                    indoor  ? 0.90 :   // pluie → indoor très favorisé
                    outdoor ? 0.30 :   // pluie → outdoor défavorisé
                              0.60;    // neutre

            case "Clear" ->
                    outdoor ? 0.95 :   // soleil → outdoor excellent
                    indoor  ? 0.65 :   // soleil → indoor légèrement moins attractif
                              0.75;    // neutre

            case "Clouds" ->
                    outdoor ? 0.70 :   // nuageux → outdoor correct
                    indoor  ? 0.75 :   // nuageux → indoor bon
                              0.72;

            case "Snow" ->
                    indoor  ? 0.85 :   // neige → indoor favorisé
                    outdoor ? 0.25 :   // neige → outdoor très défavorisé
                              0.55;

            case "Mist", "Fog", "Haze" ->
                    indoor  ? 0.80 : 0.50;

            default -> 0.70;
        };
    }

    // ── Explanation builder ───────────────────────────────────────────────────

    /**
     * Génère une explication lisible pour chaque facteur, météo incluse.
     */
    String generateExplanation(double historical, double availability,
                                double category, double day,
                                double weather, String weatherCondition,
                                String cat, LocalDate date, LocalTime time) {
        StringBuilder exp = new StringBuilder();

        // Historical
        if (historical >= 0.75)
            exp.append("✔ Forte participation historique à ").append(time.getHour()).append("h\n");
        else if (historical >= 0.50)
            exp.append("ℹ Participation historique modérée\n");
        else
            exp.append("⚠ Faible participation historique sur ce créneau\n");

        // Availability
        if (availability >= 0.75)
            exp.append("✔ Créneau très disponible — aucun conflit\n");
        else if (availability >= 0.50)
            exp.append("ℹ Quelques événements proches — légère concurrence\n");
        else
            exp.append("⚠ Créneau chargé — forte concurrence\n");

        // Category
        if (category >= 0.80)
            exp.append("✔ Horaire idéal pour la catégorie « ").append(cat).append(" »\n");
        else if (category >= 0.60)
            exp.append("ℹ Horaire correct pour « ").append(cat).append(" »\n");
        else
            exp.append("⚠ Horaire peu adapté à la catégorie « ").append(cat).append(" »\n");

        // Day
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        if (day >= 0.80)
            exp.append("✔ Jour très favorable : ").append(dayName).append("\n");
        else if (day >= 0.60)
            exp.append("ℹ Jour correct : ").append(dayName).append("\n");
        else
            exp.append("⚠ Jour peu actif : ").append(dayName).append("\n");

        // Météo
        if (!weatherCondition.equalsIgnoreCase("Unknown")) {
            String emoji = WeatherService.toEmoji(weatherCondition);
            boolean indoor  = WeatherService.isIndoor(cat);
            boolean outdoor = WeatherService.isOutdoor(cat);

            switch (weatherCondition) {
                case "Rain", "Drizzle" -> {
                    if (indoor)
                        exp.append(emoji).append(" Météo pluvieuse → favorable pour événement indoor ✔\n");
                    else if (outdoor)
                        exp.append(emoji).append(" Météo pluvieuse → défavorable pour événement outdoor ⚠\n");
                    else
                        exp.append(emoji).append(" Météo pluvieuse — impact modéré\n");
                }
                case "Thunderstorm" -> exp.append(emoji).append(" Orage prévu — impact négatif sur la participation ⚠\n");
                case "Clear" -> {
                    if (outdoor)
                        exp.append(emoji).append(" Beau temps → excellent pour événement outdoor ✔\n");
                    else
                        exp.append(emoji).append(" Beau temps → bonne météo pour l'événement ✔\n");
                }
                case "Clouds" -> exp.append(emoji).append(" Ciel nuageux — météo neutre\n");
                case "Snow"   -> exp.append(emoji).append(" Neige prévue — impact négatif sur les déplacements ⚠\n");
                case "Mist", "Fog", "Haze" -> exp.append(emoji).append(" Brouillard — visibilité réduite ℹ\n");
                default -> exp.append(emoji).append(" Météo : ").append(weatherCondition).append("\n");
            }
        }

        return exp.toString().trim();
    }

    // Surcharge sans météo (compatibilité avec l'ancien code)
    String generateExplanation(double historical, double availability,
                                double category, double day,
                                String cat, LocalDate date, LocalTime time) {
        return generateExplanation(historical, availability, category, day,
                0.7, "Unknown", cat, date, time);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private double getAverageCapacity(String category) {
        String sql = "SELECT AVG(capacite) FROM event"
                + (category != null ? " WHERE categorie = ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (category != null) stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return avg > 0 ? avg : 20.0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PredictionService] avgCapacity: " + e.getMessage());
        }
        return 20.0;
    }
}
