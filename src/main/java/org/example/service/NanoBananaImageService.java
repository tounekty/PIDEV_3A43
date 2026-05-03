package org.example.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class NanoBananaImageService {
    private static final String DEFAULT_MODEL = "gemini-2.5-flash-image";
    private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    public String generateImage(String prompt) throws IOException {
        String apiKey = readRequiredEnv("GEMINI_API_KEY");
        String model = readEnvOrDefault("GEMINI_IMAGE_MODEL", DEFAULT_MODEL);

        Request request = new Request.Builder()
                .url(String.format(GEMINI_URL_TEMPLATE, model))
                .post(RequestBody.create(gson.toJson(buildRequest(prompt)), JSON))
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Erreur Nano Banana HTTP " + response.code() + ": " + extractErrorMessage(responseBody));
            }

            ImagePayload imagePayload = extractImagePayload(responseBody);
            if (imagePayload == null || imagePayload.data().isBlank()) {
                throw new IOException("Nano Banana n'a pas retourné d'image exploitable.");
            }

            return saveImage(imagePayload);
        }
    }

    private JsonObject buildRequest(String prompt) {
        JsonObject root = new JsonObject();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", enrichPrompt(prompt));

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);
        root.add("contents", contents);

        JsonArray responseModalities = new JsonArray();
        responseModalities.add("TEXT");
        responseModalities.add("IMAGE");

        JsonObject generationConfig = new JsonObject();
        generationConfig.add("responseModalities", responseModalities);
        root.add("generationConfig", generationConfig);

        return root;
    }

    private String enrichPrompt(String prompt) {
        String safePrompt = prompt == null || prompt.isBlank()
                ? "A modern educational wellness illustration"
                : prompt.trim();
        return safePrompt + """

                Create a modern, professional educational illustration for a mental wellness resource.
                Use a calm, positive, non-medical visual style.
                No readable text, no logo, no watermark, no brand names.
                Landscape composition, suitable for an article cover.
                """;
    }

    private ImagePayload extractImagePayload(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!root.has("candidates") || !root.get("candidates").isJsonArray() || root.getAsJsonArray("candidates").isEmpty()) {
            return null;
        }

        JsonObject candidate = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
        if (!candidate.has("content") || !candidate.get("content").isJsonObject()) {
            return null;
        }

        JsonObject content = candidate.getAsJsonObject("content");
        if (!content.has("parts") || !content.get("parts").isJsonArray()) {
            return null;
        }

        for (var partElement : content.getAsJsonArray("parts")) {
            if (!partElement.isJsonObject()) {
                continue;
            }
            JsonObject part = partElement.getAsJsonObject();
            if (!part.has("inlineData") || !part.get("inlineData").isJsonObject()) {
                continue;
            }

            JsonObject inlineData = part.getAsJsonObject("inlineData");
            String mimeType = getString(inlineData, "mimeType");
            String data = getString(inlineData, "data");
            if (!data.isBlank()) {
                return new ImagePayload(mimeType.isBlank() ? "image/png" : mimeType, data);
            }
        }

        return null;
    }

    private String saveImage(ImagePayload imagePayload) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(imagePayload.data());
        Path outputDir = Path.of(System.getProperty("user.dir"), "generated-images");
        Files.createDirectories(outputDir);

        String extension = imagePayload.mimeType().toLowerCase().contains("jpeg") ? ".jpg" : ".png";
        Path outputFile = outputDir.resolve("nano-banana-" + LocalDateTime.now().format(FILE_TS) + extension);
        Files.write(outputFile, bytes);

        return outputFile.toUri().toString();
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

    private record ImagePayload(String mimeType, String data) {
    }
}
