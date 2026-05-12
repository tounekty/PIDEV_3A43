package org.example.service;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.model.Journal;

import java.io.IOException;
import java.time.Duration;

public class JournalAiAnalysisService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;

    public JournalAiAnalysisService() {
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String analyze(Journal journal) throws IOException {
        if (journal == null) {
            throw new IOException("Journal is required.");
        }
        String token = firstNonBlank(System.getenv("HF_TOKEN"), System.getenv("HUGGINGFACE_TOKEN"), System.getProperty("hf.token"));
        if (token == null) {
            throw new IOException("HuggingFace token is not configured. Set HF_TOKEN env var or -Dhf.token=...");
        }

        String model = firstNonBlank(System.getenv("HF_JOURNAL_MODEL"), System.getProperty("hf.journal.model"));
        if (model == null) {
            model = "distilbert-base-uncased-finetuned-sst-2-english";
        }

        String content = journal.getContent() == null ? "" : journal.getContent().trim();
        if (content.isEmpty()) {
            throw new IOException("Journal content is empty.");
        }

        String url = "https://api-inference.huggingface.co/models/" + model;
        String payload = "{\"inputs\":" + toJsonString(content) + "}";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            String body = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful()) {
                throw new IOException("HF API error (" + resp.code() + "): " + compact(body));
            }
            return compact(body);
        }
    }

    private String toJsonString(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
