package com.mindcare.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AppConfig {

    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private AppConfig() {
    }

    public static String get(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = clean(System.getProperty(propertyKey));
        if (!propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = clean(System.getenv(envKey));
        if (!environmentValue.isBlank()) {
            return environmentValue;
        }

        String dotEnvValue = clean(DOT_ENV.get(envKey));
        if (!dotEnvValue.isBlank()) {
            return dotEnvValue;
        }

        String dotEnvPropertyStyle = clean(DOT_ENV.get(propertyKey));
        if (!dotEnvPropertyStyle.isBlank()) {
            return dotEnvPropertyStyle;
        }

        return defaultValue;
    }

    private static Map<String, String> loadDotEnv() {
        Path dotEnvPath = Paths.get(".env");
        if (!Files.exists(dotEnvPath)) {
            return Collections.emptyMap();
        }

        try {
            List<String> lines = Files.readAllLines(dotEnvPath, StandardCharsets.UTF_8);
            Map<String, String> values = new HashMap<>();
            for (String rawLine : lines) {
                if (rawLine == null) {
                    continue;
                }

                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, equalsIndex).trim();
                if (key.isEmpty()) {
                    continue;
                }

                String value = line.substring(equalsIndex + 1).trim();
                values.put(key, clean(value));
            }
            return Collections.unmodifiableMap(values);
        } catch (IOException exception) {
            System.err.println("[AppConfig] Unable to read .env file: " + exception.getMessage());
            return Collections.emptyMap();
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            boolean quotedWithDouble = trimmed.startsWith("\"") && trimmed.endsWith("\"");
            boolean quotedWithSingle = trimmed.startsWith("'") && trimmed.endsWith("'");
            if (quotedWithDouble || quotedWithSingle) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }

        return trimmed;
    }
}