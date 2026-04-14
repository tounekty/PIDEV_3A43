package com.mindcare.services;

import com.mindcare.entities.SubTicket;
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

public class SubTicketService {

    private final MyDatabase db = MyDatabase.getInstance();
    private final TicketService ticketService = new TicketService();

    private String idCol;
    private String messageCol;
    private String senderRoleCol;
    private String isInternalCol;
    private String isReadCol;
    private String createdAtCol;
    private String ticketIdCol;
    private String senderIdCol;

    public SubTicketService() {
        initSchema();
    }

    private void initSchema() {
        try (Connection connection = db.getConnection()) {
            Set<String> columns = SchemaUtils.loadColumns(connection.getMetaData(), "sub_ticket");
            idCol = SchemaUtils.firstExistingRequired(columns, Arrays.asList("id", "sub_ticket_id"), "sub_ticket.id");
            messageCol = SchemaUtils.firstExisting(columns, Arrays.asList("message", "content", "body"));
            senderRoleCol = SchemaUtils.firstExisting(columns, Arrays.asList("sender_role", "senderrole", "role"));
            isInternalCol = SchemaUtils.firstExisting(columns, Arrays.asList("is_internal", "internal_message", "isinternal"));
            isReadCol = SchemaUtils.firstExisting(columns, Arrays.asList("is_read", "isread", "read_status"));
            createdAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("created_at", "createdat"));
            ticketIdCol = SchemaUtils.firstExistingRequired(columns, Arrays.asList("ticket_id", "ticketid"), "sub_ticket.ticket_id");
            senderIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("sender_id", "senderid", "user_id"));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect sub_ticket table schema", exception);
        }
    }

    public List<SubTicket> getByTicket(int ticketId) {
        List<SubTicket> messages = new ArrayList<>();

        String sql = "SELECT " +
            idCol + " AS id, " +
            sqlSelect(messageCol, "message") + ", " +
            sqlSelect(senderRoleCol, "sender_role") + ", " +
            sqlSelect(isInternalCol, "is_internal") + ", " +
            sqlSelect(isReadCol, "is_read") + ", " +
            sqlSelect(createdAtCol, "created_at") + ", " +
            ticketIdCol + " AS ticket_id, " +
            sqlSelect(senderIdCol, "sender_id") +
            " FROM sub_ticket WHERE " + ticketIdCol + " = ? ORDER BY " +
            (createdAtCol != null ? createdAtCol : idCol) + " ASC";

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapSubTicket(rs));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch sub tickets", exception);
        }

        return messages;
    }

    public void add(SubTicket subTicket) {
        if (subTicket == null || subTicket.getMessage() == null || subTicket.getMessage().isBlank()) {
            throw new IllegalArgumentException("Sub-ticket message is required");
        }

        List<String> insertCols = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (messageCol != null) {
            insertCols.add(messageCol);
            values.add(subTicket.getMessage());
        }
        if (senderRoleCol != null) {
            insertCols.add(senderRoleCol);
            values.add(subTicket.getSenderRole() == null ? "ADMIN" : subTicket.getSenderRole());
        }
        if (isInternalCol != null) {
            insertCols.add(isInternalCol);
            values.add(subTicket.isInternal());
        }
        if (isReadCol != null) {
            insertCols.add(isReadCol);
            values.add(subTicket.isRead());
        }
        if (createdAtCol != null) {
            insertCols.add(createdAtCol);
            values.add(Timestamp.valueOf(LocalDateTime.now()));
        }

        insertCols.add(ticketIdCol);
        values.add(subTicket.getTicketId());

        if (senderIdCol != null && subTicket.getSenderId() != null) {
            insertCols.add(senderIdCol);
            values.add(subTicket.getSenderId());
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(values.size(), "?"));
        String sql = "INSERT INTO sub_ticket (" + String.join(",", insertCols) + ") VALUES (" + placeholders + ")";

        try (Connection connection = db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                bind(statement, values);
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        subTicket.setId(keys.getInt(1));
                    }
                }
            }

            ticketService.incrementMessageStats(connection, subTicket.getTicketId());
            connection.commit();
            System.out.println("[SubTicketService] Added message id=" + subTicket.getId() + " on ticket=" + subTicket.getTicketId());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to add sub ticket", exception);
        }
    }

    public void markAsRead(int id) {
        if (isReadCol == null) {
            return;
        }

        String sql = "UPDATE sub_ticket SET " + isReadCol + " = ? WHERE " + idCol + " = ?";
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, true);
            statement.setInt(2, id);
            statement.executeUpdate();
            System.out.println("[SubTicketService] Marked message as read id=" + id);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark sub ticket as read", exception);
        }
    }

    private String sqlSelect(String column, String alias) {
        return column == null ? ("NULL AS " + alias) : (column + " AS " + alias);
    }

    private SubTicket mapSubTicket(ResultSet rs) throws SQLException {
        SubTicket subTicket = new SubTicket();
        subTicket.setId(rs.getInt("id"));
        subTicket.setMessage(rs.getString("message"));
        subTicket.setSenderRole(rs.getString("sender_role"));
        subTicket.setInternal(rs.getBoolean("is_internal"));
        subTicket.setRead(rs.getBoolean("is_read"));

        subTicket.setCreatedAt(TimestampUtils.toLocalDateTime(rs.getTimestamp("created_at")));

        subTicket.setTicketId(rs.getInt("ticket_id"));

        int sender = rs.getInt("sender_id");
        subTicket.setSenderId(rs.wasNull() ? null : sender);

        return subTicket;
    }

    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
