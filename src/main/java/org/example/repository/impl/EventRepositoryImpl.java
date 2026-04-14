package org.example.repository.impl;

import org.example.config.DatabaseConnection;
import org.example.model.Event;
import org.example.repository.EventRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventRepositoryImpl implements EventRepository {
    private static final String TABLE_NAME = "`event`";

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS event (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    titre VARCHAR(255) NOT NULL,
                    description LONGTEXT NOT NULL,
                    date_event DATETIME NOT NULL,
                    lieu VARCHAR(255) NOT NULL,
                    capacite INT NOT NULL,
                    categorie VARCHAR(100),
                    image VARCHAR(255),
                    title VARCHAR(255) NOT NULL DEFAULT '',
                    event_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    location VARCHAR(255) NOT NULL DEFAULT '',
                    id_user INT NOT NULL
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public void save(Event event) throws SQLException {
        String sql = """
                INSERT INTO event (titre, description, date_event, lieu, capacite, categorie, image, title, event_date, location, id_user)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDateTime dateEvent = event.getDateEvent() != null ? event.getDateEvent() : LocalDateTime.now();
            LocalDateTime eventDate = event.getEventDate() != null ? event.getEventDate() : dateEvent;
            String title = normalize(event.getTitle());
            if (title == null || title.isBlank()) {
                title = event.getTitre();
            }
            String location = normalize(event.getLocation());
            if (location == null || location.isBlank()) {
                location = event.getLieu();
            }

            statement.setString(1, event.getTitre());
            statement.setString(2, event.getDescription());
            statement.setTimestamp(3, Timestamp.valueOf(dateEvent));
            statement.setString(4, event.getLieu());
            statement.setInt(5, event.getCapacite());
            statement.setString(6, normalize(event.getCategorie()));
            statement.setString(7, normalize(event.getImage()));
            statement.setString(8, title);
            statement.setTimestamp(9, Timestamp.valueOf(eventDate));
            statement.setString(10, location);
            statement.setInt(11, event.getIdUser() != null ? event.getIdUser() : 0);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Event event) throws SQLException {
        String sql = """
                UPDATE event
                SET titre = ?, description = ?, date_event = ?, lieu = ?, capacite = ?, categorie = ?, image = ?,
                    title = ?, event_date = ?, location = ?, id_user = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDateTime dateEvent = event.getDateEvent() != null ? event.getDateEvent() : LocalDateTime.now();
            LocalDateTime eventDate = event.getEventDate() != null ? event.getEventDate() : dateEvent;
            String title = normalize(event.getTitle());
            if (title == null || title.isBlank()) {
                title = event.getTitre();
            }
            String location = normalize(event.getLocation());
            if (location == null || location.isBlank()) {
                location = event.getLieu();
            }

            statement.setString(1, event.getTitre());
            statement.setString(2, event.getDescription());
            statement.setTimestamp(3, Timestamp.valueOf(dateEvent));
            statement.setString(4, event.getLieu());
            statement.setInt(5, event.getCapacite());
            statement.setString(6, normalize(event.getCategorie()));
            statement.setString(7, normalize(event.getImage()));
            statement.setString(8, title);
            statement.setTimestamp(9, Timestamp.valueOf(eventDate));
            statement.setString(10, location);
            statement.setInt(11, event.getIdUser() != null ? event.getIdUser() : 0);
            statement.setInt(12, event.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM event WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Event> findAll() throws SQLException {
        String sql = baseSelect() + " ORDER BY date_event DESC";
        return queryEvents(sql, null);
    }

    @Override
    public List<Event> findByQuery(String query, String sortBy) throws SQLException {
        String orderBy = orderByClause(sortBy);
        if (query == null || query.isBlank()) {
            String sql = baseSelect() + " " + orderBy;
            return queryEvents(sql, null);
        }

        String sql = baseSelect()
                + " WHERE (LOWER(titre) LIKE ? OR LOWER(lieu) LIKE ? OR LOWER(categorie) LIKE ?) "
                + orderBy;
        String search = "%" + query.toLowerCase() + "%";
        return queryEvents(sql, new String[]{search, search, search});
    }

    @Override
    public Event findById(int id) throws SQLException {
        String sql = baseSelect() + " WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEvent(resultSet);
                }
            }
        }
        return null;
    }

    private List<Event> queryEvents(String sql, String[] params) throws SQLException {
        List<Event> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    statement.setString(i + 1, params[i]);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }
        return events;
    }

    private String baseSelect() {
        return "SELECT id, titre, description, date_event, lieu, capacite, categorie, image, title, event_date, location, id_user FROM " + TABLE_NAME;
    }

    private String orderByClause(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "Par defaut".equalsIgnoreCase(sortBy)) {
            return "ORDER BY date_event DESC";
        }
        return switch (sortBy.trim()) {
            case "Date" -> "ORDER BY date_event DESC";
            case "Capacite" -> "ORDER BY capacite DESC";
            case "Categorie" -> "ORDER BY categorie ASC";
            case "Lieu" -> "ORDER BY lieu ASC";
            default -> "ORDER BY date_event DESC";
        };
    }

    private Event mapEvent(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        Integer idUser = resultSet.getObject("id_user") != null ? resultSet.getInt("id_user") : null;
        LocalDateTime dateEvent = toLocalDateTime(resultSet.getTimestamp("date_event"));
        LocalDateTime eventDate = toLocalDateTime(resultSet.getTimestamp("event_date"));

        return new Event(
                id,
                idUser,
                resultSet.getString("titre"),
                resultSet.getString("description"),
                dateEvent,
                resultSet.getString("lieu"),
                resultSet.getInt("capacite"),
                resultSet.getString("categorie"),
                resultSet.getString("image"),
                resultSet.getString("title"),
                eventDate,
                resultSet.getString("location")
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
