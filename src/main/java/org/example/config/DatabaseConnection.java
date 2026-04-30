package org.example.config;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database Connection Manager
 * Provides database connections via HikariCP connection pool
 */
public final class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;
    private static boolean initialized = false;
    private static final Object lock = new Object();

    private DatabaseConnection() {
    }

    /**
     * Initialize HikariCP connection pool (lazy initialization)
     */
    private static void initializePool() {
        synchronized (lock) {
            if (initialized) {
                return;
            }
            initialized = true;
            
            try {
                ConfigurationManager config = ConfigurationManager.getInstance();
                HikariConfig hikariConfig = new HikariConfig();
                
                // Database connection settings
                hikariConfig.setJdbcUrl(config.getDbUrl());
                hikariConfig.setUsername(config.getDbUsername());
                hikariConfig.setPassword(config.getDbPassword());
                
                // Connection pool settings
                hikariConfig.setMaximumPoolSize(config.getDbPoolSize());
                hikariConfig.setMinimumIdle(Math.max(2, config.getDbPoolSize() / 3));
                hikariConfig.setConnectionTimeout(config.getDbConnectionTimeout());
                hikariConfig.setIdleTimeout(600000); // 10 minutes
                hikariConfig.setMaxLifetime(1800000); // 30 minutes
                
                // MySQL specific settings
                hikariConfig.setAutoCommit(true);
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                
                // Performance and reliability
                hikariConfig.setLeakDetectionThreshold(60000);
                hikariConfig.setConnectionTestQuery("SELECT 1");
                
                dataSource = new HikariDataSource(hikariConfig);
                logger.info("HikariCP Connection Pool initialized successfully");
                logger.debug("Database URL: " + config.getDbUrl());
                logger.debug("Max Pool Size: " + config.getDbPoolSize());
                
            } catch (Exception e) {
                logger.error("Failed to initialize database connection pool", e);
                // Don't throw - allow retry attempts later
            }
        }
    }

    /**
     * Get a connection from the pool
     * @return Connection from HikariCP pool
     * @throws SQLException if unable to get connection
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initializePool();
        }
        if (dataSource == null) {
            throw new SQLException("Connection pool not initialized - database is unavailable");
        }
        try {
            Connection conn = dataSource.getConnection();
            logger.debug("Connection obtained from pool. Active connections: " + 
                        dataSource.getHikariPoolMXBean().getActiveConnections());
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to obtain database connection", e);
            throw e;
        }
    }

    /**
     * Get pool status information
     * @return String with pool information
     */
    public static String getPoolStatus() {
        if (dataSource == null) {
            return "Connection pool not initialized";
        }
        var mxBean = dataSource.getHikariPoolMXBean();
        return String.format(
            "Active: %d, Idle: %d, Total: %d, Waiting: %d",
            mxBean.getActiveConnections(),
            mxBean.getIdleConnections(),
            mxBean.getTotalConnections(),
            mxBean.getThreadsAwaitingConnection()
        );
    }

    /**
     * Close the connection pool (call on application shutdown)
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP Connection Pool closed");
        }
    }

    /**
     * Test database connection
     * @return true if connection successful
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            logger.info("Database connection test successful");
            return true;
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }
}
