package org.example.dao;

import org.example.model.Commentaire;
import org.example.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class CommentaireDAO {

    private static final String TABLE = "commentaire";

    private String resolveResourceColumn(Connection conn) throws SQLException {
        return hasColumn(conn, TABLE, "id_resource") ? "id_resource" : "resource_id";
    }

    private String resolveUserColumn(Connection conn) throws SQLException {
        return hasColumn(conn, TABLE, "id_user") ? "id_user" : "user_id";
    }

    private boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
    
    // CREATE
    public void create(Commentaire commentaire) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String resourceColumn = resolveResourceColumn(conn);
            String userColumn = resolveUserColumn(conn);
            String sql = "INSERT INTO commentaire (" + resourceColumn + ", " + userColumn + ", author_name, author_email, content, rating, created_at, approved) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
            PreparedStatement realStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            try (PreparedStatement ignored = realStmt) {
                realStmt.setInt(1, commentaire.getResourceId());
                realStmt.setInt(2, commentaire.getUserId());
                realStmt.setString(3, commentaire.getAuthorName());
                realStmt.setString(4, commentaire.getAuthorEmail());
                realStmt.setString(5, commentaire.getContent());
                realStmt.setInt(6, commentaire.getRating());
                realStmt.setBoolean(7, commentaire.isApproved());

                realStmt.executeUpdate();

                try (ResultSet keys = realStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        commentaire.setId(keys.getInt(1));
                    }
                }
            }
        }
    }
    
    // READ - By Resource ID (only approved)
    public List<Commentaire> findByResourceId(int resourceId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String resourceColumn = resolveResourceColumn(conn);
            String userColumn = resolveUserColumn(conn);
            String sql = "SELECT * FROM commentaire WHERE " + resourceColumn + " = ? AND approved = true ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            
                stmt.setInt(1, resourceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        commentaires.add(mapRowToCommentaire(rs, resourceColumn, userColumn));
                    }
                }
            }
        }
        return commentaires;
    }
    
    // READ - By Resource ID (all, for admin)
    public List<Commentaire> findByResourceIdAll(int resourceId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String resourceColumn = resolveResourceColumn(conn);
            String userColumn = resolveUserColumn(conn);
            String sql = "SELECT * FROM commentaire WHERE " + resourceColumn + " = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            
                stmt.setInt(1, resourceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        commentaires.add(mapRowToCommentaire(rs, resourceColumn, userColumn));
                    }
                }
            }
        }
        return commentaires;
    }
    
    // READ - By ID
    public Commentaire findById(int id) throws SQLException {
        String sql = "SELECT * FROM commentaire WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String resourceColumn = resolveResourceColumn(conn);
            String userColumn = resolveUserColumn(conn);
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCommentaire(rs, resourceColumn, userColumn);
                }
            }
        }
        return null;
    }
    
    // UPDATE
    public void update(Commentaire commentaire) throws SQLException {
        String sql = "UPDATE commentaire SET author_name = ?, author_email = ?, content = ?, rating = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, commentaire.getAuthorName());
            stmt.setString(2, commentaire.getAuthorEmail());
            stmt.setString(3, commentaire.getContent());
            stmt.setInt(4, commentaire.getRating());
            stmt.setInt(5, commentaire.getId());
            
            stmt.executeUpdate();
        }
    }
    
    // DELETE
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM commentaire WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    // APPROVE (Admin)
    public void approve(int id) throws SQLException {
        String sql = "UPDATE commentaire SET approved = true WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    // Get all unapproved comments (for admin)
    public List<Commentaire> findUnapproved() throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE approved = false ORDER BY created_at ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            String resourceColumn = resolveResourceColumn(conn);
            String userColumn = resolveUserColumn(conn);
            
            while (rs.next()) {
                commentaires.add(mapRowToCommentaire(rs, resourceColumn, userColumn));
            }
        }
        return commentaires;
    }
    
    // Helper
    private Commentaire mapRowToCommentaire(ResultSet rs, String resourceColumn, String userColumn) throws SQLException {
        Commentaire commentaire = new Commentaire();
        commentaire.setId(rs.getInt("id"));
        commentaire.setResourceId(rs.getInt(resourceColumn));
        commentaire.setUserId(rs.getInt(userColumn));
        commentaire.setAuthorName(rs.getString("author_name"));
        commentaire.setAuthorEmail(rs.getString("author_email"));
        commentaire.setContent(rs.getString("content"));
        commentaire.setRating(rs.getInt("rating"));
        commentaire.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        commentaire.setApproved(rs.getBoolean("approved"));
        return commentaire;
    }
}
