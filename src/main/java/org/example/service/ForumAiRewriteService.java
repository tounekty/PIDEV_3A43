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
    private static final String HUGGING_FACE_CHAT_URL = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2";
    private static final String DEFAULT_MODEL = "mistralai/Mistral-7B-Instruct-v0.2";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public boolean isConfigured() {
        String token = System.getenv("HF_TOKEN");
        return token != null && !token.isBlank();
    }

    public ForumRewriteSuggestion rewriteSubject(String title, String description) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("HF_TOKEN n'est pas configure.");
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

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Tu es un assistant qui reformule des sujets de forum en francais professionnel. Tu reponds uniquement en JSON valide.");

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", configuredModel());
        payload.putArray("messages").add(systemMessage).add(userMessage);
        payload.put("max_tokens", 700);
        payload.put("temperature", 0.4);
        payload.set("response_format", responseFormat);

        HttpRequest request = HttpRequest.newBuilder(URI.create(HUGGING_FACE_CHAT_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + System.getenv("HF_TOKEN"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(readHuggingFaceError(response.statusCode(), response.body()));
        }

        String outputText = extractOutputText(response.body());
        JsonNode suggestion = objectMapper.readTree(cleanJsonText(outputText));
        String rewrittenTitle = suggestion.path("title").asText("").trim();
        String rewrittenDescription = suggestion.path("description").asText("").trim();
        if (rewrittenTitle.isBlank() || rewrittenDescription.isBlank()) {
            throw new IOException("La reponse IA ne contient pas title et description.");
        }
        return new ForumRewriteSuggestion(rewrittenTitle, rewrittenDescription);
    }

    private String configuredModel() {
        String model = System.getenv("HF_MODEL");
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isTextual() && !content.asText().isBlank()) {
            return content.asText();
        }
        throw new IOException("Impossible de lire le texte genere par l'IA.");
    }

    private String readHuggingFaceError(int statusCode, String responseBody) {
        String message = "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            if (error.isTextual()) {
                message = error.asText();
            } else if (error.isObject()) {
                message = error.path("message").asText("");
            }
            if (message.isBlank()) {
                message = root.path("message").asText("");
            }
        } catch (Exception ignored) {
            message = responseBody == null ? "" : responseBody;
        }

        String lowerMessage = message.toLowerCase();
        if (statusCode == 401 || statusCode == 403) {
            return "Token Hugging Face invalide ou non autorise. Verifiez HF_TOKEN et ses permissions Inference Providers.";
        }
        if (statusCode == 404 || lowerMessage.contains("model") && lowerMessage.contains("not")) {
            return "Modele Hugging Face indisponible. Essayez HF_MODEL=Qwen/Qwen2.5-7B-Instruct ou choisissez un modele dans le Playground Hugging Face.";
        }
        if (statusCode == 429 || lowerMessage.contains("rate") || lowerMessage.contains("quota")) {
            return "Limite Hugging Face atteinte. Reessayez plus tard ou utilisez un autre modele/projet Hugging Face.";
        }
        if (statusCode >= 500) {
            return "Service Hugging Face temporairement indisponible. Reessayez dans quelques instants.";
        }
        return "Hugging Face a retourne HTTP " + statusCode + (message.isBlank() ? "." : ": " + message);
    }

    private String cleanJsonText(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
