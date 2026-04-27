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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZenQuotesService {
    private static final String ZEN_QUOTES_RANDOM_URL = "https://zenquotes.io/api/random";
    private static final String ZEN_QUOTES_BATCH_URL = "https://zenquotes.io/api/quotes";
    private static final Quote FALLBACK_QUOTE = new Quote(
            "Small steps every day become the quiet architecture of change.",
            "MindCare",
            "local fallback"
    );
    private static final List<ThemeQuote> THEMED_FALLBACKS = List.of(
            new ThemeQuote(List.of("stress", "exam", "pressure", "overwhelmed", "anxiety", "validation"),
                    "You do not have to solve the whole mountain today. Choose the next steady step.",
                    "MindCare"),
            new ThemeQuote(List.of("sad", "sadness", "alone", "hopeless", "tired", "exhausted"),
                    "A difficult day is still a day you survived, and that matters.",
                    "MindCare"),
            new ThemeQuote(List.of("happy", "joy", "grateful", "proud", "good", "peaceful"),
                    "Notice what helped today, then protect a small piece of it for tomorrow.",
                    "MindCare"),
            new ThemeQuote(List.of("motivation", "unmotivated", "drained", "lost", "empty"),
                    "Motivation often returns after the first small action, not before it.",
                    "MindCare"),
            new ThemeQuote(List.of("anger", "angry", "frustrated", "rage"),
                    "Pause before the reaction becomes the story. Your next choice still belongs to you.",
                    "MindCare")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Quote getRandomQuote() {
        String endpoint = buildEndpoint(ZEN_QUOTES_RANDOM_URL);
        try {
            HttpResponse<String> response = get(endpoint);
            ensureSuccess(response);
            Quote parsed = parseQuote(response.body());
            if (parsed.quote().isBlank()) {
                return fallback("local fallback after empty ZenQuotes response");
            }
            return new Quote(parsed.quote(), parsed.author(), "ZenQuotes API");
        } catch (Exception e) {
            return fallback("local fallback after ZenQuotes error: " + compactError(e));
        }
    }

    public Quote getQuoteForJournal(Journal journal) {
        String journalText = journalText(journal);
        if (journalText.isBlank()) {
            return fallback("local fallback - no journal text selected");
        }

        List<String> themeTerms = detectThemeTerms(journalText);
        try {
            HttpResponse<String> response = get(buildEndpoint(ZEN_QUOTES_BATCH_URL));
            ensureSuccess(response);
            List<Quote> quotes = parseQuotes(response.body());
            Quote match = bestMatch(quotes, themeTerms);
            if (match != null) {
                return new Quote(
                        match.quote(),
                        match.author(),
                        "ZenQuotes API matched to journal theme: " + String.join(", ", themeTerms)
                );
            }
            return themedFallback(themeTerms, "local themed fallback - ZenQuotes batch had no close match");
        } catch (Exception e) {
            return themedFallback(themeTerms, "local themed fallback after ZenQuotes error: " + compactError(e));
        }
    }

    private String buildEndpoint(String baseUrl) {
        String key = resolveApiKey();
        if (key == null) {
            return baseUrl;
        }
        return baseUrl + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> get(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void ensureSuccess(HttpResponse<String> response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("ZenQuotes HTTP " + response.statusCode());
        }
    }

    private String resolveApiKey() {
        String key = System.getenv("ZENQUOTES_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("ZEN_QUOTES_API_KEY");
        }
        return key == null || key.isBlank() ? null : key.trim();
    }

    private Quote parseQuote(String json) throws IOException {
        String quote = findJsonValue(json, "q");
        String author = findJsonValue(json, "a");
        String source = findJsonValue(json, "source");
        if (quote.isBlank() && !source.isBlank()) {
            // ZenQuotes sometimes returns rate-limit messages in a "message" object.
            throw new IOException(source);
        }
        return new Quote(quote, author.isBlank() ? "Unknown" : author, "ZenQuotes API");
    }

    private List<Quote> parseQuotes(String json) throws IOException {
        List<Quote> quotes = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{.*?}", Pattern.DOTALL).matcher(json == null ? "" : json);
        while (matcher.find()) {
            Quote quote = parseQuote(matcher.group());
            if (!quote.quote().isBlank()) {
                quotes.add(quote);
            }
        }
        return quotes;
    }

    private Quote bestMatch(List<Quote> quotes, List<String> themeTerms) {
        return quotes.stream()
                .map(quote -> new ScoredQuote(quote, scoreQuote(quote, themeTerms)))
                .filter(scored -> scored.score() > 0)
                .max(Comparator.comparingInt(ScoredQuote::score))
                .map(ScoredQuote::quote)
                .orElse(null);
    }

    private int scoreQuote(Quote quote, List<String> themeTerms) {
        String text = (quote.quote() + " " + quote.author()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : themeTerms) {
            if (text.contains(term)) {
                score += 4;
            }
        }
        if (themeTerms.contains("stress") && containsAny(text, List.of("difficult", "courage", "strength", "calm", "peace", "overcome"))) {
            score += 2;
        }
        if (themeTerms.contains("sad") && containsAny(text, List.of("hope", "light", "heart", "life", "day"))) {
            score += 2;
        }
        if (themeTerms.contains("motivation") && containsAny(text, List.of("work", "action", "begin", "progress", "success"))) {
            score += 2;
        }
        return score;
    }

    private List<String> detectThemeTerms(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (ThemeQuote theme : THEMED_FALLBACKS) {
            if (containsAny(lower, theme.terms())) {
                return theme.terms();
            }
        }
        return List.of("inspiration", "courage", "life", "change", "growth");
    }

    private boolean containsAny(String text, List<String> terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private Quote themedFallback(List<String> themeTerms, String source) {
        return THEMED_FALLBACKS.stream()
                .filter(theme -> theme.terms().equals(themeTerms) || theme.terms().stream().anyMatch(themeTerms::contains))
                .findFirst()
                .map(theme -> new Quote(theme.quote(), theme.author(), source))
                .orElseGet(() -> fallback(source));
    }

    private String journalText(Journal journal) {
        if (journal == null) {
            return "";
        }
        return ((journal.getTitle() == null ? "" : journal.getTitle()) + " "
                + (journal.getContent() == null ? "" : journal.getContent())).trim();
    }

    private String findJsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                .matcher(json == null ? "" : json);
        return matcher.find() ? jsonUnescape(matcher.group(1)).trim() : "";
    }

    private Quote fallback(String source) {
        return new Quote(FALLBACK_QUOTE.quote(), FALLBACK_QUOTE.author(), source);
    }

    private String compactError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    private String jsonUnescape(String value) {
        return value == null ? "" : value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    public record Quote(String quote, String author, String source) {
    }

    private record ThemeQuote(List<String> terms, String quote, String author) {
    }

    private record ScoredQuote(Quote quote, int score) {
    }
}
