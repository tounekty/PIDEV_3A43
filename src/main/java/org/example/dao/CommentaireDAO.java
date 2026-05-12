package org.example.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.example.config.DatabaseConnection;
import org.example.model.Commentaire;

public class CommentaireDAO {

    private static final String TABLE = "commentaire";
    private static final String VOTE_TABLE = "commentaire_vote";
    private static final String VOTE_LIKE = "LIKE";
    private static final String VOTE_DISLIKE = "DISLIKE";

    public CommentaireDAO() {
        // Vérifier et créer les colonnes likes/dislikes si nécessaire
        try {
            ensureLikeColumnsExist();
            ensureVoteTableExists();
        } catch (SQLException e) {
            System.err.println("Avertissement: Impossible de vérifier les colonnes like/dislike: " + e.getMessage());
        }
    }

    private void ensureLikeColumnsExist() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (!hasColumn(conn, TABLE, "like_count")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE " + TABLE + " ADD COLUMN like_count INT DEFAULT 0");
                    System.out.println("Colonne 'like_count' créée");
                }
            }
            if (!hasColumn(conn, TABLE, "dislike_count")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE " + TABLE + " ADD COLUMN dislike_count INT DEFAULT 0");
                    System.out.println("Colonne 'dislike_count' créée");
                }
            }
        }
    }

    private void ensureVoteTableExists() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + VOTE_TABLE + " ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT, "
                            + "id_commentaire INT NOT NULL, "
                            + "id_user INT NOT NULL, "
                            + "vote_type VARCHAR(10) NOT NULL, "
                            + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                            + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                            + "UNIQUE KEY uk_commentaire_vote_user (id_commentaire, id_user), "
                            + "INDEX idx_commentaire_vote_commentaire (id_commentaire), "
                            + "INDEX idx_commentaire_vote_user (id_user), "
                            + "CONSTRAINT fk_commentaire_vote_commentaire "
                            + "FOREIGN KEY (id_commentaire) REFERENCES commentaire(id) ON DELETE CASCADE"
                            + ")"
            );
        }
    }

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
            int persistedUserId = resolvePersistedUserId(conn, commentaire.getUserId());
            String resourceColumn = resolveResourceColumn(conn);
            String userColumn = resolveUserColumn(conn);
            String sql = "INSERT INTO commentaire (" + resourceColumn + ", " + userColumn + ", author_name, author_email, content, rating, created_at, approved) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
            PreparedStatement realStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            try (PreparedStatement ignored = realStmt) {
                realStmt.setInt(1, commentaire.getResourceId());
                realStmt.setInt(2, persistedUserId);
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

        throw new SQLException("Aucun utilisateur trouve dans la table user pour ajouter le commentaire.");
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
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, id);
        }
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
    
    // LIKE/DISLIKE management
    public Commentaire addLike(int commentId, int userId) throws SQLException {
        return applyVote(commentId, userId, VOTE_LIKE);
    }

    public void removeLike(int commentId) throws SQLException {
        ensureLikeColumnsExist();
        String sql = "UPDATE commentaire SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.executeUpdate();
        }
    }

    public Commentaire addDislike(int commentId, int userId) throws SQLException {
        return applyVote(commentId, userId, VOTE_DISLIKE);
    }

    public void removeDislike(int commentId) throws SQLException {
        ensureLikeColumnsExist();
        String sql = "UPDATE commentaire SET dislike_count = GREATEST(dislike_count - 1, 0) WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.executeUpdate();
        }
    }

    private Commentaire applyVote(int commentId, int userId, String voteType) throws SQLException {
        if (userId <= 0) {
            throw new SQLException("Utilisateur connecte invalide pour voter.");
        }
        if (!VOTE_LIKE.equals(voteType) && !VOTE_DISLIKE.equals(voteType)) {
            throw new SQLException("Type de vote invalide.");
        }

        ensureLikeColumnsExist();
        ensureVoteTableExists();

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String previousVote = findUserVoteForUpdate(conn, commentId, userId);
                if (previousVote == null) {
                    insertVote(conn, commentId, userId, voteType);
                    incrementVoteCounter(conn, commentId, voteType);
                } else if (!previousVote.equals(voteType)) {
                    updateVote(conn, commentId, userId, voteType);
                    decrementVoteCounter(conn, commentId, previousVote);
                    incrementVoteCounter(conn, commentId, voteType);
                }

                Commentaire updated = findById(conn, commentId);
                conn.commit();
                return updated;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private String findUserVoteForUpdate(Connection conn, int commentId, int userId) throws SQLException {
        String sql = "SELECT vote_type FROM " + VOTE_TABLE + " WHERE id_commentaire = ? AND id_user = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("vote_type") : null;
            }
        }
    }

    private void insertVote(Connection conn, int commentId, int userId, String voteType) throws SQLException {
        String sql = "INSERT INTO " + VOTE_TABLE + " (id_commentaire, id_user, vote_type) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.setInt(2, userId);
            stmt.setString(3, voteType);
            stmt.executeUpdate();
        }
    }

    private void updateVote(Connection conn, int commentId, int userId, String voteType) throws SQLException {
        String sql = "UPDATE " + VOTE_TABLE + " SET vote_type = ? WHERE id_commentaire = ? AND id_user = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voteType);
            stmt.setInt(2, commentId);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
        }
    }

    private void incrementVoteCounter(Connection conn, int commentId, String voteType) throws SQLException {
        String column = VOTE_LIKE.equals(voteType) ? "like_count" : "dislike_count";
        String sql = "UPDATE commentaire SET " + column + " = " + column + " + 1 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.executeUpdate();
        }
    }

    private void decrementVoteCounter(Connection conn, int commentId, String voteType) throws SQLException {
        String column = VOTE_LIKE.equals(voteType) ? "like_count" : "dislike_count";
        String sql = "UPDATE commentaire SET " + column + " = GREATEST(" + column + " - 1, 0) WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.executeUpdate();
        }
    }

    // Helper
    private Commentaire findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM commentaire WHERE id = ?";
        String resourceColumn = resolveResourceColumn(conn);
        String userColumn = resolveUserColumn(conn);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCommentaire(rs, resourceColumn, userColumn);
                }
            }
        }
        return null;
    }

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
        
        // Récupérer les likes/dislikes avec fallback pour les commentaires existants
        int likeCount = 0;
        int dislikeCount = 0;
        try {
            // Essayer de lire la colonne
            likeCount = rs.getInt("like_count");
            if (rs.wasNull()) likeCount = 0;
        } catch (SQLException e) {
            likeCount = 0;
        }
        try {
            dislikeCount = rs.getInt("dislike_count");
            if (rs.wasNull()) dislikeCount = 0;
        } catch (SQLException e) {
            dislikeCount = 0;
        }
        commentaire.setLikeCount(likeCount);
        commentaire.setDislikeCount(dislikeCount);
        
        return commentaire;
    }
}
