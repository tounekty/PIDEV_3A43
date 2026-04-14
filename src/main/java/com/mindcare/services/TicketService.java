package com.mindcare.services;

import com.mindcare.entities.Ticket;
import com.mindcare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TicketService {

    private final MyDatabase db = MyDatabase.getInstance();

    private Set<String> columns;
    private String idCol;
    private String userIdCol;
    private String categoryIdCol;
    private String subjectCol;
    private String descriptionCol;
    private String statusCol;
    private String priorityCol;
    private String resolutionCol;
    private String messageCountCol;
    private String createdAtCol;
    private String closedAtCol;
    private String lastMessageAtCol;
    private String updatedAtCol;
    private String aiSentimentCol;
    private String aiUrgencyCol;
    private String aiSuggestedPriorityCol;
    private String aiSummaryCol;
    private String acknowledgedByAdCol;
    private String createdByIdCol;
    private String updatedByIdCol;
    private String deletedByIdCol;
    private String deletedAtCol;

    public TicketService() {
        initSchema();
    }

    private void initSchema() {
        try (Connection connection = db.getConnection()) {
            columns = SchemaUtils.loadColumns(connection.getMetaData(), "ticket");
            idCol = SchemaUtils.firstExistingRequired(columns, Arrays.asList("id", "ticket_id"), "id");
            userIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("user_id", "client_id", "creator_id", "created_by"));
            categoryIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("category_id", "ticket_category_id"));
            subjectCol = SchemaUtils.firstExisting(columns, Arrays.asList("subject", "title", "ticket_subject"));
            descriptionCol = SchemaUtils.firstExisting(columns, Arrays.asList("description", "content", "body", "message"));
            statusCol = SchemaUtils.firstExisting(columns, Arrays.asList("status", "ticket_status"));
            priorityCol = SchemaUtils.firstExisting(columns, Arrays.asList("priority", "ticket_priority"));
            resolutionCol = SchemaUtils.firstExisting(columns, Arrays.asList("resolution", "admin_resolution"));
            messageCountCol = SchemaUtils.firstExisting(columns, Arrays.asList("message_count", "messages_count"));
            createdAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("created_at", "createdat", "creation_date"));
            closedAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("closed_at", "closedat"));
            lastMessageAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("last_message_at", "lastmessageat"));
            updatedAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("updated_at", "updatedat"));
            aiSentimentCol = SchemaUtils.firstExisting(columns, Arrays.asList("ai_sentiment", "sentiment"));
            aiUrgencyCol = SchemaUtils.firstExisting(columns, Arrays.asList("ai_urgency", "urgency_score"));
            aiSuggestedPriorityCol = SchemaUtils.firstExisting(columns, Arrays.asList("ai_suggested_priority", "suggested_priority"));
            aiSummaryCol = SchemaUtils.firstExisting(columns, Arrays.asList("ai_summary", "summary"));
            acknowledgedByAdCol = SchemaUtils.firstExisting(columns, Arrays.asList("acknowledged_by_ad", "acknowledged_by_admin"));
            createdByIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("created_by_id", "created_by"));
            updatedByIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("updated_by_id", "updated_by"));
            deletedByIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("deleted_by_id", "deleted_by"));
            deletedAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("deleted_at", "deletedat"));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect ticket table schema", exception);
        }
    }

    public static boolean isValidStatusTransition(Ticket.Status from, Ticket.Status to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return switch (from) {
            case OPEN -> to == Ticket.Status.IN_PROGRESS;
            case IN_PROGRESS -> to == Ticket.Status.WAITING_USER;
            case WAITING_USER -> to == Ticket.Status.CLOSED;
            case CLOSED -> false;
        };
    }

    public List<Ticket> getAll() {
        List<Ticket> tickets = new ArrayList<>();
        String orderBy = createdAtCol != null ? createdAtCol : idCol;

        String sql = "SELECT " +
            idCol + " AS id, " +
            sqlSelect(userIdCol, "user_id") + ", " +
            sqlSelect(categoryIdCol, "category_id") + ", " +
            sqlSelect(subjectCol, "subject") + ", " +
            sqlSelect(descriptionCol, "description") + ", " +
            sqlSelect(statusCol, "status") + ", " +
            sqlSelect(priorityCol, "priority") + ", " +
            sqlSelect(resolutionCol, "resolution") + ", " +
            sqlSelect(messageCountCol, "message_count") + ", " +
            sqlSelect(createdAtCol, "created_at") + ", " +
            sqlSelect(closedAtCol, "closed_at") + ", " +
            sqlSelect(aiSentimentCol, "ai_sentiment") + ", " +
            sqlSelect(aiUrgencyCol, "ai_urgency") + ", " +
            sqlSelect(aiSuggestedPriorityCol, "ai_suggested_priority") + ", " +
            sqlSelect(aiSummaryCol, "ai_summary") +
            " FROM ticket ORDER BY " + orderBy + " DESC";

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                tickets.add(mapTicket(rs));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch tickets", exception);
        }
        return tickets;
    }

    public void add(Ticket ticket) {
        if (ticket == null || ticket.getSubject() == null || ticket.getSubject().isBlank()) {
            throw new IllegalArgumentException("Ticket subject is required");
        }
        if (userIdCol != null && (ticket.getUserId() == null || ticket.getUserId() <= 0)) {
            throw new IllegalArgumentException("A valid user is required to create this ticket");
        }

        List<String> insertCols = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        addIfPresent(insertCols, values, userIdCol, ticket.getUserId());
        addIfPresent(insertCols, values, categoryIdCol, ticket.getCategoryId());
        addIfPresent(insertCols, values, subjectCol, ticket.getSubject());
        addIfPresent(insertCols, values, descriptionCol, normalizeText(ticket.getDescription()));
        addIfPresent(insertCols, values, statusCol, normalizeStatus(ticket.getStatus()));
        addIfPresent(insertCols, values, priorityCol, normalizePriority(ticket.getPriority()));
        addIfPresent(insertCols, values, resolutionCol, emptyToNull(ticket.getResolution()));
        addIfPresent(insertCols, values, messageCountCol, ticket.getMessageCount());
        addIfPresent(insertCols, values, aiSentimentCol, emptyToNull(ticket.getAiSentiment()));
        addIfPresent(insertCols, values, aiUrgencyCol, ticket.getAiUrgency());
        addIfPresent(insertCols, values, aiSuggestedPriorityCol, emptyToNull(ticket.getAiSuggestedPriority()));
        addIfPresent(insertCols, values, aiSummaryCol, emptyToNull(ticket.getAiSummary()));
        
        // Handle columns with default values (set to 0 or NULL if they exist but no value provided)
        if (categoryIdCol != null && (ticket.getCategoryId() == null || ticket.getCategoryId() <= 0)) {
            insertCols.add(categoryIdCol);
            values.add(1); // Default to "General" category
        }
        if (acknowledgedByAdCol != null) {
            insertCols.add(acknowledgedByAdCol);
            values.add(0);
        }
        if (createdByIdCol != null) {
            insertCols.add(createdByIdCol);
            values.add(userIdCol != null ? ticket.getUserId() : 0);
        }
        if (updatedByIdCol != null) {
            insertCols.add(updatedByIdCol);
            values.add(userIdCol != null ? ticket.getUserId() : 0);
        }
        if (deletedByIdCol != null) {
            insertCols.add(deletedByIdCol);
            values.add(0);
        }
        if (deletedAtCol != null) {
            insertCols.add(deletedAtCol);
            values.add(null);
        }

        if (createdAtCol != null) {
            insertCols.add(createdAtCol);
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }
        if (updatedAtCol != null) {
            insertCols.add(updatedAtCol);
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }

        if (insertCols.isEmpty()) {
            throw new IllegalStateException("No writable columns detected for ticket table");
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(values.size(), "?"));
        String sql = "INSERT INTO ticket (" + String.join(",", insertCols) + ") VALUES (" + placeholders + ")";

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            bind(statement, values);
            statement.executeUpdate();

            try (ResultSet generated = statement.getGeneratedKeys()) {
                if (generated.next()) {
                    ticket.setId(generated.getInt(1));
                }
            }
            System.out.println("[TicketService] Added ticket id=" + ticket.getId());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to add ticket", exception);
        }
    }

    public void update(Ticket ticket) {
        if (ticket == null || ticket.getId() <= 0) {
            throw new IllegalArgumentException("Ticket id is required for update");
        }

        Ticket current = findById(ticket.getId());
        if (current == null) {
            throw new IllegalStateException("Ticket not found: " + ticket.getId());
        }

        if (!isValidStatusTransition(current.getStatus(), ticket.getStatus())) {
            throw new IllegalArgumentException("Invalid status transition: " + current.getStatus() + " -> " + ticket.getStatus());
        }

        List<String> setParts = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (subjectCol != null) {
            setParts.add(subjectCol + " = ?");
            values.add(ticket.getSubject());
        }
        if (descriptionCol != null) {
            setParts.add(descriptionCol + " = ?");
            values.add(normalizeText(ticket.getDescription()));
        }
        if (userIdCol != null && ticket.getUserId() != null && ticket.getUserId() > 0) {
            setParts.add(userIdCol + " = ?");
            values.add(ticket.getUserId());
        }
        if (statusCol != null) {
            setParts.add(statusCol + " = ?");
            values.add(normalizeStatus(ticket.getStatus()));
        }
        if (priorityCol != null) {
            setParts.add(priorityCol + " = ?");
            values.add(normalizePriority(ticket.getPriority()));
        }
        if (resolutionCol != null) {
            setParts.add(resolutionCol + " = ?");
            values.add(emptyToNull(ticket.getResolution()));
        }
        if (closedAtCol != null && ticket.getStatus() == Ticket.Status.CLOSED) {
            setParts.add(closedAtCol + " = ?");
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }
        if (updatedAtCol != null) {
            setParts.add(updatedAtCol + " = ?");
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }
        if (updatedByIdCol != null && userIdCol != null) {
            setParts.add(updatedByIdCol + " = ?");
            values.add(ticket.getUserId() != null && ticket.getUserId() > 0 ? ticket.getUserId() : 0);
        }

        if (setParts.isEmpty()) {
            return;
        }

        String sql = "UPDATE ticket SET " + String.join(", ", setParts) + " WHERE " + idCol + " = ?";
        values.add(ticket.getId());

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
            System.out.println("[TicketService] Updated ticket id=" + ticket.getId());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update ticket", exception);
        }
    }

    public void delete(int id) {
        if (id <= 0) {
            return;
        }
        String sql = "DELETE FROM ticket WHERE " + idCol + " = ?";
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            System.out.println("[TicketService] Deleted ticket id=" + id);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete ticket", exception);
        }
    }

    public Ticket findById(int id) {
        String sql = "SELECT " +
            idCol + " AS id, " +
            sqlSelect(userIdCol, "user_id") + ", " +
            sqlSelect(subjectCol, "subject") + ", " +
            sqlSelect(descriptionCol, "description") + ", " +
            sqlSelect(statusCol, "status") + ", " +
            sqlSelect(priorityCol, "priority") + ", " +
            sqlSelect(resolutionCol, "resolution") + ", " +
            sqlSelect(messageCountCol, "message_count") + ", " +
            sqlSelect(createdAtCol, "created_at") + ", " +
            sqlSelect(closedAtCol, "closed_at") + ", " +
            sqlSelect(aiSentimentCol, "ai_sentiment") + ", " +
            sqlSelect(aiUrgencyCol, "ai_urgency") + ", " +
            sqlSelect(aiSuggestedPriorityCol, "ai_suggested_priority") + ", " +
            sqlSelect(aiSummaryCol, "ai_summary") +
            " FROM ticket WHERE " + idCol + " = ?";

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapTicket(rs);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch ticket by id", exception);
        }
        return null;
    }

    void incrementMessageStats(Connection connection, int ticketId) throws SQLException {
        List<String> updates = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (messageCountCol != null) {
            updates.add(messageCountCol + " = COALESCE(" + messageCountCol + ", 0) + 1");
        }
        if (lastMessageAtCol != null) {
            updates.add(lastMessageAtCol + " = ?");
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }
        if (updatedAtCol != null) {
            updates.add(updatedAtCol + " = ?");
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }

        if (updates.isEmpty()) {
            return;
        }

        String sql = "UPDATE ticket SET " + String.join(", ", updates) + " WHERE " + idCol + " = ?";
        values.add(ticketId);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
        }
    }

    private String sqlSelect(String column, String alias) {
        return column == null ? ("NULL AS " + alias) : (column + " AS " + alias);
    }

    private void addIfPresent(List<String> columnsList, List<Object> values, String column, Object value) {
        if (column != null && value != null) {
            columnsList.add(column);
            values.add(value);
        }
    }

    private String normalizeStatus(Ticket.Status status) {
        return (status == null ? Ticket.Status.OPEN : status).name();
    }

    private String normalizePriority(Ticket.Priority priority) {
        return (priority == null ? Ticket.Priority.MEDIUM : priority).name();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        ticket.setUserId(rs.wasNull() ? null : userId);
        int categoryId = rs.getInt("category_id");
        ticket.setCategoryId(rs.wasNull() ? null : categoryId);
        ticket.setSubject(rs.getString("subject"));
        ticket.setDescription(rs.getString("description"));
        ticket.setStatus(Ticket.Status.fromDb(rs.getString("status")));
        ticket.setPriority(Ticket.Priority.fromDb(rs.getString("priority")));
        ticket.setResolution(rs.getString("resolution"));
        ticket.setMessageCount(rs.getInt("message_count"));
        ticket.setCreatedAt(TimestampUtils.toLocalDateTime(rs.getTimestamp("created_at")));
        ticket.setClosedAt(TimestampUtils.toLocalDateTime(rs.getTimestamp("closed_at")));
        ticket.setAiSentiment(rs.getString("ai_sentiment"));

        int urgency = rs.getInt("ai_urgency");
        ticket.setAiUrgency(rs.wasNull() ? null : urgency);

        ticket.setAiSuggestedPriority(rs.getString("ai_suggested_priority"));
        ticket.setAiSummary(rs.getString("ai_summary"));
        return ticket;
    }



    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
