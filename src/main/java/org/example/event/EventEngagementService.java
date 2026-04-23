package org.example.event;

import org.example.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventEngagementService {
    public void initializeTables() throws SQLException {
        String likeSql = """
                CREATE TABLE IF NOT EXISTS event_like (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    event_id INT NOT NULL,
                    user_id INT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_event_like_user_event (event_id, user_id)
                )
                """;
        String reviewSql = """
                CREATE TABLE IF NOT EXISTS event_review (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    event_id INT NOT NULL,
                    user_id INT NOT NULL,
                    rating INT NOT NULL,
                    comment TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_event_review_user_event (event_id, user_id)
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(likeSql);
            statement.execute(reviewSql);
        }
    }

    public Set<Integer> getLikedEventIdsByUser(int userId) throws SQLException {
        String sql = "SELECT event_id FROM event_like WHERE user_id = ?";
        Set<Integer> liked = new HashSet<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    liked.add(resultSet.getInt("event_id"));
                }
            }
        }
        return liked;
    }

    public Map<Integer, Integer> getLikeCountsByEvent() throws SQLException {
        String sql = "SELECT event_id, COUNT(*) AS total FROM event_like GROUP BY event_id";
        Map<Integer, Integer> counts = new HashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                counts.put(resultSet.getInt("event_id"), resultSet.getInt("total"));
            }
        }
        return counts;
    }

    public boolean hasLikedEvent(int eventId, int userId) throws SQLException {
        String sql = "SELECT id FROM event_like WHERE event_id = ? AND user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean toggleLike(int eventId, int userId) throws SQLException {
        if (hasLikedEvent(eventId, userId)) {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM event_like WHERE event_id = ? AND user_id = ?")) {
                preparedStatement.setInt(1, eventId);
                preparedStatement.setInt(2, userId);
                preparedStatement.executeUpdate();
            }
            return false;
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO event_like(event_id, user_id) VALUES (?, ?)")) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        }
        return true;
    }

    public void addOrUpdateReview(int eventId, int userId, int rating, String comment) throws SQLException {
        if (rating < 1 || rating > 5) {
            throw new SQLException("La note doit etre comprise entre 1 et 5.");
        }
        if (comment == null || comment.trim().isBlank()) {
            throw new SQLException("Le commentaire est obligatoire.");
        }

        String sql = """
                INSERT INTO event_review(event_id, user_id, rating, comment)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, rating);
            preparedStatement.setString(4, comment.trim());
            preparedStatement.executeUpdate();
        }
    }

    public EventReview getReviewByEventAndUser(int eventId, int userId) throws SQLException {
        String sql = """
                SELECT r.id, r.event_id, r.user_id, u.username, r.rating, r.comment, COALESCE(r.updated_at, r.created_at) AS created_at
                FROM event_review r
                LEFT JOIN app_user u ON u.id = r.user_id
                WHERE r.event_id = ? AND r.user_id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    return new EventReview(
                            resultSet.getInt("id"),
                            resultSet.getInt("event_id"),
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getInt("rating"),
                            resultSet.getString("comment"),
                            createdAt == null ? LocalDateTime.now() : createdAt.toLocalDateTime()
                    );
                }
            }
        }
        return null;
    }

    public List<EventReview> getReviewsByEvent(int eventId) throws SQLException {
        String sql = """
                SELECT r.id, r.event_id, r.user_id, u.username, r.rating, r.comment, COALESCE(r.updated_at, r.created_at) AS created_at
                FROM event_review r
                LEFT JOIN app_user u ON u.id = r.user_id
                WHERE r.event_id = ?
                ORDER BY created_at DESC
                """;
        List<EventReview> reviews = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    reviews.add(new EventReview(
                            resultSet.getInt("id"),
                            resultSet.getInt("event_id"),
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getInt("rating"),
                            resultSet.getString("comment"),
                            createdAt == null ? LocalDateTime.now() : createdAt.toLocalDateTime()
                    ));
                }
            }
        }
        return reviews;
    }

    public Map<Integer, Integer> getReviewCountsByEvent() throws SQLException {
        String sql = "SELECT event_id, COUNT(*) AS total FROM event_review GROUP BY event_id";
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                counts.put(resultSet.getInt("event_id"), resultSet.getInt("total"));
            }
        }
        return counts;
    }

    public Map<Integer, Double> getAverageRatingsByEvent() throws SQLException {
        String sql = "SELECT event_id, AVG(rating) AS avg_rating FROM event_review GROUP BY event_id";
        Map<Integer, Double> ratings = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                ratings.put(resultSet.getInt("event_id"), resultSet.getDouble("avg_rating"));
            }
        }
        return ratings;
    }

    public void deleteEngagementForEvent(int eventId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement deleteLikes = connection.prepareStatement("DELETE FROM event_like WHERE event_id = ?");
                 PreparedStatement deleteReviews = connection.prepareStatement("DELETE FROM event_review WHERE event_id = ?")) {
                deleteLikes.setInt(1, eventId);
                deleteLikes.executeUpdate();
                deleteReviews.setInt(1, eventId);
                deleteReviews.executeUpdate();
            }
        }
    }
}
