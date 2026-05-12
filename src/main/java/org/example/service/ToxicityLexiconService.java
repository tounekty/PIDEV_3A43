package org.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads and caches the toxicity lexicon from a file resource.
 * Supports lines with comments (#), empty lines are ignored.
 */
public class ToxicityLexiconService {
    private static final String DEFAULT_LEXICON_FILE = "/toxic-words.txt";
    private volatile Set<String> cachedLexicon = null;

    /**
     * Load toxic words from resource file (cached after first call).
     * @return Set of lowercase toxic words/patterns.
     * @throws IOException if the resource file cannot be read.
     */
    public Set<String> loadLexicon() throws IOException {
        if (cachedLexicon != null) {
            return cachedLexicon;
        }

        Set<String> lexicon = new HashSet<>();
        InputStream resourceStream = ToxicityLexiconService.class.getResourceAsStream(DEFAULT_LEXICON_FILE);

        if (resourceStream == null) {
            throw new IOException("Toxicity lexicon file not found: " + DEFAULT_LEXICON_FILE);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Normalize word: lowercase, remove leading/trailing whitespace
                String normalized = line.toLowerCase().trim();
                if (!normalized.isEmpty()) {
                    lexicon.add(normalized);
                }
            }
        }

        this.cachedLexicon = lexicon;
        System.out.println("✓ Toxicity lexicon loaded: " + lexicon.size() + " terms");
        return lexicon;
    }

    /**
     * Clear the cache (useful for testing).
     */
    public void clearCache() {
        cachedLexicon = null;
    }

    /**
     * Get currently cached lexicon size (0 if not loaded).
     */
    public int getCachedSize() {
        return cachedLexicon == null ? 0 : cachedLexicon.size();
    }
}
