package org.example.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects toxic words in text using lexicon-based approach.
 * Normalizes text (lowercase, accents removal, punctuation handling) to minimize false negatives.
 */
public class ToxicityDetectionService {
    private final ToxicityLexiconService lexiconService;

    // Cache compiled patterns for toxic words to avoid regex recompilation
    private final Map<String, Pattern> patternCache = new HashMap<>();

    public ToxicityDetectionService() {
        this.lexiconService = new ToxicityLexiconService();
    }

    /**
     * Check if any text contains toxic words.
     * @param texts Variable number of text strings to check (e.g., title, description).
     * @return true if any toxic content detected, false otherwise.
     */
    public boolean isToxic(String... texts) {
        try {
            Set<String> detected = detectToxicWords(texts);
            return !detected.isEmpty();
        } catch (IOException e) {
            System.err.println("Error loading toxicity lexicon: " + e.getMessage());
            return false; // Fail open: do not block if lexicon fails to load
        }
    }

    /**
     * Detect and return the specific toxic words found in text.
     * @param texts Variable number of text strings to check.
     * @return Set of detected toxic words (normalized form).
     * @throws IOException if lexicon cannot be loaded.
     */
    public Set<String> detectToxicWords(String... texts) throws IOException {
        Set<String> lexicon = lexiconService.loadLexicon();
        Set<String> detected = new HashSet<>();

        if (lexicon == null || lexicon.isEmpty()) {
            return detected;
        }

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }

            String normalized = normalizeForDetection(text);
            
            for (String toxicWord : lexicon) {
                if (foundToxicWord(normalized, toxicWord)) {
                    detected.add(toxicWord);
                }
            }
        }

        return detected;
    }

    /**
     * Normalize text for toxicity detection:
     * - Convert to lowercase
     * - Remove accents (é -> e, ç -> c, etc)
     * - Replace punctuation and extra whitespace with spaces
     */
    private String normalizeForDetection(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Convert to lowercase
        String lower = text.toLowerCase(Locale.ROOT);

        // Remove accents using Unicode normalization
        String withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // Remove combining diacritical marks

        // Replace punctuation and non-alphanumeric with spaces, collapse whitespace
        String cleaned = withoutAccents
                .replaceAll("[^a-z0-9\\s@]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned;
    }

    /**
     * Check if a toxic word is found in the normalized text.
     * Uses word boundary approach to avoid matching substrings of non-toxic words.
     * Example: "sex" should not match "sexual" but should match " sex ".
     */
    private boolean foundToxicWord(String normalizedText, String toxicWord) {
        if (normalizedText == null || toxicWord == null) {
            return false;
        }

        // Build a regex pattern with word boundaries
        // Pattern: \btoxicword\b (whole word only)
        Pattern pattern = patternCache.computeIfAbsent(toxicWord, word -> {
            String escaped = Pattern.quote(word);
            return Pattern.compile("\\b" + escaped + "\\b");
        });

        return pattern.matcher(normalizedText).find();
    }
}
