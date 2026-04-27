package org.example.db;

public final class DatabaseConfig {
    private DatabaseConfig() {
    }

    public static String url() {
        return envOrDefault("DB_URL", "");
    }

    public static String user() {
        return envOrDefault("DB_USER", "root");
    }

    public static String password() {
        return envOrDefault("DB_PASSWORD", "");
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
