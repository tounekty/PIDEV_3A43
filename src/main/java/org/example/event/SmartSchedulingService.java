package org.example.event;

import org.example.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * 🔥 Assistant intelligent de planification prédictive.
 *
 * Modèle de score :
 *   Score = 0.25 × historique
 *         + 0.20 × disponibilité
 *         + 0.15 × popularité_catégorie
 *         + 0.15 × jour_semaine
 *         + 0.15 × saisonnalité
 *         + 0.10 × engagement_utilisateur
 *
 * Lecture seule — n'écrit rien en base.
 */
public class SmartSchedulingService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ── Version 2: PredictionService integration ──────────────────────────────
    private final PredictionService predictionService = new PredictionService();

    // ── Public model ──────────────────────────────────────────────────────────

    public static class SuggestedSlot {
        public final LocalDateTime dateTime;
        public final double        score;           // 0–100
        public final double        successProb;     // 0–100 %
        public final String        badge;           // 🟢 / 🟡 / 🔴
        public final String        explanation;     // bullet-point reasons
        public final String        label;

        // Component scores (for display)
        public final double scoreHistorique;
        public final double scoreDisponibilite;
        public final double scoreCategorie;
        public final double scoreJour;
        public final double scoreSaison;
        public final double scoreEngagement;

        // Version 2: enriched prediction
        public final PredictionResult prediction;

        public SuggestedSlot(LocalDateTime dt, double hist, double dispo,
                             double cat, double jour, double saison, double eng,
                             String explanation) {
            this(dt, hist, dispo, cat, jour, saison, eng, explanation, null);
        }

        public SuggestedSlot(LocalDateTime dt, double hist, double dispo,
                             double cat, double jour, double saison, double eng,
                             String explanation, PredictionResult prediction) {
            this.dateTime           = dt;
            this.scoreHistorique    = hist;
            this.scoreDisponibilite = dispo;
            this.scoreCategorie     = cat;
            this.scoreJour          = jour;
            this.scoreSaison        = saison;
            this.scoreEngagement    = eng;

            this.score = Math.min(100, Math.max(0,
                    0.25 * hist + 0.20 * dispo + 0.15 * cat
                  + 0.15 * jour + 0.15 * saison + 0.10 * eng));

            // If prediction available, blend its probability with v1 score
            double blendedProb = prediction != null
                    ? (score * 0.5 + prediction.getSuccessProbability() * 100 * 0.5)
                    : Math.min(99, Math.max(10, score * 0.92 + 5));

            this.successProb = blendedProb;

            this.badge = successProb >= 75 ? "🟢 Excellent"
                       : successProb >= 50 ? "🟡 Moyen"
                       : "🔴 Faible";

            // Merge explanations from both v1 and v2
            String v2Expl = prediction != null ? prediction.getExplanation() : "";
            this.explanation = (explanation != null && !explanation.isBlank())
                    ? explanation + (v2Expl.isBlank() ? "" : "\n" + v2Expl)
                    : v2Expl;

            this.label      = dt.format(DATE_FMT) + " à " + dt.format(TIME_FMT);
            this.prediction = prediction;
        }
    }

    // ── Main API ──────────────────────────────────────────────────────────────

    public List<SuggestedSlot> suggestBestSlots(String category, int durationMinutes) {
        try {
            // Pre-compute all factors once
            Map<DayOfWeek, Double> histByDay  = getHistFillRateByDay(category);
            Map<Integer,   Double> histByHour = getHistFillRateByHour(category);
            double                 catPop     = getCategoryPopularity(category);
            double                 avgEng     = getAvgUserEngagement();
            Set<LocalDate>         busyDates  = getBusyDates(30);

            List<SuggestedSlot> candidates = new ArrayList<>();
            LocalDate today = LocalDate.now();

            // Candidate hours: morning, afternoon, evening
            int[] hours = {9, 10, 14, 15, 17, 18, 19};

            for (int dayOffset = 1; dayOffset <= 21; dayOffset++) {
                LocalDate d = today.plusDays(dayOffset);
                DayOfWeek dow = d.getDayOfWeek();

                for (int hour : hours) {
                    // ── 1. Historique (0–100) ──
                    double hist = 0.5 * histByDay.getOrDefault(dow, 50.0)
                                + 0.5 * histByHour.getOrDefault(hour, 50.0);

                    // ── 2. Disponibilité (0–100) ──
                    double dispo = busyDates.contains(d) ? 20.0 : 80.0;
                    // Penalty if many events same day
                    int eventsOnDay = countEventsOnDate(d);
                    dispo = Math.max(0, dispo - eventsOnDay * 15);

                    // ── 3. Popularité catégorie (0–100) ──
                    double cat = catPop;

                    // ── 4. Jour de semaine (0–100) ──
                    double jour = dayOfWeekScore(dow, category);

                    // ── 5. Saisonnalité (0–100) ──
                    double saison = seasonalityScore(d, hour, category);

                    // ── 6. Engagement utilisateur (0–100) ──
                    double eng = Math.min(100, avgEng * 1.2);

                    // ── Build explanation ──
                    String expl = buildExplanation(dow, hour, hist, dispo,
                            cat, jour, saison, eng, category, eventsOnDay);

                    // ── Version 2: enrich with PredictionService ──
                    PredictionResult prediction = predictionService.predict(
                            category, d, LocalTime.of(hour, 0));

                    candidates.add(new SuggestedSlot(
                            LocalDateTime.of(d, LocalTime.of(hour, 0)),
                            hist, dispo, cat, jour, saison, eng, expl, prediction));
                }
            }

            // Sort by score desc, deduplicate (keep best per day), top 5
            candidates.sort(Comparator.comparingDouble((SuggestedSlot s) -> s.score).reversed());
            List<SuggestedSlot> result = new ArrayList<>();
            Set<LocalDate> usedDays = new HashSet<>();
            for (SuggestedSlot s : candidates) {
                if (!usedDays.contains(s.dateTime.toLocalDate())) {
                    result.add(s);
                    usedDays.add(s.dateTime.toLocalDate());
                }
                if (result.size() == 5) break;
            }
            return result;

        } catch (SQLException e) {
            System.err.println("[SmartScheduling] " + e.getMessage());
            return fallback();
        }
    }

    /** Summary text for the analysis panel. */
    public String getAnalysisSummary(String category) {
        try {
            int    total   = getTotalEvents(category);
            double avgFill = getAvgFillRate(category);
            String bestDay = getBestDay(category);
            String bestHr  = getBestHour(category);
            double eng     = getAvgUserEngagement();

            return String.format(
                "📊 Analyse sur %d événement(s)%s\n" +
                "📈 Taux de remplissage moyen : %.0f%%\n" +
                "📅 Meilleur jour : %s\n" +
                "⏰ Meilleure heure : %s\n" +
                "👥 Engagement moyen utilisateurs : %.0f%%",
                total,
                category != null ? " (" + category + ")" : "",
                avgFill, bestDay, bestHr, eng);
        } catch (SQLException e) {
            return "Analyse indisponible.";
        }
    }

    // ── Factor computations ───────────────────────────────────────────────────

    /** Historical fill rate per day of week (0–100). */
    private Map<DayOfWeek, Double> getHistFillRateByDay(String category) throws SQLException {
        String sql = "SELECT DAYOFWEEK(e.date_event) AS dow, " +
                "AVG(CASE WHEN e.capacite>0 THEN 100.0*COUNT(r.id)/e.capacite ELSE 0 END) AS fr " +
                "FROM event e LEFT JOIN reservation_event r " +
                "ON r.event_id=e.id AND (r.status='CONFIRMED' OR r.status IS NULL OR r.status='') " +
                (category != null ? "WHERE e.categorie=? " : "") +
                "GROUP BY DAYOFWEEK(e.date_event)";
        Map<DayOfWeek, Double> m = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            if (category != null) s.setString(1, category);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) m.put(mysqlDow(rs.getInt("dow")), rs.getDouble("fr"));
            }
        }
        return m;
    }

    /** Historical fill rate per hour (0–100). */
    private Map<Integer, Double> getHistFillRateByHour(String category) throws SQLException {
        String sql = "SELECT HOUR(e.date_event) AS hr, " +
                "AVG(CASE WHEN e.capacite>0 THEN 100.0*COUNT(r.id)/e.capacite ELSE 0 END) AS fr " +
                "FROM event e LEFT JOIN reservation_event r " +
                "ON r.event_id=e.id AND (r.status='CONFIRMED' OR r.status IS NULL OR r.status='') " +
                (category != null ? "WHERE e.categorie=? " : "") +
                "GROUP BY HOUR(e.date_event)";
        Map<Integer, Double> m = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            if (category != null) s.setString(1, category);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) m.put(rs.getInt("hr"), rs.getDouble("fr"));
            }
        }
        return m;
    }

    /** Category popularity score (0–100) based on reservation count. */
    private double getCategoryPopularity(String category) throws SQLException {
        if (category == null) return 50.0;
        String sql = "SELECT COUNT(r.id) AS total FROM event e " +
                "LEFT JOIN reservation_event r ON r.event_id=e.id " +
                "WHERE e.categorie=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, category);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("total");
                    return Math.min(100, 30 + count * 2.0); // scale
                }
            }
        }
        return 50.0;
    }

    /** Average user engagement: avg reservations per user (0–100). */
    private double getAvgUserEngagement() throws SQLException {
        String sql = "SELECT AVG(cnt) FROM " +
                "(SELECT COUNT(*) AS cnt FROM reservation_event " +
                "WHERE status='CONFIRMED' OR status IS NULL OR status='' " +
                "GROUP BY user_id) AS sub";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) {
                double avg = rs.getDouble(1);
                return Math.min(100, avg * 20); // 5 reservations = 100
            }
        }
        return 50.0;
    }

    /** Dates in next N days that already have events. */
    private Set<LocalDate> getBusyDates(int days) throws SQLException {
        String sql = "SELECT DISTINCT DATE(date_event) FROM event " +
                "WHERE date_event >= NOW() AND date_event <= DATE_ADD(NOW(), INTERVAL ? DAY)";
        Set<LocalDate> busy = new HashSet<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, days);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate(1);
                    if (d != null) busy.add(d.toLocalDate());
                }
            }
        }
        return busy;
    }

    /** Count events on a specific date. */
    private int countEventsOnDate(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM event WHERE DATE(date_event)=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) { return 0; }
    }

    // ── Scoring helpers ───────────────────────────────────────────────────────

    /** Day-of-week score (0–100) with category awareness. */
    private double dayOfWeekScore(DayOfWeek dow, String cat) {
        boolean isWellness = cat != null && (cat.equalsIgnoreCase("yoga")
                || cat.equalsIgnoreCase("wellness") || cat.equalsIgnoreCase("meditation"));
        boolean isSport    = cat != null && cat.equalsIgnoreCase("sport");
        boolean isConf     = cat != null && (cat.equalsIgnoreCase("conference")
                || cat.equalsIgnoreCase("atelier"));

        return switch (dow) {
            case MONDAY    -> isConf ? 70 : 40;
            case TUESDAY   -> 60;
            case WEDNESDAY -> 65;
            case THURSDAY  -> 70;
            case FRIDAY    -> isSport ? 85 : 75;
            case SATURDAY  -> isWellness ? 90 : isSport ? 80 : 60;
            case SUNDAY    -> isWellness ? 85 : 50;
        };
    }

    /** Seasonality score (0–100): month + time-of-day + school periods. */
    private double seasonalityScore(LocalDate date, int hour, String cat) {
        double score = 50.0;

        // Time of day
        if (hour >= 9  && hour <= 11) score += 10; // morning
        if (hour >= 14 && hour <= 16) score += 8;  // afternoon
        if (hour >= 17 && hour <= 19) score += 15; // peak evening

        // Month effect
        Month m = date.getMonth();
        score += switch (m) {
            case JANUARY, FEBRUARY -> 5;   // new year motivation
            case MARCH, APRIL      -> 10;  // spring
            case MAY               -> 12;
            case JUNE              -> 8;   // pre-exam
            case JULY, AUGUST      -> -10; // summer holidays
            case SEPTEMBER         -> 15;  // back to school
            case OCTOBER, NOVEMBER -> 10;
            case DECEMBER          -> -5;  // holiday season
        };

        // Category-specific time preference
        if (cat != null) {
            boolean morning = hour >= 8 && hour <= 11;
            boolean evening = hour >= 17;
            if ((cat.equalsIgnoreCase("yoga") || cat.equalsIgnoreCase("wellness")) && evening) score += 10;
            if (cat.equalsIgnoreCase("conference") && morning) score += 10;
            if (cat.equalsIgnoreCase("sport") && evening) score += 12;
        }

        return Math.min(100, Math.max(0, score));
    }

    // ── Explanation builder ───────────────────────────────────────────────────

    private String buildExplanation(DayOfWeek dow, int hour, double hist,
                                     double dispo, double cat, double jour,
                                     double saison, double eng,
                                     String category, int eventsOnDay) {
        StringBuilder sb = new StringBuilder();
        String dayName = dow.getDisplayName(TextStyle.FULL, Locale.FRENCH);

        // Historique
        if (hist >= 70) sb.append("✔ Forte participation historique à ").append(hour).append("h le ").append(dayName).append("\n");
        else if (hist >= 40) sb.append("✔ Participation modérée historiquement\n");
        else sb.append("ℹ Peu de données historiques pour ce créneau\n");

        // Disponibilité
        if (dispo >= 70) sb.append("✔ Aucun conflit détecté dans le calendrier\n");
        else if (eventsOnDay > 0) sb.append("⚠ ").append(eventsOnDay).append(" événement(s) déjà ce jour — concurrence possible\n");
        else sb.append("⚠ Créneau chargé\n");

        // Catégorie
        if (category != null) {
            if (cat >= 70) sb.append("✔ Catégorie « ").append(category).append(" » très populaire\n");
            else sb.append("ℹ Catégorie « ").append(category).append(" » — popularité modérée\n");
        }

        // Jour
        if (jour >= 75) sb.append("✔ Très bon taux de participation le ").append(dayName).append("\n");
        else if (jour >= 50) sb.append("ℹ Jour standard — participation correcte\n");
        else sb.append("⚠ Jour peu actif — envisager un autre jour\n");

        // Saisonnalité
        String period = hour < 12 ? "matin" : hour < 17 ? "après-midi" : "soir";
        if (saison >= 70) sb.append("✔ Créneau ").append(period).append(" favorable cette période\n");
        else if (saison < 40) sb.append("⚠ Période moins active (vacances / mois creux)\n");

        // Engagement
        if (eng >= 60) sb.append("✔ Bonne fidélité des étudiants (engagement élevé)\n");

        return sb.toString().trim();
    }

    // ── Stats helpers ─────────────────────────────────────────────────────────

    private int getTotalEvents(String cat) throws SQLException {
        String sql = "SELECT COUNT(*) FROM event" + (cat != null ? " WHERE categorie=?" : "");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            if (cat != null) s.setString(1, cat);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private double getAvgFillRate(String cat) throws SQLException {
        String sql = "SELECT AVG(CASE WHEN e.capacite>0 THEN 100.0*COUNT(r.id)/e.capacite ELSE 0 END) " +
                "FROM event e LEFT JOIN reservation_event r ON r.event_id=e.id " +
                (cat != null ? "WHERE e.categorie=?" : "");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            if (cat != null) s.setString(1, cat);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        }
    }

    private String getBestDay(String cat) throws SQLException {
        return getHistFillRateByDay(cat).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getDisplayName(TextStyle.FULL, Locale.FRENCH))
                .orElse("Indéterminé");
    }

    private String getBestHour(String cat) throws SQLException {
        return getHistFillRateByHour(cat).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + "h00 – " + (e.getKey() + 1) + "h00")
                .orElse("Indéterminé");
    }

    private DayOfWeek mysqlDow(int d) {
        return switch (d) {
            case 1 -> DayOfWeek.SUNDAY;   case 2 -> DayOfWeek.MONDAY;
            case 3 -> DayOfWeek.TUESDAY;  case 4 -> DayOfWeek.WEDNESDAY;
            case 5 -> DayOfWeek.THURSDAY; case 6 -> DayOfWeek.FRIDAY;
            default -> DayOfWeek.SATURDAY;
        };
    }

    private List<SuggestedSlot> fallback() {
        List<SuggestedSlot> list = new ArrayList<>();
        LocalDate base = LocalDate.now().plusDays(1);
        int[][] cfg = {{0,10},{1,17},{3,14},{5,9},{7,18}};
        double[] sc = {78,74,70,66,62};
        for (int i = 0; i < cfg.length; i++) {
            LocalDateTime dt = LocalDateTime.of(base.plusDays(cfg[i][0]), LocalTime.of(cfg[i][1], 0));
            PredictionResult pred = predictionService.predict(null, dt.toLocalDate(), dt.toLocalTime());
            list.add(new SuggestedSlot(dt, sc[i], 80, 60, 65, 60, 55,
                    "✔ Créneau standard recommandé\nℹ Données historiques insuffisantes", pred));
        }
        return list;
    }
}
