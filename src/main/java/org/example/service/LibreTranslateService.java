package org.example.service;

import org.example.config.AppConfig;
import org.example.model.Journal;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibreTranslateService {
    private static final String DEFAULT_URL = "https://libretranslate.com/translate";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<TranslationResult> translateJournal(Journal journal) {
        String text = journal == null ? "" : value(journal.getContent());
        if (text.isBlank()) {
            return List.of(new TranslationResult("none", "", "No journal content selected.", "local"));
        }
        String source = detectLanguage(text);
        List<TranslationResult> results = new ArrayList<>();
        results.add(translate(text, source, "en", "English"));
        results.add(translate(text, source, "fr", "French"));
        results.add(translate(text, source, "ar", "Arabic"));
        return results;
    }

    private TranslationResult translate(String text, String source, String target, String label) {
        if (source.equals(target)) {
            return new TranslationResult(label, target, text, "local - already " + label);
        }

        try {
            String body = "{"
                    + "\"q\":\"" + jsonEscape(limit(text, 4500)) + "\","
                    + "\"source\":\"" + source + "\","
                    + "\"target\":\"" + target + "\","
                    + "\"format\":\"text\""
                    + apiKeyJson()
                    + "}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String translated = findJsonValue(response.body(), "translatedText");
                if (!translated.isBlank()) {
                    return new TranslationResult(label, target, translated, "LibreTranslate API");
                }
            }
            return translateWithMyMemory(text, source, target, label,
                    "LibreTranslate HTTP " + response.statusCode());
        } catch (Exception e) {
            return translateWithMyMemory(text, source, target, label, compact(e));
        }
    }

    private TranslationResult translateWithMyMemory(String text, String source, String target, String label, String libreError) {
        try {
            String endpoint = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(limit(text, 500), StandardCharsets.UTF_8)
                    + "&langpair=" + source + "%7C" + target;
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String translated = findJsonValue(response.body(), "translatedText");
                if (!translated.isBlank()) {
                    return new TranslationResult(label, target, translated,
                            "MyMemory API fallback after LibreTranslate error: " + libreError);
                }
            }
            return new TranslationResult(label, target,
                    "Translation unavailable. LibreTranslate: " + libreError + ". MyMemory HTTP " + response.statusCode() + ".",
                    "translation APIs unavailable");
        } catch (Exception e) {
            return new TranslationResult(label, target,
                    "Translation unavailable. LibreTranslate: " + libreError + ". MyMemory: " + compact(e) + ".",
                    "translation APIs unavailable");
        }
    }

    private String endpoint() {
        String configured = AppConfig.first("LIBRETRANSLATE_URL", "LIBRE_TRANSLATE_URL");
        return configured == null ? DEFAULT_URL : configured;
    }

    private String apiKeyJson() {
        String key = AppConfig.first("LIBRETRANSLATE_API_KEY", "LIBRE_TRANSLATE_API_KEY");
        return key == null ? "" : ",\"api_key\":\"" + jsonEscape(key) + "\"";
    }

    private String detectLanguage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (Pattern.compile("[\\u0600-\\u06FF]").matcher(text).find()) {
            return "ar";
        }
        if (lower.matches(".*\\b(je|tu|nous|vous|avec|pour|dans|triste|fatigue|stresse|stressé|heureux|bonjour)\\b.*")) {
            return "fr";
        }
        return "en";
    }

    private String findJsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                .matcher(json == null ? "" : json);
        return matcher.find() ? jsonUnescape(matcher.group(1)).trim() : "";
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String jsonUnescape(String value) {
        if (value == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(value);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(String.valueOf((char) codePoint)));
        }
        matcher.appendTail(decoded);
        return decoded.toString()
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String compact(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message.replaceAll("\\s+", " ");
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record TranslationResult(String language, String languageCode, String text, String source) {
    }
}
