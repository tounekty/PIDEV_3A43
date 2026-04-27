package org.example.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AppConfig {
    private static final Map<String, String> DOTENV = loadDotEnv();

    private AppConfig() {
    }

    public static String first(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                value = DOTENV.get(name);
            }
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static Map<String, String> loadDotEnv() {
        Path path = Path.of(".env");
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equals).trim();
                String value = unquote(trimmed.substring(equals + 1).trim());
                if (!key.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.copyOf(values);
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
