package org.example.service;

import org.example.model.Journal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HuggingFaceJournalAnalysisService {
    private static final String HF_BASE_URL = "https://router.huggingface.co/hf-inference/models/";
    private static final String SENTIMENT_MODEL = "distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    private static final String EMOTION_MODEL = "facebook/bart-large-mnli";
    private static final String SUMMARY_MODEL = "facebook/bart-large-cnn";
    private static final List<String> EMOTION_LABELS = List.of(
            "calm", "joy", "sadness", "anxiety", "anger", "emotional fatigue", "stress", "loss of motivation"
    );
    private static final List<String> NEGATIVE_WORDS = List.of(
            "sad", "stress", "stressed", "anxious", "angry", "tired", "exhausted", "overwhelmed",
            "hopeless", "alone", "panic", "pressure", "drained", "unmotivated", "bad"
    );
    private static final List<String> POSITIVE_WORDS = List.of(
            "happy", "calm", "good", "great", "grateful", "better", "peaceful", "motivated", "proud", "hopeful"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public JournalAiAnalysis analyze(Journal journal) {
        String text = journalText(journal);
        if (text.isBlank()) {
            return fallbackAnalysis(journal, "local fallback");
        }

        String token = resolveToken();
        if (token == null) {
            JournalAiAnalysis fallback = fallbackAnalysis(journal, "local fallback - set HF_TOKEN to enable Hugging Face");
            return new JournalAiAnalysis(
                    fallback.sentiment(),
                    fallback.sentimentScore(),
                    fallback.emotion(),
                    fallback.emotionScore(),
                    fallback.summary(),
                    fallback.interpretation(),
                    fallback.recommendations(),
                    "local fallback - set HF_TOKEN to enable Hugging Face"
            );
        }

        JournalAiAnalysis fallback = fallbackAnalysis(journal, "local fallback");
        List<String> warnings = new ArrayList<>();
        LabelScore sentiment = callOrFallback(
                () -> classifySentiment(text, token),
                new LabelScore(fallback.sentiment(), fallback.sentimentScore()),
                "sentiment",
                warnings
        );
        LabelScore emotion = callOrFallback(
                () -> classifyEmotion(text, token),
                new LabelScore(fallback.emotion(), fallback.emotionScore()),
                "emotion",
                warnings
        );
        String summary = callOrFallback(
                () -> summarize(text, token),
                fallback.summary(),
                "summary",
                warnings
        );

        if (warnings.size() < 3) {
            return new JournalAiAnalysis(
                    normalizeSentiment(sentiment.label()),
                    sentiment.score(),
                    normalizeLabel(emotion.label()),
                    emotion.score(),
                    summary,
                    buildInterpretation(sentiment, emotion),
                    buildRecommendations(sentiment, emotion),
                    buildSource(warnings)
            );
        }

        return new JournalAiAnalysis(
                fallback.sentiment(),
                fallback.sentimentScore(),
                fallback.emotion(),
                fallback.emotionScore(),
                fallback.summary(),
                "Hugging Face could not complete the request, so this result uses local analysis. "
                        + shortWarnings(warnings) + " " + fallback.interpretation(),
                fallback.recommendations(),
                "local fallback after Hugging Face error: " + shortWarnings(warnings)
        );
    }

    private LabelScore classifySentiment(String text, String token) throws IOException, InterruptedException {
        String response = post(SENTIMENT_MODEL, "{\"inputs\":\"" + jsonEscape(limit(text, 1800)) + "\"}", token);
        return parseLabelScores(response).stream()
                .max(Comparator.comparingDouble(LabelScore::score))
                .orElse(new LabelScore("NEUTRAL", 0.0));
    }

    private LabelScore classifyEmotion(String text, String token) throws IOException, InterruptedException {
        String labels = EMOTION_LABELS.stream()
                .map(label -> "\"" + jsonEscape(label) + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("\"calm\"");
        String body = "{\"inputs\":\"" + jsonEscape(limit(text, 1800)) + "\",\"parameters\":{\"candidate_labels\":[" + labels + "]}}";
        String response = post(EMOTION_MODEL, body, token);
        return parseZeroShot(response);
    }

    private String summarize(String text, String token) throws IOException, InterruptedException {
        String clean = text.trim();
        if (clean.length() < 180) {
            return clean;
        }
        String body = "{\"inputs\":\"" + jsonEscape(limit(clean, 2200)) + "\",\"parameters\":{\"truncation\":\"longest_first\"}}";
        String response = post(SUMMARY_MODEL, body, token);
        Matcher matcher = Pattern.compile("\"summary_text\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(response);
        if (matcher.find()) {
            return jsonUnescape(matcher.group(1));
        }
        return firstSentence(clean);
    }

    private String post(String model, String jsonBody, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(HF_BASE_URL + model))
                .timeout(Duration.ofSeconds(35))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Hugging Face HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private <T> T callOrFallback(ThrowingSupplier<T> supplier, T fallback, String feature, List<String> warnings) {
        try {
            return supplier.get();
        } catch (Exception e) {
            warnings.add(feature + " failed: " + compactError(e));
            return fallback;
        }
    }

    private String buildSource(List<String> warnings) {
        if (warnings.isEmpty()) {
            return "Hugging Face Inference API";
        }
        return "Hugging Face Inference API with local fallback for " + shortWarnings(warnings);
    }

    private String shortWarnings(List<String> warnings) {
        return limit(String.join("; ", warnings), 220);
    }

    private String compactError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    private List<LabelScore> parseLabelScores(String json) {
        List<LabelScore> scores = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"label\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"score\"\\s*:\\s*([0-9.Ee+-]+)").matcher(json);
        while (matcher.find()) {
            scores.add(new LabelScore(jsonUnescape(matcher.group(1)), parseDouble(matcher.group(2))));
        }
        return scores;
    }

    private LabelScore parseZeroShot(String json) {
        List<LabelScore> labelScores = parseLabelScores(json);
        if (!labelScores.isEmpty()) {
            return labelScores.stream()
                    .max(Comparator.comparingDouble(LabelScore::score))
                    .orElse(new LabelScore("unknown", 0.0));
        }

        List<String> labels = parseStringArray(json, "labels");
        List<Double> scores = parseNumberArray(json, "scores");
        if (!labels.isEmpty()) {
            double score = scores.isEmpty() ? 0.0 : scores.get(0);
            return new LabelScore(labels.get(0), score);
        }
        return new LabelScore("unknown", 0.0);
    }

    private List<String> parseStringArray(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        Matcher itemMatcher = Pattern.compile("\"((?:\\\\.|[^\"])*)\"").matcher(matcher.group(1));
        while (itemMatcher.find()) {
            values.add(jsonUnescape(itemMatcher.group(1)));
        }
        return values;
    }

    private List<Double> parseNumberArray(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        Matcher itemMatcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(?:[Ee][-+]?[0-9]+)?").matcher(matcher.group(1));
        while (itemMatcher.find()) {
            values.add(parseDouble(itemMatcher.group()));
        }
        return values;
    }

    private JournalAiAnalysis fallbackAnalysis(Journal journal, String source) {
        String text = journalText(journal);
        String content = journal == null || journal.getContent() == null ? "" : journal.getContent().trim();
        String textForSummary = content.isBlank() ? text : content;
        String lower = text.toLowerCase(Locale.ROOT);
        int negative = countMatches(lower, NEGATIVE_WORDS);
        int positive = countMatches(lower, POSITIVE_WORDS);
        boolean mixed = positive > 0 && negative > 0;
        String sentiment = mixed ? "Mixed" : negative > positive ? "Negative" : positive > negative ? "Positive" : "Neutral";
        String emotion = inferEmotion(lower, positive, negative);
        double sentimentScore = mixed ? 0.68 : negative == positive ? 0.50 : Math.min(0.95, 0.62 + Math.abs(negative - positive) * 0.08);
        String summary = buildLocalSummary(textForSummary, sentiment, emotion, positive, negative);
        LabelScore sentimentLabel = new LabelScore(sentiment, sentimentScore);
        LabelScore emotionLabel = new LabelScore(emotion, sentimentScore);
        return new JournalAiAnalysis(
                sentiment,
                sentimentScore,
                emotion,
                sentimentScore,
                summary,
                buildInterpretation(sentimentLabel, emotionLabel),
                buildRecommendations(sentimentLabel, emotionLabel),
                source
        );
    }

    private String buildInterpretation(LabelScore sentiment, LabelScore emotion) {
        String normalizedSentiment = normalizeSentiment(sentiment.label());
        String normalizedEmotion = normalizeLabel(emotion.label());
        if ("Mixed".equals(normalizedSentiment)) {
            return "The entry contains both positive and stressful signals. The main emotional tone appears to be "
                    + normalizedEmotion + ", so this may be a day with useful moments but also pressure that should be watched.";
        }
        if ("Negative".equals(normalizedSentiment)) {
            return "The journal text leans negative and the strongest emotional signal is " + normalizedEmotion
                    + ". This may point to pressure, fatigue, or a repeated thought pattern worth watching.";
        }
        if ("Positive".equals(normalizedSentiment)) {
            return "The journal text leans positive and the strongest emotional signal is " + normalizedEmotion
                    + ". This suggests a more stable or restorative emotional state.";
        }
        return "The journal text looks emotionally mixed, with " + normalizedEmotion
                + " as the strongest signal. More entries will make the pattern clearer.";
    }

    private List<String> buildRecommendations(LabelScore sentiment, LabelScore emotion) {
        String sentimentLabel = normalizeSentiment(sentiment.label());
        String emotionLabel = normalizeLabel(emotion.label()).toLowerCase(Locale.ROOT);
        List<String> items = new ArrayList<>();
        if ("Mixed".equals(sentimentLabel)) {
            items.add("Keep the positive part of the day visible, but write down the exact source of stress so it does not stay vague.");
            items.add("Choose one small recovery action today before the stress builds further.");
            items.add("Compare this entry with your stress and energy levels to see whether this mixed pattern repeats.");
        } else if ("Negative".equals(sentimentLabel) || emotionLabel.contains("stress") || emotionLabel.contains("fatigue")) {
            items.add("Write one concrete cause behind this feeling and one small action you can take today.");
            items.add("Reduce pressure for the next few hours by choosing the most important task only.");
            items.add("If this pattern repeats for several days, consider talking with a trusted person or professional.");
        } else if ("Positive".equals(sentimentLabel)) {
            items.add("Notice what helped this positive state and try to repeat one part of it tomorrow.");
            items.add("Keep logging so the app can learn which routines support your balance.");
        } else {
            items.add("Add one sentence about what triggered the feeling and one sentence about what helped.");
            items.add("Compare this entry with your next mood log to see whether the feeling is temporary or repeating.");
        }
        return items;
    }

    private String inferEmotion(String lower, int positive, int negative) {
        if (lower.contains("stress") || lower.contains("pressure") || lower.contains("overwhelmed")) {
            return "stress";
        }
        if (lower.contains("tired") || lower.contains("exhausted") || lower.contains("drained")) {
            return "emotional fatigue";
        }
        if (lower.contains("anxious") || lower.contains("panic")) {
            return "anxiety";
        }
        if (lower.contains("angry")) {
            return "anger";
        }
        if (lower.contains("sad") || lower.contains("hopeless")) {
            return "sadness";
        }
        if (positive > 0 && negative > 0) {
            return "mixed pressure";
        }
        return positive > negative ? "calm or positive" : negative > positive ? "stress or sadness" : "unclear";
    }

    private String buildLocalSummary(String text, String sentiment, String emotion, int positive, int negative) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() < 12) {
            return "The journal entry is too short for a detailed summary. Add a few more sentences for a better AI reading.";
        }
        String signal = "Mixed".equals(sentiment)
                ? "It contains both positive and stressful language"
                : "It is mainly " + sentiment.toLowerCase(Locale.ROOT);
        return signal + ", with " + emotion + " as the strongest local signal. Positive cues: "
                + positive + ", pressure cues: " + negative + ". Original note: " + limit(trimmed, 220);
    }

    private String journalText(Journal journal) {
        if (journal == null) {
            return "";
        }
        return ((journal.getTitle() == null ? "" : journal.getTitle()) + "\n" +
                (journal.getContent() == null ? "" : journal.getContent())).trim();
    }

    private String resolveToken() {
        String token = System.getenv("HF_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getenv("HUGGINGFACE_API_TOKEN");
        }
        if (token == null || token.isBlank()) {
            token = System.getenv("HUGGINGFACE_HUB_TOKEN");
        }
        return token == null || token.isBlank() ? null : token.trim();
    }

    private int countMatches(String text, List<String> words) {
        int count = 0;
        for (String word : words) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }

    private String normalizeSentiment(String label) {
        String normalized = normalizeLabel(label);
        if (normalized.equalsIgnoreCase("LABEL_1") || normalized.equalsIgnoreCase("positive")) {
            return "Positive";
        }
        if (normalized.equalsIgnoreCase("LABEL_0") || normalized.equalsIgnoreCase("negative")) {
            return "Negative";
        }
        if (normalized.equalsIgnoreCase("mixed")) {
            return "Mixed";
        }
        return normalized.isBlank() ? "Neutral" : normalized;
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return "unknown";
        }
        String normalized = label.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String firstSentence(String text) {
        String trimmed = text.trim();
        int end = Math.max(trimmed.indexOf('.'), Math.max(trimmed.indexOf('!'), trimmed.indexOf('?')));
        if (end > 40) {
            return trimmed.substring(0, Math.min(end + 1, 300));
        }
        return limit(trimmed, 300);
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
        return value == null ? "" : value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private record LabelScore(String label, double score) {
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public record JournalAiAnalysis(
            String sentiment,
            double sentimentScore,
            String emotion,
            double emotionScore,
            String summary,
            String interpretation,
            List<String> recommendations,
            String source
    ) {
    }
}
