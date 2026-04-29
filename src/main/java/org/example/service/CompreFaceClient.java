package org.example.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CompreFaceClient {
    private static final String BASE_URL = "http://localhost:8000";
    // Using the same API key from your PHP project's .env
    private static final String API_KEY = "473d629b-27ec-49f6-b736-e92e0ebd49d5";
    private static final double MIN_SIMILARITY = 0.9;
    private final HttpClient httpClient;

    public CompreFaceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String recognizeFace(String base64Image) throws Exception {
        String url = BASE_URL + "/api/v1/recognition/recognize?prediction_count=1&det_prob_threshold=0.7";

        JSONObject payload = new JSONObject();
        payload.put("file", base64Image);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception("CompreFace API Error: " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        if (!jsonResponse.has("result")) {
            return null;
        }

        JSONArray results = jsonResponse.getJSONArray("result");
        if (results.isEmpty()) {
            return null;
        }

        JSONObject firstResult = results.getJSONObject(0);
        JSONArray subjects = firstResult.getJSONArray("subjects");
        if (subjects.isEmpty()) {
            return null;
        }

        JSONObject firstSubject = subjects.getJSONObject(0);
        double similarity = firstSubject.getDouble("similarity");
        String subject = firstSubject.getString("subject");

        if (similarity >= MIN_SIMILARITY) {
            return subject;
        }

        return null;
    }

    public boolean enrollFace(String base64Image, String subject) throws Exception {
        String url = BASE_URL + "/api/v1/recognition/faces?subject=" + subject;

        JSONObject payload = new JSONObject();
        payload.put("file", base64Image);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception("CompreFace API Error (Enroll): " + response.body());
        }

        return true;
    }

    public boolean deleteSubject(String subject) throws Exception {
        String url = BASE_URL + "/api/v1/recognition/subjects/" + subject;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-api-key", API_KEY)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 404 means the subject didn't exist anyway
        if (response.statusCode() >= 400 && response.statusCode() != 404) {
            throw new Exception("CompreFace API Error (Delete): " + response.body());
        }

        return true;
    }
}
