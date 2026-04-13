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
                    location VARCHAR(255) NOT NULL
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
        }
    }

    public void addEvent(Event event) throws SQLException {
        String sql = """
                INSERT INTO event(id_user, titre, description, date_event, lieu, capacite, categorie, image, title, event_date, location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public List<Event> getEvents(String query, String sortBy) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id, id_user, titre, description, date_event, lieu, capacite, categorie, image, title, event_date, location
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

    public void updateEvent(Event event) throws SQLException {
        String sql = """
                UPDATE event
                SET id_user = ?, titre = ?, description = ?, date_event = ?, lieu = ?, capacite = ?,
                    categorie = ?, image = ?, title = ?, event_date = ?, location = ?
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
            preparedStatement.setInt(12, event.getId());
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
                resultSet.getString("location")
        );
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
}
