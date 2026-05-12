package com.mindcare.services;

import com.mindcare.entities.CategoryTicket;
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

public class CategoryTicketService {

    private final MyDatabase db = MyDatabase.getInstance();

    private Set<String> columns;
    private String idCol;
    private String nameCol;
    private String descriptionCol;
    private String createdAtCol;
    private String updatedAtCol;
    private String createdByIdCol;
    private String updatedByIdCol;

    public CategoryTicketService() {
        initSchema();
    }

    private void initSchema() {
        try (Connection connection = db.getConnection()) {
            columns = SchemaUtils.loadColumns(connection.getMetaData(), "category_ticket");
            idCol = SchemaUtils.firstExistingRequired(columns, Arrays.asList("id", "category_id"), "id");
            nameCol = SchemaUtils.firstExisting(columns, Arrays.asList("name", "category_name"));
            descriptionCol = SchemaUtils.firstExisting(columns, Arrays.asList("description"));
            createdAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("created_at", "createdat"));
            updatedAtCol = SchemaUtils.firstExisting(columns, Arrays.asList("updated_at", "updatedat"));
            createdByIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("created_by_id", "created_by"));
            updatedByIdCol = SchemaUtils.firstExisting(columns, Arrays.asList("updated_by_id", "updated_by"));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect category_ticket table schema", exception);
        }
    }

    public List<CategoryTicket> getAll() {
        List<CategoryTicket> categories = new ArrayList<>();
        String orderBy = nameCol != null ? nameCol : idCol;

        String sql = "SELECT " +
            idCol + " AS id, " +
            sqlSelect(nameCol, "name") + ", " +
            sqlSelect(descriptionCol, "description") + ", " +
            sqlSelect(createdAtCol, "created_at") + ", " +
            sqlSelect(updatedAtCol, "updated_at") + ", " +
            sqlSelect(createdByIdCol, "created_by_id") + ", " +
            sqlSelect(updatedByIdCol, "updated_by_id") +
            " FROM category_ticket ORDER BY " + orderBy;

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                categories.add(mapCategory(rs));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch categories", exception);
        }
        return categories;
    }

    public CategoryTicket findById(int id) {
        String sql = "SELECT " +
            idCol + " AS id, " +
            sqlSelect(nameCol, "name") + ", " +
            sqlSelect(descriptionCol, "description") + ", " +
            sqlSelect(createdAtCol, "created_at") + ", " +
            sqlSelect(updatedAtCol, "updated_at") + ", " +
            sqlSelect(createdByIdCol, "created_by_id") + ", " +
            sqlSelect(updatedByIdCol, "updated_by_id") +
            " FROM category_ticket WHERE " + idCol + " = ?";

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapCategory(rs);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch category by id", exception);
        }
        return null;
    }

    private CategoryTicket mapCategory(ResultSet rs) throws SQLException {
        CategoryTicket category = new CategoryTicket();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));

        // Handle zero dates gracefully
        category.setCreatedAt(TimestampUtils.toLocalDateTime(rs.getTimestamp("created_at")));
        category.setUpdatedAt(TimestampUtils.toLocalDateTime(rs.getTimestamp("updated_at")));

        int createdById = rs.getInt("created_by_id");
        if (!rs.wasNull()) {
            category.setCreatedById(createdById);
        }
        
        int updatedById = rs.getInt("updated_by_id");
        if (!rs.wasNull()) {
            category.setUpdatedById(updatedById);
        }

        return category;
    }

    private String sqlSelect(String column, String alias) {
        return column != null ? column + " AS " + alias : "NULL AS " + alias;
    }
}
