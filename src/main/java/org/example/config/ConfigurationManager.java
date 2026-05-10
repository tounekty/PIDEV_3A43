package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationManager {
    private static ConfigurationManager instance;
    private final Properties properties = new Properties();

    private ConfigurationManager() {
        try (InputStream input = ConfigurationManager.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("[ConfigurationManager] Could not load application.properties: " + e.getMessage());
        }
    }

    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    public String getMailSmtpHost()    { return getProperty("mail.smtp.host", "smtp.gmail.com"); }
    public int    getMailSmtpPort()    { return getIntProperty("mail.smtp.port", 587); }
    public String getMailUsername()    { return getProperty("mail.username", ""); }
    public String getMailPassword()    { return getProperty("mail.password", ""); }
    public String getMailFromAddress() { return getProperty("mail.from.address", "noreply@mindcare.local"); }
    public String getMailFromName()    { return getProperty("mail.from.name", "MindCare Events"); }
}
