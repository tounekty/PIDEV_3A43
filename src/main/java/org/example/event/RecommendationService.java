package org.example.event;

import org.example.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 Système de recommandation personnalisée basé sur la similarité entre utilisateurs.
 *
 * Logique :
 *   1. Trouve les utilisateurs similaires (ont réservé/aimé les mêmes événements)
 *   2. Regarde ce qu'ils ont aimé d'autre
 *   3. Propose ces événements à l'utilisateur actuel
 *
 * Lecture seule — ne modifie rien.
 */
public class RecommendationService {

    private final EventService eventService = new EventService();

    /**
     * Recommande les meilleurs événements pour un utilisateur donné.
     *
     * @param userId       ID de l'utilisateur actuel
     * @param maxResults   nombre max de recommandations (ex: 5)
     * @return liste triée par score décroissant
     */
    public List<RecommendationResult> recommendForUser(int userId, int maxResults) {
        try {
            // 1. Récupérer les événements que l'utilisateur a déjà réservés/aimés
            Set<Integer> userEvents = getUserInteractions(userId);
            if (userEvents.isEmpty()) {
                // Pas encore d'historique → recommandations populaires
                return getPopularEvents(userId, maxResults);
            }

            // 2. Trouver les utilisateurs similaires
            Map<Integer, Double> similarUsers = findSimilarUsers(userId, userEvents);
            if (similarUsers.isEmpty()) {
                return getPopularEvents(userId, maxResults);
            }

            // 3. Récupérer les événements aimés par les utilisateurs similaires
            Map<Integer, Double> candidateScores = new HashMap<>();
            for (Map.Entry<Integer, Double> entry : similarUsers.entrySet()) {
                int otherUserId = entry.getKey();
                double similarity = entry.getValue();
                Set<Integer> theirEvents = getUserInteractions(otherUserId);
                for (int eventId : theirEvents) {
                    if (!userEvents.contains(eventId)) {
                        candidateScores.merge(eventId, similarity, Double::sum);
                    }
                }
            }

            // 4. Trier par score et récupérer les événements
            List<RecommendationResult> results = new ArrayList<>();
            candidateScores.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .limit(maxResults)
                    .forEach(entry -> {
                        try {
                            Event ev = eventService.getEventById(entry.getKey());
                            if (ev != null) {
                                double normalizedScore = Math.min(1.0, entry.getValue() / similarUsers.size());
                                String reason = buildReason(normalizedScore, ev.getCategorie());
                                results.add(new RecommendationResult(ev, normalizedScore, reason));
                            }
                        } catch (SQLException ignored) {}
                    });

            return results;

        } catch (SQLException e) {
            System.err.println("[RecommendationService] " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /**
     * Récupère tous les événements avec lesquels l'utilisateur a interagi
     * (réservations + likes).
     */
    private Set<Integer> getUserInteractions(int userId) throws SQLException {
        Set<Integer> events = new HashSet<>();

        // Réservations
        String sqlRes = """
                SELECT DISTINCT event_id FROM reservation_event
                WHERE user_id = ?
                  AND (status = 'CONFIRMED' OR status IS NULL OR status = '')
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlRes)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) events.add(rs.getInt("event_id"));
            }
        }

        // Likes
        String sqlLike = "SELECT DISTINCT event_id FROM event_like WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlLike)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) events.add(rs.getInt("event_id"));
            }
        }

        return events;
    }

    /**
     * Trouve les utilisateurs similaires basés sur les événements communs.
     * Retourne Map<userId, similarityScore>.
     */
    private Map<Integer, Double> findSimilarUsers(int userId, Set<Integer> userEvents) throws SQLException {
        Map<Integer, Double> similarity = new HashMap<>();

        // Pour chaque événement que l'utilisateur aime, trouve qui d'autre l'aime
        for (int eventId : userEvents) {
            // Réservations
            String sqlRes = """
                    SELECT user_id FROM reservation_event
                    WHERE event_id = ? AND user_id != ?
                      AND (status = 'CONFIRMED' OR status IS NULL OR status = '')
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sqlRes)) {
                stmt.setInt(1, eventId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int otherId = rs.getInt("user_id");
                        similarity.merge(otherId, 1.0, Double::sum);
                    }
                }
            }

            // Likes
            String sqlLike = "SELECT user_id FROM event_like WHERE event_id = ? AND user_id != ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sqlLike)) {
                stmt.setInt(1, eventId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int otherId = rs.getInt("user_id");
                        similarity.merge(otherId, 0.5, Double::sum);
                    }
                }
            }
        }

        return similarity;
    }

    /**
     * Fallback : événements populaires (les plus réservés).
     */
    private List<RecommendationResult> getPopularEvents(int userId, int maxResults) throws SQLException {
        String sql = """
                SELECT e.id, COUNT(r.id) AS popularity
                FROM event e
                LEFT JOIN reservation_event r ON r.event_id = e.id
                WHERE e.id NOT IN (
                    SELECT event_id FROM reservation_event WHERE user_id = ?
                )
                GROUP BY e.id
                ORDER BY popularity DESC
                LIMIT ?
                """;

        List<RecommendationResult> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, maxResults);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event ev = eventService.getEventById(rs.getInt("id"));
                    if (ev != null) {
                        int pop = rs.getInt("popularity");
                        double score = Math.min(1.0, pop / 10.0);
                        results.add(new RecommendationResult(ev, score,
                                "🔥 Événement populaire — " + pop + " réservation(s)"));
                    }
                }
            }
        }
        return results;
    }

    /**
     * Génère une explication lisible pour la recommandation.
     */
    private String buildReason(double score, String category) {
        StringBuilder sb = new StringBuilder();
        if (score >= 0.7) {
            sb.append("✔ Fortement recommandé — utilisateurs similaires adorent cet événement\n");
        } else if (score >= 0.4) {
            sb.append("ℹ Recommandé — plusieurs utilisateurs similaires ont aimé\n");
        } else {
            sb.append("ℹ Suggestion — basée sur vos préférences\n");
        }
        if (category != null && !category.isBlank()) {
            sb.append("🏷️ Catégorie : ").append(category);
        }
        return sb.toString().trim();
    }
}
