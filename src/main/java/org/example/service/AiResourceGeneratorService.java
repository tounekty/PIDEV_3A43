package org.example.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.example.model.Resource;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiResourceGeneratorService {
    private static final String GROQ_CHAT_COMPLETIONS_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final HuggingFaceImageService huggingFaceImageService = new HuggingFaceImageService();

    public Resource generateArticleDraft(String prompt, int userId) throws IOException {
        String apiKey = readRequiredEnv("GROQ_API_KEY");
        String model = readEnvOrDefault("GROQ_MODEL", DEFAULT_GROQ_MODEL);

        JsonObject requestJson = buildGroqRequest(model, prompt);
        Request request = new Request.Builder()
                .url(GROQ_CHAT_COMPLETIONS_URL)
                .post(RequestBody.create(gson.toJson(requestJson), JSON))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Erreur Groq HTTP " + response.code() + ": " + extractErrorMessage(responseBody));
            }

            String content = extractAssistantContent(responseBody);
            if (content == null || content.isBlank()) {
                throw new IOException("La réponse Groq ne contient pas de ressource exploitable.");
            }

            return mapDraftToResource(content, userId);
        }
    }

    private JsonObject buildGroqRequest(String model, String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("temperature", 0.4);
        root.addProperty("max_completion_tokens", 1200);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        root.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        messages.add(message("system", """
                Tu es un assistant de création de ressources pédagogiques.
                Réponds uniquement avec un objet JSON valide, sans markdown.
                Le JSON doit contenir exactement ces champs:
                title, description, type, filePath, videoUrl, imageUrl, imagePrompt.
                type doit toujours être "article".
                filePath, videoUrl et imageUrl doivent être des chaînes vides si l'utilisateur ne donne rien.
                imagePrompt doit être en anglais et décrire une image moderne, positive et non médicale, adaptée au contexte.
                La description doit être en français, claire, professionnelle, et directement publiable.
                Elle doit contenir 2 à 4 paragraphes courts et des conseils pratiques.
                """));
        messages.add(message("user", "Génère une ressource à partir de ce prompt:\n" + prompt));
        root.add("messages", messages);

        return root;
    }

    private JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private Resource mapDraftToResource(String outputText, int userId) throws IOException {
        try {
            String jsonText = extractJsonObject(outputText);
            JsonObject draft = JsonParser.parseString(jsonText).getAsJsonObject();
            Resource resource = new Resource(
                    getString(draft, "title"),
                    getString(draft, "description"),
                    Resource.TYPE_ARTICLE,
                    userId
            );
            resource.setFilePath(getString(draft, "filePath"));
            resource.setVideoUrl(getString(draft, "videoUrl"));
            resource.setImageUrl(resolveGeneratedImagePath(draft));
            return resource;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Format de réponse Groq invalide: " + e.getMessage(), e);
        }
    }

    private String resolveGeneratedImagePath(JsonObject draft) throws IOException {
        String explicitImageUrl = getString(draft, "imageUrl");
        if (!explicitImageUrl.isBlank()) {
            return explicitImageUrl;
        }

        String imagePrompt = getString(draft, "imagePrompt");
        if (imagePrompt.isBlank()) {
            imagePrompt = getString(draft, "title") + ". " + getString(draft, "description");
        }

        return huggingFaceImageService.generateImage(imagePrompt);
    }

    private String extractAssistantContent(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!root.has("choices") || !root.get("choices").isJsonArray() || root.getAsJsonArray("choices").isEmpty()) {
            return null;
        }

        JsonObject choice = root.getAsJsonArray("choices").get(0).getAsJsonObject();
        if (!choice.has("message") || !choice.get("message").isJsonObject()) {
            return null;
        }

        JsonObject message = choice.getAsJsonObject("message");
        if (!message.has("content") || message.get("content").isJsonNull()) {
            return null;
        }

        return message.get("content").getAsString();
    }

    private String extractJsonObject(String value) {
        if (value == null) {
            return "";
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return value.trim();
        }
        return value.substring(start, end + 1);
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "réponse vide";
        }
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            return responseBody;
        }
        return responseBody;
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString().trim();
    }

    private String readRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variable d'environnement manquante: " + key);
        }
        return value.trim();
    }

    private String readEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
