package org.example.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ConnectionFactory {
    private static volatile String resolvedUrl;

    private ConnectionFactory() {
    }

    public static Connection getConnection() throws SQLException {
        String configuredUrl = DatabaseConfig.url();
        if (!configuredUrl.isBlank()) {
            return DriverManager.getConnection(
                    configuredUrl,
                    DatabaseConfig.user(),
                    DatabaseConfig.password()
            );
        }

        String url = resolveUrl();
        return DriverManager.getConnection(
                url,
                DatabaseConfig.user(),
                DatabaseConfig.password()
        );
    }

    private static String resolveUrl() throws SQLException {
        if (resolvedUrl != null) {
            return resolvedUrl;
        }
        synchronized (ConnectionFactory.class) {
            if (resolvedUrl != null) {
                return resolvedUrl;
            }

            String[] candidates = new String[]{
                    "jdbc:mysql://localhost:3306/moodtracker_db",
                    "jdbc:mysql://localhost:3308/moodtracker_db"
            };

            String bestUrl = null;
            int bestScore = Integer.MIN_VALUE;
            SQLException lastError = null;

            for (String candidate : candidates) {
                try (Connection conn = DriverManager.getConnection(candidate, DatabaseConfig.user(), DatabaseConfig.password())) {
                    int score = scoreConnection(conn);
                    if (score > bestScore) {
                        bestScore = score;
                        bestUrl = candidate;
                    }
                } catch (SQLException e) {
                    lastError = e;
                }
            }

            if (bestUrl == null) {
                if (lastError != null) {
                    throw lastError;
                }
                throw new SQLException("No reachable MySQL URL found for moodtracker_db.");
            }

            resolvedUrl = bestUrl;
            System.out.println("Using DB URL: " + resolvedUrl + " (set DB_URL to override)");
            return resolvedUrl;
        }
    }

    private static int scoreConnection(Connection conn) throws SQLException {
        int score = 1;
        if (tableExists(conn, "mood")) {
            score += 10 + Math.min(10, rowCount(conn, "mood"));
        }
        if (tableExists(conn, "journal")) {
            score += 10 + Math.min(10, rowCount(conn, "journal"));
        }
        return score;
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static int rowCount(Connection conn, String tableName) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ignored) {
            return 0;
        }
    }
}
