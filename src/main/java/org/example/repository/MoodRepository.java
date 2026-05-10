package org.example.repository;

import com.mindcare.db.DBConnection;
import org.example.model.Mood;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MoodRepository {

    public int create(Mood mood) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(buildInsertSql(conn), Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            if (hasColumn(conn, "mood", "mood_type")) {
                ps.setString(index++, mood.getMoodType());
            }
            if (hasColumn(conn, "mood", "humeur")) {
                ps.setString(index++, mood.getMoodType());
            }
            if (hasColumn(conn, "mood", "mood_date")) {
                ps.setDate(index++, Date.valueOf(mood.getMoodDate()));
            }
            if (hasColumn(conn, "mood", "date_humeur")) {
                ps.setDate(index++, Date.valueOf(mood.getMoodDate()));
            }
            if (hasColumn(conn, "mood", "datemood")) {
                ps.setDate(index++, Date.valueOf(mood.getMoodDate()));
            }
            if (hasColumn(conn, "mood", "intensite")) {
                ps.setInt(index++, mood.getStressLevel() == null ? 5 : mood.getStressLevel());
            }
            if (hasColumn(conn, "mood", "id_user")) {
                ps.setInt(index++, resolveUserId(conn, mood.getUserId()));
            }
            if (hasColumn(conn, "mood", "user_id")) {
                ps.setInt(index++, resolveUserId(conn, mood.getUserId()));
            }
            if (hasColumn(conn, "mood", "note")) {
                ps.setString(index++, mood.getNote());
            }
            if (hasColumn(conn, "mood", "commentaire")) {
                ps.setString(index++, mood.getNote());
            }
            if (hasColumn(conn, "mood", "description")) {
                ps.setString(index++, mood.getNote());
            }
            if (hasColumn(conn, "mood", "stress_level")) {
                setNullableInt(ps, index++, mood.getStressLevel());
            }
            if (hasColumn(conn, "mood", "energy_level")) {
                setNullableInt(ps, index++, mood.getEnergyLevel());
            }
            if (hasColumn(conn, "mood", "sleep_time")) {
                ps.setString(index++, mood.getSleepTime());
            }
            if (hasColumn(conn, "mood", "wake_time")) {
                ps.setString(index++, mood.getWakeTime());
            }
            if (hasColumn(conn, "mood", "sleep_hours")) {
                setSleepHours(ps, index++, mood.getSleepHours());
            }
            if (hasColumn(conn, "mood", "admin_comment")) {
                ps.setString(index++, mood.getAdminComment());
            }
            if (hasColumn(conn, "mood", "support_email_sent")) {
                ps.setBoolean(index++, mood.isSupportEmailSent());
            }
            if (hasColumn(conn, "mood", "case_status")) {
                ps.setString(index++, mood.getCaseStatus());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No generated id returned.");
            }
        }
    }

    public List<Mood> findAll() throws SQLException {
        List<Mood> moods = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(buildSelectAllSql(conn));
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                moods.add(map(rs));
            }
        }
        return moods;
    }

    public Mood findById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(buildSelectByIdSql(conn))) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public boolean update(Mood mood) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(buildUpdateSql(conn))) {
            int index = 1;
            if (hasColumn(conn, "mood", "mood_type")) {
                ps.setString(index++, mood.getMoodType());
            }
            if (hasColumn(conn, "mood", "humeur")) {
                ps.setString(index++, mood.getMoodType());
            }
            if (hasColumn(conn, "mood", "mood_date")) {
                ps.setDate(index++, Date.valueOf(mood.getMoodDate()));
            }
            if (hasColumn(conn, "mood", "date_humeur")) {
                ps.setDate(index++, Date.valueOf(mood.getMoodDate()));
            }
            if (hasColumn(conn, "mood", "datemood")) {
                ps.setDate(index++, Date.valueOf(mood.getMoodDate()));
            }
            if (hasColumn(conn, "mood", "intensite")) {
                ps.setInt(index++, mood.getStressLevel() == null ? 5 : mood.getStressLevel());
            }
            if (hasColumn(conn, "mood", "note")) {
                ps.setString(index++, mood.getNote());
            }
            if (hasColumn(conn, "mood", "commentaire")) {
                ps.setString(index++, mood.getNote());
            }
            if (hasColumn(conn, "mood", "description")) {
                ps.setString(index++, mood.getNote());
            }
            if (hasColumn(conn, "mood", "stress_level")) {
                setNullableInt(ps, index++, mood.getStressLevel());
            }
            if (hasColumn(conn, "mood", "energy_level")) {
                setNullableInt(ps, index++, mood.getEnergyLevel());
            }
            if (hasColumn(conn, "mood", "sleep_time")) {
                ps.setString(index++, mood.getSleepTime());
            }
            if (hasColumn(conn, "mood", "wake_time")) {
                ps.setString(index++, mood.getWakeTime());
            }
            if (hasColumn(conn, "mood", "sleep_hours")) {
                setSleepHours(ps, index++, mood.getSleepHours());
            }
            if (hasColumn(conn, "mood", "admin_comment")) {
                ps.setString(index++, mood.getAdminComment());
            }
            if (hasColumn(conn, "mood", "support_email_sent")) {
                ps.setBoolean(index++, mood.isSupportEmailSent());
            }
            if (hasColumn(conn, "mood", "case_status")) {
                ps.setString(index++, mood.getCaseStatus());
            }
            ps.setInt(index, mood.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM mood WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Mood> findByDate(java.time.LocalDate date) throws SQLException {
        List<Mood> moods = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, " + typeExpr(conn) + " AS mood_type, " +
                     dateExpr(conn) + " AS mood_date, " + noteExpr(conn) + " AS note, " +
                     stressLevelExpr(conn) + " AS stress_level, " + energyLevelExpr(conn) + " AS energy_level, " +
                     sleepTimeExpr(conn) + " AS sleep_time, " + wakeTimeExpr(conn) + " AS wake_time, " +
                     sleepHoursExpr(conn) + " AS sleep_hours, " + adminCommentExpr(conn) + " AS admin_comment, " +
                     supportEmailSentExpr(conn) + " AS support_email_sent, " +
                     caseStatusExpr(conn) + " AS case_status, " +
                     studentNameExpr(conn) + " AS student_name, " + studentEmailExpr(conn) + " AS student_email, " +
                     userIdExpr(conn) + " AS user_id FROM mood WHERE " +
                     dateExpr(conn) + " = ? ORDER BY id DESC")) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    moods.add(map(rs));
                }
            }
        }
        return moods;
    }

    private Mood map(ResultSet rs) throws SQLException {
        return new Mood(
                rs.getInt("id"),
                rs.getString("mood_type"),
                rs.getDate("mood_date").toLocalDate(),
                rs.getString("note"),
                getNullableInt(rs, "stress_level"),
                getNullableInt(rs, "energy_level"),
                rs.getString("sleep_time"),
                rs.getString("wake_time"),
                getSleepHours(rs),
                rs.getString("admin_comment"),
                rs.getBoolean("support_email_sent"),
                rs.getString("student_name"),
                rs.getString("student_email"),
                getNullableInt(rs, "user_id"),
                rs.getString("case_status")
        );
    }

    private String buildSelectAllSql(Connection conn) throws SQLException {
        return "SELECT id, " + typeExpr(conn) + " AS mood_type, " + dateExpr(conn) + " AS mood_date, " +
                noteExpr(conn) + " AS note, " + stressLevelExpr(conn) + " AS stress_level, " +
                energyLevelExpr(conn) + " AS energy_level, " + sleepTimeExpr(conn) + " AS sleep_time, " +
                wakeTimeExpr(conn) + " AS wake_time, " + sleepHoursExpr(conn) + " AS sleep_hours, " +
                adminCommentExpr(conn) + " AS admin_comment, " +
                supportEmailSentExpr(conn) + " AS support_email_sent, " +
                caseStatusExpr(conn) + " AS case_status, " +
                studentNameExpr(conn) + " AS student_name, " + studentEmailExpr(conn) + " AS student_email, " +
                userIdExpr(conn) + " AS user_id FROM mood ORDER BY id DESC";
    }

    private String buildSelectByIdSql(Connection conn) throws SQLException {
        return "SELECT id, " + typeExpr(conn) + " AS mood_type, " + dateExpr(conn) + " AS mood_date, " +
                noteExpr(conn) + " AS note, " + stressLevelExpr(conn) + " AS stress_level, " +
                energyLevelExpr(conn) + " AS energy_level, " + sleepTimeExpr(conn) + " AS sleep_time, " +
                wakeTimeExpr(conn) + " AS wake_time, " + sleepHoursExpr(conn) + " AS sleep_hours, " +
                adminCommentExpr(conn) + " AS admin_comment, " +
                supportEmailSentExpr(conn) + " AS support_email_sent, " +
                caseStatusExpr(conn) + " AS case_status, " +
                studentNameExpr(conn) + " AS student_name, " + studentEmailExpr(conn) + " AS student_email, " +
                userIdExpr(conn) + " AS user_id FROM mood WHERE id = ?";
    }

    private String buildInsertSql(Connection conn) throws SQLException {
        List<String> columns = new ArrayList<>();
        if (hasColumn(conn, "mood", "mood_type")) columns.add("mood_type");
        if (hasColumn(conn, "mood", "humeur")) columns.add("humeur");
        if (hasColumn(conn, "mood", "mood_date")) columns.add("mood_date");
        if (hasColumn(conn, "mood", "date_humeur")) columns.add("date_humeur");
        if (hasColumn(conn, "mood", "datemood")) columns.add("datemood");
        if (hasColumn(conn, "mood", "intensite")) columns.add("intensite");
        if (hasColumn(conn, "mood", "id_user")) columns.add("id_user");
        if (hasColumn(conn, "mood", "user_id")) columns.add("user_id");
        if (hasColumn(conn, "mood", "note")) columns.add("note");
        if (hasColumn(conn, "mood", "commentaire")) columns.add("commentaire");
        if (hasColumn(conn, "mood", "description")) columns.add("description");
        if (hasColumn(conn, "mood", "stress_level")) columns.add("stress_level");
        if (hasColumn(conn, "mood", "energy_level")) columns.add("energy_level");
        if (hasColumn(conn, "mood", "sleep_time")) columns.add("sleep_time");
        if (hasColumn(conn, "mood", "wake_time")) columns.add("wake_time");
        if (hasColumn(conn, "mood", "sleep_hours")) columns.add("sleep_hours");
        if (hasColumn(conn, "mood", "admin_comment")) columns.add("admin_comment");
        if (hasColumn(conn, "mood", "support_email_sent")) columns.add("support_email_sent");
        if (hasColumn(conn, "mood", "case_status")) columns.add("case_status");

        if (columns.isEmpty()) {
            throw new SQLException("Table mood has no supported columns.");
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            placeholders.append("?");
            if (i < columns.size() - 1) placeholders.append(", ");
        }
        return "INSERT INTO mood (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
    }

    private String buildUpdateSql(Connection conn) throws SQLException {
        List<String> sets = new ArrayList<>();
        if (hasColumn(conn, "mood", "mood_type")) sets.add("mood_type = ?");
        if (hasColumn(conn, "mood", "humeur")) sets.add("humeur = ?");
        if (hasColumn(conn, "mood", "mood_date")) sets.add("mood_date = ?");
        if (hasColumn(conn, "mood", "date_humeur")) sets.add("date_humeur = ?");
        if (hasColumn(conn, "mood", "datemood")) sets.add("datemood = ?");
        if (hasColumn(conn, "mood", "intensite")) sets.add("intensite = ?");
        if (hasColumn(conn, "mood", "note")) sets.add("note = ?");
        if (hasColumn(conn, "mood", "commentaire")) sets.add("commentaire = ?");
        if (hasColumn(conn, "mood", "description")) sets.add("description = ?");
        if (hasColumn(conn, "mood", "stress_level")) sets.add("stress_level = ?");
        if (hasColumn(conn, "mood", "energy_level")) sets.add("energy_level = ?");
        if (hasColumn(conn, "mood", "sleep_time")) sets.add("sleep_time = ?");
        if (hasColumn(conn, "mood", "wake_time")) sets.add("wake_time = ?");
        if (hasColumn(conn, "mood", "sleep_hours")) sets.add("sleep_hours = ?");
        if (hasColumn(conn, "mood", "admin_comment")) sets.add("admin_comment = ?");
        if (hasColumn(conn, "mood", "support_email_sent")) sets.add("support_email_sent = ?");
        if (hasColumn(conn, "mood", "case_status")) sets.add("case_status = ?");

        if (sets.isEmpty()) {
            throw new SQLException("Table mood has no supported columns.");
        }
        return "UPDATE mood SET " + String.join(", ", sets) + " WHERE id = ?";
    }

    private String typeExpr(Connection conn) throws SQLException {
        boolean hasMoodType = hasColumn(conn, "mood", "mood_type");
        boolean hasHumeur = hasColumn(conn, "mood", "humeur");
        if (hasHumeur && hasMoodType) {
            return "COALESCE(NULLIF(humeur, ''), mood_type)";
        }
        if (hasMoodType) {
            return "mood_type";
        }
        if (hasHumeur) {
            return "humeur";
        }
        throw new SQLException("No mood type column found (mood_type/humeur).");
    }

    private String dateExpr(Connection conn) throws SQLException {
        boolean hasMoodDate = hasColumn(conn, "mood", "mood_date");
        boolean hasDateHumeur = hasColumn(conn, "mood", "date_humeur");
        boolean hasDateMood = hasColumn(conn, "mood", "datemood");
        if (hasDateHumeur && hasMoodDate && hasDateMood) {
            return "COALESCE(date_humeur, mood_date, datemood)";
        }
        if (hasDateHumeur && hasMoodDate) {
            return "COALESCE(date_humeur, mood_date)";
        }
        if (hasDateMood && hasMoodDate) {
            return "COALESCE(datemood, mood_date)";
        }
        if (hasDateHumeur && hasDateMood) {
            return "COALESCE(date_humeur, datemood)";
        }
        if (hasMoodDate) {
            return "mood_date";
        }
        if (hasDateHumeur) {
            return "date_humeur";
        }
        if (hasDateMood) {
            return "datemood";
        }
        throw new SQLException("No mood date column found (mood_date/date_humeur).");
    }

    private String noteExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "note")) return "note";
        if (hasColumn(conn, "mood", "commentaire")) return "commentaire";
        if (hasColumn(conn, "mood", "description")) return "description";
        return "NULL";
    }

    private String sleepTimeExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "sleep_time")) return "sleep_time";
        return "NULL";
    }

    private String stressLevelExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "stress_level")) return "stress_level";
        return "NULL";
    }

    private String energyLevelExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "energy_level")) return "energy_level";
        return "NULL";
    }

    private String wakeTimeExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "wake_time")) return "wake_time";
        return "NULL";
    }

    private String sleepHoursExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "sleep_hours")) return "sleep_hours";
        return "NULL";
    }

    private String adminCommentExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "admin_comment")) return "admin_comment";
        return "NULL";
    }

    private String supportEmailSentExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "support_email_sent")) return "support_email_sent";
        return "FALSE";
    }

    private String caseStatusExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "case_status")) return "case_status";
        return "CASE WHEN " + supportEmailSentExpr(conn) + " THEN 'Solved' ELSE 'New' END";
    }

    private String userIdExpr(Connection conn) throws SQLException {
        if (hasColumn(conn, "mood", "id_user")) return "id_user";
        if (hasColumn(conn, "mood", "user_id")) return "user_id";
        return "NULL";
    }

    private String studentNameExpr(Connection conn) throws SQLException {
        if (!tableExists(conn, "user")) {
            return "NULL";
        }
        String nameExpr = "NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), '')";
        if (hasColumn(conn, "mood", "id_user")) {
            return "COALESCE((SELECT " + nameExpr + " FROM `user` u WHERE u.id = mood.id_user AND LOWER(u.role) NOT LIKE '%admin%' LIMIT 1), " +
                    fallbackStudentNameExpr(nameExpr) + ")";
        }
        if (hasColumn(conn, "mood", "user_id")) {
            return "COALESCE((SELECT " + nameExpr + " FROM `user` u WHERE u.id = mood.user_id AND LOWER(u.role) NOT LIKE '%admin%' LIMIT 1), " +
                    fallbackStudentNameExpr(nameExpr) + ")";
        }
        return fallbackStudentNameExpr(nameExpr);
    }

    private String studentEmailExpr(Connection conn) throws SQLException {
        if (!tableExists(conn, "user")) {
            return "NULL";
        }
        if (hasColumn(conn, "mood", "id_user")) {
            return "COALESCE((SELECT u.email FROM `user` u WHERE u.id = mood.id_user AND LOWER(u.role) NOT LIKE '%admin%' LIMIT 1), " +
                    fallbackStudentEmailExpr() + ")";
        }
        if (hasColumn(conn, "mood", "user_id")) {
            return "COALESCE((SELECT u.email FROM `user` u WHERE u.id = mood.user_id AND LOWER(u.role) NOT LIKE '%admin%' LIMIT 1), " +
                    fallbackStudentEmailExpr() + ")";
        }
        return fallbackStudentEmailExpr();
    }

    private String fallbackStudentNameExpr(String nameExpr) {
        return "(SELECT " + nameExpr + " FROM `user` u WHERE LOWER(u.role) IN ('etudiant', 'student') ORDER BY u.id ASC LIMIT 1)";
    }

    private String fallbackStudentEmailExpr() {
        return "(SELECT u.email FROM `user` u WHERE LOWER(u.role) IN ('etudiant', 'student') ORDER BY u.id ASC LIMIT 1)";
    }

    private void setSleepHours(PreparedStatement ps, int index, Double sleepHours) throws SQLException {
        if (sleepHours == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, sleepHours);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private Double getSleepHours(ResultSet rs) throws SQLException {
        double value = rs.getDouble("sleep_hours");
        return rs.wasNull() ? null : value;
    }

    private Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private int resolveUserId(Connection conn, Integer requestedUserId) throws SQLException {
        if (requestedUserId != null) {
            return requestedUserId;
        }

        String raw = System.getenv("DEFAULT_USER_ID");
        if (raw != null && !raw.isBlank()) {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String sql = """
                SELECT id
                FROM `user`
                WHERE LOWER(role) IN ('etudiant', 'student')
                ORDER BY id ASC
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        throw new SQLException("Cannot create mood: no logged-in student id and no etudiant user found.");
    }
}
