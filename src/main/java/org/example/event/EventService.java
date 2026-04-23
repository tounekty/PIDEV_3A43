package org.example.event;

import org.example.config.DatabaseConnection;
import org.example.util.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventService {
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS event (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    id_user INT NULL,
                    titre VARCHAR(255) NOT NULL,
                    description LONGTEXT NOT NULL,
                    date_event DATETIME NOT NULL,
                    lieu VARCHAR(255) NOT NULL,
                    capacite INT NOT NULL,
                    categorie VARCHAR(100) NULL,
                    image VARCHAR(255) NULL,
                    title VARCHAR(255) NOT NULL,
                    event_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    location VARCHAR(255) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    duration_minutes INT NOT NULL DEFAULT 60
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            ensureColumnExists(statement, "id_user", "ALTER TABLE event ADD COLUMN id_user INT NULL AFTER id");
            ensureColumnExists(statement, "titre", "ALTER TABLE event ADD COLUMN titre VARCHAR(255) NOT NULL AFTER id_user");
            ensureColumnExists(statement, "description", "ALTER TABLE event ADD COLUMN description LONGTEXT NOT NULL AFTER titre");
            ensureColumnExists(statement, "date_event", "ALTER TABLE event ADD COLUMN date_event DATETIME NOT NULL AFTER description");
            ensureColumnExists(statement, "lieu", "ALTER TABLE event ADD COLUMN lieu VARCHAR(255) NOT NULL AFTER date_event");
            ensureColumnExists(statement, "capacite", "ALTER TABLE event ADD COLUMN capacite INT NOT NULL DEFAULT 0 AFTER lieu");
            ensureColumnExists(statement, "categorie", "ALTER TABLE event ADD COLUMN categorie VARCHAR(100) NULL AFTER capacite");
            ensureColumnExists(statement, "image", "ALTER TABLE event ADD COLUMN image VARCHAR(255) NULL AFTER categorie");
            ensureColumnExists(statement, "title", "ALTER TABLE event ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT '' AFTER image");
            ensureColumnExists(statement, "event_date", "ALTER TABLE event ADD COLUMN event_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER title");
            ensureColumnExists(statement, "location", "ALTER TABLE event ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT '' AFTER event_date");
            ensureColumnExists(statement, "status", "ALTER TABLE event ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER location");
            ensureColumnExists(statement, "duration_minutes", "ALTER TABLE event ADD COLUMN duration_minutes INT NOT NULL DEFAULT 60 AFTER status");
        }
    }

    public void addEvent(Event event) throws SQLException {
        validateEvent(event);
        assertNoConflicts(event, null);

        String sql = """
                INSERT INTO event(id_user, titre, description, date_event, lieu, capacite, categorie, image,
                                  title, event_date, location, status, duration_minutes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp eventTimestamp = Timestamp.valueOf(event.getDateEvent());

            if (event.getIdUser() == null) {
                preparedStatement.setNull(1, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(1, event.getIdUser());
            }
            preparedStatement.setString(2, event.getTitre());
            preparedStatement.setString(3, event.getDescription());
            preparedStatement.setTimestamp(4, eventTimestamp);
            preparedStatement.setString(5, event.getLieu());
            preparedStatement.setInt(6, event.getCapacite());
            preparedStatement.setString(7, emptyToNull(event.getCategorie()));
            preparedStatement.setString(8, emptyToNull(event.getImage()));
            preparedStatement.setString(9, event.getTitle());
            preparedStatement.setTimestamp(10, Timestamp.valueOf(event.getEventDate()));
            preparedStatement.setString(11, event.getLocation());
            preparedStatement.setString(12, normalizeStatus(event.getStatus()));
            preparedStatement.setInt(13, normalizeDuration(event.getDurationMinutes()));
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    event.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Event> getAllEvents() throws SQLException {
        return getEvents(null, null);
    }

    public Event getEventById(int id) throws SQLException {
        String sql = """
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title,
                       event_date, location, status, duration_minutes
                FROM event
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEvent(resultSet);
                }
            }
        }

        return null;
    }

    public List<Event> getEvents(String query, String sortBy) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title,
                       event_date, location, status, duration_minutes
                FROM event
                """);
        List<String> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append("WHERE titre LIKE ? OR description LIKE ? OR lieu LIKE ? OR categorie LIKE ? OR title LIKE ? OR location LIKE ? ");
            String search = "%" + query.trim() + "%";
            for (int i = 0; i < 6; i++) {
                params.add(search);
            }
        }

        String orderBy = "id";
        if (sortBy != null) {
            switch (sortBy) {
                case "Date": orderBy = "date_event"; break;
                case "Capacite": orderBy = "capacite"; break;
                case "Categorie": orderBy = "categorie"; break;
                case "Lieu": orderBy = "lieu"; break;
                default: orderBy = "id"; break;
            }
        }
        sql.append("ORDER BY ").append(orderBy);

        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                preparedStatement.setString(i + 1, params.get(i));
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }

        return events;
    }

    public List<Event> searchEvents(String query) throws SQLException {
        return getEvents(query, null);
    }

    public List<Event> filterEvents(String category, LocalDate date, String location) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title,
                       event_date, location, status, duration_minutes
                FROM event
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND categorie = ? ");
            params.add(category.trim());
        }
        if (date != null) {
            sql.append(" AND DATE(date_event) = ? ");
            params.add(java.sql.Date.valueOf(date));
        }
        if (location != null && !location.isBlank()) {
            sql.append(" AND lieu LIKE ? ");
            params.add("%" + location.trim() + "%");
        }
        sql.append(" ORDER BY date_event ASC ");

        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof java.sql.Date sqlDate) {
                    preparedStatement.setDate(i + 1, sqlDate);
                } else {
                    preparedStatement.setString(i + 1, String.valueOf(value));
                }
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }
        return events;
    }

    public List<Event> getEventsInRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = """
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title,
                       event_date, location, status, duration_minutes
                FROM event
                WHERE status <> 'CANCELLED'
                  AND date_event >= ?
                  AND date_event < ?
                ORDER BY date_event ASC
                """;

        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setTimestamp(1, Timestamp.valueOf(start));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }
        return events;
    }

    public List<Event> getUserPlanning(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = """
                SELECT DISTINCT e.id, e.id_user, e.titre, e.description, e.date_event, e.lieu, e.capacite,
                       e.categorie, e.image, e.title, e.event_date, e.location, e.status, e.duration_minutes
                FROM event e
                LEFT JOIN reservation_event r ON r.event_id = e.id
                WHERE e.status <> 'CANCELLED'
                  AND e.date_event >= ?
                  AND e.date_event < ?
                  AND (e.id_user = ? OR r.user_id = ?)
                ORDER BY e.date_event ASC
                """;

        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setTimestamp(1, Timestamp.valueOf(start));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(end));
            preparedStatement.setInt(3, userId);
            preparedStatement.setInt(4, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }
        return events;
    }

    public List<Event> getConflictingEvents(Event event, Integer excludeEventId) throws SQLException {
        LocalDateTime start = event.getDateEvent();
        LocalDateTime end = start.plusMinutes(normalizeDuration(event.getDurationMinutes()));
        String sql = """
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title,
                       event_date, location, status, duration_minutes
                FROM event
                WHERE status <> 'CANCELLED'
                  AND date_event < ?
                  AND DATE_ADD(date_event, INTERVAL duration_minutes MINUTE) > ?
                  AND (lieu = ? OR (? IS NOT NULL AND id_user = ?))
                  AND (? IS NULL OR id <> ?)
                ORDER BY date_event ASC
                """;

        List<Event> conflicts = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setTimestamp(1, Timestamp.valueOf(end));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(start));
            preparedStatement.setString(3, event.getLieu());
            if (event.getIdUser() == null) {
                preparedStatement.setNull(4, java.sql.Types.INTEGER);
                preparedStatement.setNull(5, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(4, event.getIdUser());
                preparedStatement.setInt(5, event.getIdUser());
            }
            if (excludeEventId == null) {
                preparedStatement.setNull(6, java.sql.Types.INTEGER);
                preparedStatement.setNull(7, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(6, excludeEventId);
                preparedStatement.setInt(7, excludeEventId);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    conflicts.add(mapEvent(resultSet));
                }
            }
        }
        return conflicts;
    }

    public List<Event> getUpcomingEvents() throws SQLException {
        return getEventsByDateBoundary(true);
    }

    public List<Event> getPastEvents() throws SQLException {
        return getEventsByDateBoundary(false);
    }

    public List<Event> getFullEvents() throws SQLException {
        String sql = """
                SELECT e.id, e.id_user, e.titre, e.description, e.date_event, e.lieu, e.capacite,
                       e.categorie, e.image, e.title, e.event_date, e.location, e.status, e.duration_minutes
                FROM event e
                LEFT JOIN reservation_event r ON r.event_id = e.id
                GROUP BY e.id, e.id_user, e.titre, e.description, e.date_event, e.lieu, e.capacite,
                         e.categorie, e.image, e.title, e.event_date, e.location, e.status, e.duration_minutes
                HAVING COUNT(r.id) >= e.capacite
                ORDER BY e.date_event ASC
                """;
        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                events.add(mapEvent(resultSet));
            }
        }
        return events;
    }

    public void updateEventStatus(int eventId, String status) throws SQLException {
        String sql = "UPDATE event SET status = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, normalizeStatus(status));
            preparedStatement.setInt(2, eventId);
            preparedStatement.executeUpdate();
        }
    }

    public void updateEvent(Event event) throws SQLException {
        validateEvent(event);
        assertNoConflicts(event, event.getId());

        String sql = """
                UPDATE event
                SET id_user = ?, titre = ?, description = ?, date_event = ?, lieu = ?, capacite = ?,
                    categorie = ?, image = ?, title = ?, event_date = ?, location = ?, status = ?, duration_minutes = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            Timestamp eventTimestamp = Timestamp.valueOf(event.getDateEvent());

            if (event.getIdUser() == null) {
                preparedStatement.setNull(1, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(1, event.getIdUser());
            }
            preparedStatement.setString(2, event.getTitre());
            preparedStatement.setString(3, event.getDescription());
            preparedStatement.setTimestamp(4, eventTimestamp);
            preparedStatement.setString(5, event.getLieu());
            preparedStatement.setInt(6, event.getCapacite());
            preparedStatement.setString(7, emptyToNull(event.getCategorie()));
            preparedStatement.setString(8, emptyToNull(event.getImage()));
            preparedStatement.setString(9, event.getTitle());
            preparedStatement.setTimestamp(10, Timestamp.valueOf(event.getEventDate()));
            preparedStatement.setString(11, event.getLocation());
            preparedStatement.setString(12, normalizeStatus(event.getStatus()));
            preparedStatement.setInt(13, normalizeDuration(event.getDurationMinutes()));
            preparedStatement.setInt(14, event.getId());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteEvent(int id) throws SQLException {
        String sql = "DELETE FROM event WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }
    }

    private Event mapEvent(ResultSet resultSet) throws SQLException {
        Integer idUser = resultSet.getObject("id_user") == null ? null : resultSet.getInt("id_user");
        Timestamp dateEventTimestamp = resultSet.getTimestamp("date_event");
        Timestamp eventDateTimestamp = resultSet.getTimestamp("event_date");

        return new Event(
                resultSet.getInt("id"),
                idUser,
                resultSet.getString("titre"),
                resultSet.getString("description"),
                dateEventTimestamp == null ? LocalDateTime.now() : dateEventTimestamp.toLocalDateTime(),
                resultSet.getString("lieu"),
                resultSet.getInt("capacite"),
                resultSet.getString("categorie"),
                resultSet.getString("image"),
                resultSet.getString("title"),
                eventDateTimestamp == null ? LocalDateTime.now() : eventDateTimestamp.toLocalDateTime(),
                resultSet.getString("location"),
                resultSet.getString("status"),
                resultSet.getInt("duration_minutes")
        );
    }

    private List<Event> getEventsByDateBoundary(boolean upcoming) throws SQLException {
        String operator = upcoming ? ">=" : "<";
        String sql = """
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title,
                       event_date, location, status, duration_minutes
                FROM event
                WHERE date_event %s ?
                ORDER BY date_event ASC
                """.formatted(operator);
        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }
        return events;
    }

    private void assertNoConflicts(Event event, Integer excludeEventId) throws SQLException {
        List<Event> conflicts = getConflictingEvents(event, excludeEventId);
        if (!conflicts.isEmpty()) {
            Event first = conflicts.get(0);
            throw new SQLException("Conflit detecte avec l'evenement #" + first.getId() + " (" + first.getTitre() + ").");
        }
    }

    private void validateEvent(Event event) throws SQLException {
        try {
            ValidationUtil.validateEventComplete(
                    event.getTitre(),
                    event.getDescription(),
                    event.getDateEvent(),
                    event.getLieu(),
                    event.getCapacite(),
                    event.getCategorie(),
                    event.getIdUser()
            );
            int duration = normalizeDuration(event.getDurationMinutes());
            event.setDurationMinutes(duration);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Erreur de validation : " + e.getMessage(), e);
        }
    }

    private void ensureColumnExists(Statement statement, String columnName, String alterSql) throws SQLException {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM event LIKE '" + columnName + "'")) {
            if (!columns.next()) {
                statement.execute(alterSql);
            }
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ACTIVE";
        }
        return value.trim().toUpperCase();
    }

    private int normalizeDuration(int durationMinutes) {
        if (durationMinutes <= 0) {
            return 60;
        }
        return Math.min(durationMinutes, 720);
    }
}
