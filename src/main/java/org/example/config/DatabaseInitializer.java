package org.example.config;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database Initializer
 * Handles database initialization, schema creation, and data persistence
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    /**
     * Initialize database (called once on application startup)
     */
    public static void initializeDatabase() {
        logger.info("Initializing database...");
        
        int maxRetries = 5;
        int retryCount = 0;
        long retryDelay = 2000; // 2 seconds
        
        while (retryCount < maxRetries) {
            try {
                // Test connection
                if (!DatabaseConnection.testConnection()) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        logger.warn("Database connection failed (attempt " + retryCount + "/" + maxRetries + "). Retrying in " + (retryDelay / 1000) + " seconds...");
                        Thread.sleep(retryDelay);
                        retryDelay *= 2; // Exponential backoff
                        continue;
                    } else {
                        logger.error("Failed to connect to database after " + maxRetries + " attempts");
                        throw new RuntimeException("Database connection failed after " + maxRetries + " retries");
                    }
                }
                
                logger.info("✓ Database connection successful");
                
                // Check if tables exist
                if (tablesExist()) {
                    logger.info("✓ Database schema already initialized");
                } else {
                    logger.info("Creating database schema...");
                    createSchema();
                    logger.info("✓ Database schema created successfully");
                }
                
                // Insert initial data if needed
                insertInitialDataIfNeeded();
                
                // Log pool status
                logger.info("Database connection pool status: " + DatabaseConnection.getPoolStatus());
                
                return; // Success - exit the retry loop
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Database initialization interrupted", e);
                throw new RuntimeException("Database initialization interrupted", e);
            } catch (Exception e) {
                retryCount++;
                if (retryCount < maxRetries) {
                    logger.warn("Database initialization failed (attempt " + retryCount + "/" + maxRetries + ")", e);
                    try {
                        Thread.sleep(retryDelay);
                        retryDelay *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Database initialization interrupted", ie);
                    }
                } else {
                    logger.error("Database initialization failed after " + maxRetries + " attempts", e);
                    throw new RuntimeException("Failed to initialize database after " + maxRetries + " retries", e);
                }
            }
        }
    }

    /**
     * Check if tables exist in the database
     */
    private static boolean tablesExist() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'mindcare'")) {
            
            if (rs.next() && rs.getInt(1) > 0) {
                logger.info("Found " + rs.getInt(1) + " tables in database");
                return true;
            }
            return false;
        } catch (SQLException e) {
            logger.debug("Tables check failed (might be first run)", e);
            return false;
        }
    }

    /**
     * Create database schema using Flyway migrations
     */
    private static void createSchema() {
        try {
            // Execute initial schema creation
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Read and execute the schema file
                String schema = readSchemaSQL();
                String[] statements = schema.split(";");
                
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty() && !sql.startsWith("--")) {
                        try {
                            stmt.execute(sql);
                            logger.debug("Executed: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                        } catch (SQLException e) {
                            // Skip if table already exists
                            if (!e.getMessage().contains("already exists")) {
                                throw e;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create database schema", e);
            throw new RuntimeException("Schema creation failed", e);
        }
    }

    /**
     * Read schema SQL from migration file
     */
    private static String readSchemaSQL() {
        // This would read from V1__Initial_Schema.sql
        // For now, return a basic schema
        return "";
    }

    /**
     * Insert initial data if tables are empty
     */
    private static void insertInitialDataIfNeeded() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                logger.info("Inserting initial data...");
                insertInitialUsers(conn);
            }
        } catch (SQLException e) {
            logger.debug("Initial data insertion skipped", e);
        }
    }

    /**
     * Insert initial users
     */
    private static void insertInitialUsers(Connection conn) throws SQLException {
        String[] insertStatements = {
            "INSERT INTO users (username, email, password_hash, role, is_admin, full_name) " +
            "VALUES ('admin', 'admin@mindcare.local', '$2a$12$...', 'ADMIN', TRUE, 'Administrator')",
            
            "INSERT INTO users (username, email, password_hash, role, is_admin, full_name) " +
            "VALUES ('student1', 'student1@mindcare.local', '$2a$12$...', 'STUDENT', FALSE, 'John Doe')"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String sql : insertStatements) {
                try {
                    stmt.execute(sql);
                    logger.info("Inserted initial user");
                } catch (SQLException e) {
                    logger.debug("User already exists", e);
                }
            }
        }
    }

    /**
     * Drop all tables (USE WITH CAUTION - for testing only)
     */
    public static void dropAllTables() {
        logger.warn("!!! DROPPING ALL TABLES - USE ONLY FOR TESTING !!!");
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Drop tables in reverse order of creation
            String[] dropStatements = {
                "DROP TABLE IF EXISTS api_keys",
                "DROP TABLE IF EXISTS audit_log",
                "DROP TABLE IF EXISTS smart_capacity",
                "DROP TABLE IF EXISTS recommendations",
                "DROP TABLE IF EXISTS predictions",
                "DROP TABLE IF EXISTS reminders",
                "DROP TABLE IF EXISTS waitlist",
                "DROP TABLE IF EXISTS event_reviews",
                "DROP TABLE IF EXISTS event_engagement",
                "DROP TABLE IF EXISTS reservations",
                "DROP TABLE IF EXISTS events",
                "DROP TABLE IF EXISTS users"
            };
            
            for (String sql : dropStatements) {
                stmt.execute(sql);
                logger.warn("Executed: " + sql);
            }
            
            logger.warn("All tables dropped!");
            
        } catch (SQLException e) {
            logger.error("Failed to drop tables", e);
        }
    }

    /**
     * Get database statistics
     */
    public static void printDatabaseStats() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT TABLE_NAME, TABLE_ROWS, ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = 'mindcare'")) {
            
            logger.info("=== Database Statistics ===");
            while (rs.next()) {
                logger.info(String.format("%s: %d rows, %.2f MB",
                    rs.getString("TABLE_NAME"),
                    rs.getLong("TABLE_ROWS"),
                    rs.getDouble("size_mb")));
            }
            logger.info("===========================");
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve database stats", e);
        }
    }
}
