package org.example.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service de generation d'image via Pollinations (gratuit, sans cle API).
 * API: https://image.pollinations.ai/prompt/{prompt}
 */
public class PollinationsImageService {
    private static final String BASE_URL = "https://image.pollinations.ai/prompt/";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();

    public String generateImage(String prompt) throws IOException {
        String enrichedPrompt = enrichPrompt(prompt);
        String encodedPrompt = URLEncoder.encode(enrichedPrompt, StandardCharsets.UTF_8);
        String url = BASE_URL + encodedPrompt + "?width=1024&height=576&nologo=true";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() == null ? "" : response.body().string();
                throw new IOException("Erreur Pollinations HTTP " + response.code() + ": " + truncate(body));
            }

            if (response.body() == null) {
                throw new IOException("Pollinations a renvoye une reponse vide.");
            }

            byte[] bytes = response.body().bytes();
            if (bytes.length == 0) {
                throw new IOException("Pollinations a renvoye une image vide.");
            }

            return saveImage(bytes);
        }
    }

    private String enrichPrompt(String prompt) {
        String safePrompt = (prompt == null || prompt.isBlank())
                ? "Modern educational illustration"
                : prompt.trim();

        return safePrompt + ", modern, professional, clean design, educational context, no text, no watermark";
    }

    private String saveImage(byte[] bytes) throws IOException {
        Path outputDir = Path.of(System.getProperty("user.dir"), "generated-images");
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve("pollinations-" + LocalDateTime.now().format(FILE_TS) + ".png");
        Files.write(outputFile, bytes);
        return outputFile.toUri().toString();
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "reponse vide";
        }
        return value.length() <= 200 ? value : value.substring(0, 200);
    }
}
