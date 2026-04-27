package org.example.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WiktionaryEmotionService {
    private static final Map<String, String> EMOTION_WORDS = new LinkedHashMap<>();
    private static final Map<String, String> LOCAL_ADVICE = new LinkedHashMap<>();

    static {
        add("overwhelmed", "Try naming the one pressure that feels largest, then choose one small next action.");
        add("stress", "Reduce the next step: pick one task, one pause, and one person you can ask for help.");
        add("stressed", "Reduce the next step: pick one task, one pause, and one person you can ask for help.");
        add("anxious", "Slow breathing and concrete planning can help when the feeling is vague or future-focused.");
        add("sad", "Acknowledge the feeling, then add one gentle action: rest, message someone, or write one more sentence.");
        add("angry", "Pause before responding. Try writing the trigger without judging yourself.");
        add("tired", "Check sleep, food, and workload before treating this as a personal failure.");
        add("exhausted", "This can be a signal to lower load and ask for support.");
        add("hopeless", "This is important to share with a trusted person or professional if it continues.");
        add("panic", "Focus on immediate grounding: breathe, name five things you see, and seek support.");
        add("pressure", "Separate urgent from important; pressure becomes easier when it has shape.");
        add("triste", "Accueille le sentiment, puis choisis une petite action douce.");
        add("stressé", "Respire lentement et reduis la prochaine etape.");
        add("anxieux", "Essaie de transformer l'inquietude en une action concrete.");
        add("fatigué", "Verifie sommeil, repas et charge avant de te juger.");
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<EmotionWordDefinition> explainJournal(Journal journal) {
        String text = ((journal == null ? "" : value(journal.getTitle())) + " " + (journal == null ? "" : value(journal.getContent())))
                .toLowerCase(Locale.ROOT);
        List<EmotionWordDefinition> definitions = new ArrayList<>();
        for (String word : EMOTION_WORDS.keySet()) {
            if (text.contains(word.toLowerCase(Locale.ROOT))) {
                definitions.add(lookup(word));
            }
            if (definitions.size() >= 6) {
                break;
            }
        }
        if (definitions.isEmpty()) {
            definitions.add(new EmotionWordDefinition(
                    "No emotional keyword found",
                    "Add words like stressed, anxious, sad, tired, overwhelmed, triste, or stresse to get dictionary explanations.",
                    "Keep journaling with concrete feeling words; they make support recommendations more precise.",
                    "local"
            ));
        }
        return definitions;
    }

    private EmotionWordDefinition lookup(String word) {
        String language = word.matches(".*[éèêàùç].*|triste|anxieux") ? "fr" : "en";
        try {
            String endpoint = "https://" + language + ".wiktionary.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&format=json&titles="
                    + URLEncoder.encode(word, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String extract = findJsonValue(response.body(), "extract");
                if (!extract.isBlank()) {
                    return new EmotionWordDefinition(word, cleanExtract(extract), LOCAL_ADVICE.get(word), "Wiktionary API");
                }
            }
            return localDefinition(word, "local fallback after Wiktionary HTTP " + response.statusCode());
        } catch (Exception e) {
            return localDefinition(word, "local fallback after Wiktionary error");
        }
    }

    private EmotionWordDefinition localDefinition(String word, String source) {
        return new EmotionWordDefinition(
                word,
                "Dictionary definition unavailable right now. The word was detected as emotionally relevant in this journal.",
                LOCAL_ADVICE.get(word),
                source
        );
    }

    private static void add(String word, String advice) {
        EMOTION_WORDS.put(word, word);
        LOCAL_ADVICE.put(word, advice);
    }

    private String findJsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                .matcher(json == null ? "" : json);
        return matcher.find() ? jsonUnescape(matcher.group(1)).trim() : "";
    }

    private String cleanExtract(String extract) {
        String cleaned = extract.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 420 ? cleaned : cleaned.substring(0, 420) + "...";
    }

    private String jsonUnescape(String value) {
        return value == null ? "" : value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record EmotionWordDefinition(String word, String definition, String advice, String source) {
    }
}
