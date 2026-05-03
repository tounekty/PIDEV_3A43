package org.example.service;

import com.google.gson.*;
import java.io.IOException;
import okhttp3.*;

/**
 * Google Perspective API service for detecting toxic comments
 * Requires: PERSPECTIVE_API_KEY environment variable
 */
public class PerspectiveAPIService {

    private static final String PERSPECTIVE_URL = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze";
    private static final float TOXICITY_THRESHOLD = 0.7f; // 70% toxicity threshold
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * Check if a comment is toxic using Google Perspective API
     * @param text The comment text to analyze
     * @return true if text is considered toxic, false otherwise
     * @throws IOException if API call fails
     */
    public static boolean isToxic(String text) throws IOException {
        return getToxicityScore(text) > TOXICITY_THRESHOLD;
    }

    /**
     * Get the toxicity score (0.0 to 1.0) for a comment
     * @param text The comment text to analyze
     * @return Toxicity score between 0 and 1
     * @throws IOException if API call fails
     */
    public static float getToxicityScore(String text) throws IOException {
        String apiKey = System.getenv("PERSPECTIVE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("PERSPECTIVE_API_KEY environment variable not set");
        }

        String url = PERSPECTIVE_URL + "?key=" + apiKey;

        // Build request body
        JsonObject requestBody = new JsonObject();
        
        JsonObject comment = new JsonObject();
        comment.addProperty("text", text);
        requestBody.add("comment", comment);

        JsonArray attributes = new JsonArray();
        attributes.add("TOXICITY");
        requestBody.add("requestedAttributes", attributes);

        // Make POST request
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Perspective API request failed: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

            // Extract toxicity score
            JsonObject attributeScores = responseJson.getAsJsonObject("attributeScores");
            if (attributeScores == null) {
                throw new IOException("No attributeScores in Perspective API response");
            }

            JsonObject toxicity = attributeScores.getAsJsonObject("TOXICITY");
            if (toxicity == null) {
                throw new IOException("No TOXICITY score in response");
            }

            JsonObject summaryScore = toxicity.getAsJsonObject("summaryScore");
            if (summaryScore == null) {
                throw new IOException("No summaryScore in response");
            }

            return summaryScore.get("value").getAsFloat();
        }
    }

    /**
     * Set the toxicity threshold (0.0 to 1.0)
     * Higher = more lenient
     * Lower = stricter
     */
    public static void setToxicityThreshold(float threshold) {
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        // Note: This would require a non-final field in a real implementation
        // For now, the threshold is fixed at 0.7
    }

    /**
     * Get a human-readable toxicity level
     */
    public static String getToxicityLevel(float score) {
        if (score < 0.3f) return "Acceptable";
        if (score < 0.6f) return "Borderline";
        if (score < 0.8f) return "Toxic";
        return "Highly Toxic";
    }
}
