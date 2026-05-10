package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.model.ForumRewriteSuggestion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ForumAiRewriteService {
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.2";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public boolean isConfigured() {
        String baseUrl = configuredBaseUrl();
        return baseUrl != null && !baseUrl.isBlank();
    }

    public ForumRewriteSuggestion rewriteSubject(String title, String description) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("Ollama n'est pas configure.");
        }

        String prompt = """
                Reformule le sujet de forum ci-dessous en francais professionnel, clair et bienveillant.
                Garde le sens original, ne rajoute pas de faits inventes, et conserve les mentions @username si elles existent.
                Retourne uniquement un JSON valide avec deux champs: title et description.

                Titre original:
                %s

                Description originale:
                %s
                """.formatted(nullToEmpty(title), nullToEmpty(description));

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode options = objectMapper.createObjectNode();
        payload.put("model", configuredModel());
        payload.put("prompt", prompt);
        payload.put("stream", false);
        payload.put("format", "json");
        options.put("temperature", 0.4);
        payload.set("options", options);

        HttpRequest request = HttpRequest.newBuilder(URI.create(configuredBaseUrl() + "/api/generate"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(readOllamaError(response.statusCode(), response.body()));
        }

        String outputText = extractOutputText(response.body());
        JsonNode suggestion = objectMapper.readTree(extractJsonObject(cleanJsonText(outputText)));
        String rewrittenTitle = suggestion.path("title").asText("").trim();
        String rewrittenDescription = suggestion.path("description").asText("").trim();
        if (rewrittenTitle.isBlank() || rewrittenDescription.isBlank()) {
            throw new IOException("La reponse IA ne contient pas title et description.");
        }
        return new ForumRewriteSuggestion(rewrittenTitle, rewrittenDescription);
    }

    private String configuredBaseUrl() {
        String baseUrl = System.getenv("OLLAMA_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_OLLAMA_BASE_URL;
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private String configuredModel() {
        String model = System.getenv("OLLAMA_MODEL");
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    public String validateModelAvailability() {
        if (!isConfigured()) return "OLLAMA_BASE_URL n'est pas configure.";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(configuredBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return readOllamaError(response.statusCode(), response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode models = root.path("models");
            String wanted = configuredModel();
            String wantedPrefix = wanted + ":";
            for (JsonNode model : models) {
                String name = model.path("name").asText("");
                if (name.equalsIgnoreCase(wanted) || name.equalsIgnoreCase(wanted + ":latest") || name.toLowerCase().startsWith(wantedPrefix.toLowerCase())) {
                    return null;
                }
            }
            return "Modele Ollama introuvable: " + wanted + ". Lancez: ollama pull " + wanted;
        } catch (Exception e) {
            return "Impossible de contacter Ollama. Verifiez que Ollama est lance sur " + configuredBaseUrl() + ".";
        }
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode output = root.path("response");
        if (output.isTextual() && !output.asText().isBlank()) {
            return output.asText();
        }

        JsonNode error = root.path("error");
        if (error.isTextual() && !error.asText().isBlank()) {
            throw new IOException(error.asText());
        }
        throw new IOException("Impossible de lire le texte genere par l'IA.");
    }

    private String readOllamaError(int statusCode, String responseBody) {
        String message = "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            if (error.isTextual()) {
                message = error.asText();
            }
        } catch (Exception ignored) {
            message = responseBody == null ? "" : responseBody;
        }

        if (statusCode == 404) {
            return "Service Ollama introuvable sur " + configuredBaseUrl() + ".";
        }
        if (statusCode >= 500) {
            return "Service Ollama indisponible. Verifiez que Ollama est demarre.";
        }
        return "Ollama a retourne HTTP " + statusCode + (message.isBlank() ? "." : ": " + message);
    }

    private String cleanJsonText(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private String extractJsonObject(String text) throws IOException {
        if (text == null || text.isBlank()) {
            throw new IOException("La reponse IA est vide.");
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }

        throw new IOException("La reponse IA ne contient pas de JSON exploitable.");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
