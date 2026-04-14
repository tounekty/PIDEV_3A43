package org.example.dao;

import org.example.model.Resource;
import org.example.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class ResourceDAO {
    
    // CREATE
    public void create(Resource resource) throws SQLException {
        String sql = "INSERT INTO resource (title, description, type, file_path, video_url, image_url, created_at, id_user) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int persistedUserId = resolvePersistedUserId(conn, resource.getUserId());
            
            stmt.setString(1, resource.getTitle());
            stmt.setString(2, resource.getDescription());
            stmt.setString(3, resource.getType());
            stmt.setString(4, resource.getFilePath());
            stmt.setString(5, resource.getVideoUrl());
            stmt.setString(6, resource.getImageUrl());
            stmt.setInt(7, persistedUserId);
            
            stmt.executeUpdate();
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    resource.setId(keys.getInt(1));
                }
            }
        }
    }

    private int resolvePersistedUserId(Connection conn, int preferredUserId) throws SQLException {
        String existsSql = "SELECT id FROM `user` WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(existsSql)) {
            stmt.setInt(1, preferredUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return preferredUserId;
                }
            }
        }

        String fallbackSql = "SELECT MIN(id) AS id FROM `user`";
        try (PreparedStatement stmt = conn.prepareStatement(fallbackSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt("id") > 0) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Aucun utilisateur trouvé dans la table user pour satisfaire la contrainte id_user.");
    }
    
    // READ - Get all
    public List<Resource> findAll() throws SQLException {
        List<Resource> resources = new ArrayList<>();
        String sql = "SELECT * FROM resource ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                resources.add(mapRowToResource(rs));
            }
        }
        return resources;
    }
    
    // READ - Get by ID
    public Resource findById(int id) throws SQLException {
        String sql = "SELECT * FROM resource WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToResource(rs);
                }
            }
        }
        return null;
    }
    
    // UPDATE
    public void update(Resource resource) throws SQLException {
        String sql = "UPDATE resource SET title = ?, description = ?, type = ?, file_path = ?, " +
                     "video_url = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, resource.getTitle());
            stmt.setString(2, resource.getDescription());
            stmt.setString(3, resource.getType());
            stmt.setString(4, resource.getFilePath());
            stmt.setString(5, resource.getVideoUrl());
            stmt.setString(6, resource.getImageUrl());
            stmt.setInt(7, resource.getId());
            
            stmt.executeUpdate();
        }
    }
    
    // DELETE
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM resource WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    // SEARCH
    public List<Resource> search(String query) throws SQLException {
        List<Resource> resources = new ArrayList<>();
        String sql = "SELECT * FROM resource WHERE LOWER(title) LIKE ? OR LOWER(description) LIKE ? " +
                     "ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query.toLowerCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapRowToResource(rs));
                }
            }
        }
        return resources;
    }
    
    // Helper
    private Resource mapRowToResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getInt("id"));
        resource.setTitle(rs.getString("title"));
        resource.setDescription(rs.getString("description"));
        resource.setType(rs.getString("type"));
        resource.setFilePath(rs.getString("file_path"));
        resource.setVideoUrl(rs.getString("video_url"));
        resource.setImageUrl(rs.getString("image_url"));
        resource.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        resource.setUserId(rs.getInt("id_user"));
        return resource;
    }
}
