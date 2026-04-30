package org.example.config;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration Manager
 * Loads application properties from application.properties file
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final Properties properties = new Properties();
    private static ConfigurationManager instance;

    static {
        loadProperties();
    }

    private ConfigurationManager() {
    }

    /**
     * Load properties from application.properties
     */
    private static void loadProperties() {
        try (InputStream input = ConfigurationManager.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.warn("application.properties not found! Using default values.");
                return;
            }
            properties.load(input);
            logger.info("Application properties loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load application.properties", e);
        }
    }

    /**
     * Get singleton instance
     */
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    /**
     * Get property value
     */
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Get property value with default
     */
    public String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key.replace(".", "_").toUpperCase());
        if (envValue != null) {
            return envValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get property as integer
     */
    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = getProperty(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer property: " + key, e);
        }
        return defaultValue;
    }

    /**
     * Get property as boolean
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Get property as double
     */
    public double getDoubleProperty(String key, double defaultValue) {
        try {
            String value = getProperty(key);
            if (value != null) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid double property: " + key, e);
        }
        return defaultValue;
    }

    // ============================================================================
    // Database Configuration Getters
    // ============================================================================

    public String getDbUrl() {
        return getProperty("db.url", "jdbc:mysql://localhost:3306/mindcare");
    }

    public String getDbUsername() {
        return getProperty("db.username", "root");
    }

    public String getDbPassword() {
        return getProperty("db.password", "");
    }

    public int getDbPoolSize() {
        return getIntProperty("db.connection.pool.size", 10);
    }

    public long getDbConnectionTimeout() {
        return getIntProperty("db.connection.timeout", 30000);
    }

    // ============================================================================
    // Email Configuration Getters
    // ============================================================================

    public String getMailSmtpHost() {
        return getProperty("mail.smtp.host", "smtp.gmail.com");
    }

    public int getMailSmtpPort() {
        return getIntProperty("mail.smtp.port", 587);
    }

    public String getMailUsername() {
        return getProperty("mail.username", "");
    }

    public String getMailPassword() {
        return getProperty("mail.password", "");
    }

    public String getMailFromAddress() {
        return getProperty("mail.from.address", "noreply@mindcare.local");
    }

    public String getMailFromName() {
        return getProperty("mail.from.name", "MindCare Events");
    }

    // ============================================================================
    // API Configuration Getters
    // ============================================================================

    public String getApiHost() {
        return getProperty("api.server.host", "localhost");
    }

    public int getApiPort() {
        return getIntProperty("api.server.port", 8080);
    }

    public boolean isApiCorsEnabled() {
        return getBooleanProperty("api.enable.cors", true);
    }

    // ============================================================================
    // Event Configuration Getters
    // ============================================================================

    public int getEventDefaultDurationMinutes() {
        return getIntProperty("event.default.duration.minutes", 60);
    }

    public double getEventDefaultOverbookingPercent() {
        return getDoubleProperty("event.default.overbooking.percent", 10.0);
    }

    public int getEventMaxCapacity() {
        return getIntProperty("event.max.capacity", 5000);
    }

    // ============================================================================
    // Logging Configuration Getters
    // ============================================================================

    public String getLoggingLevel() {
        return getProperty("logging.level", "INFO");
    }

    public String getLogFilePath() {
        return getProperty("logging.file.path", "./logs/");
    }

    // ============================================================================
    // Security Configuration Getters
    // ============================================================================

    public String getSecurityPasswordAlgorithm() {
        return getProperty("security.password.algorithm", "BCRYPT");
    }

    public int getSecurityPasswordStrength() {
        return getIntProperty("security.password.strength", 12);
    }

    public int getSecurityLoginMaxAttempts() {
        return getIntProperty("security.login.max.attempts", 5);
    }

    public int getSecuritySessionTimeoutMinutes() {
        return getIntProperty("security.session.timeout.minutes", 30);
    }

    // ============================================================================
    // Application Configuration Getters
    // ============================================================================

    public String getAppName() {
        return getProperty("app.name", "MindCare Events");
    }

    public String getAppVersion() {
        return getProperty("app.version", "1.0.0");
    }

    public void printConfiguration() {
        logger.info("=== Application Configuration ===");
        logger.info("App Name: " + getAppName());
        logger.info("App Version: " + getAppVersion());
        logger.info("Database URL: " + getDbUrl());
        logger.info("API Host: " + getApiHost() + ":" + getApiPort());
        logger.info("Logging Level: " + getLoggingLevel());
        logger.info("====== End Configuration ======");
    }
}
