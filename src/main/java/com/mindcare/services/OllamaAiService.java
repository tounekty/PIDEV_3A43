package com.mindcare.services;

import com.mindcare.model.PatientFile;
import com.mindcare.utils.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaAiService {

    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.1:8b";
    private static final int DEFAULT_NUM_CTX = 1024;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;
    private final int numCtx;

    public OllamaAiService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.baseUrl = AppConfig.get("ollama.baseUrl", "OLLAMA_BASE_URL", DEFAULT_BASE_URL);
        this.model = AppConfig.get("ollama.model", "OLLAMA_MODEL", DEFAULT_MODEL);
        this.numCtx = parseIntOrDefault(
            AppConfig.get("ollama.numCtx", "OLLAMA_NUM_CTX", String.valueOf(DEFAULT_NUM_CTX)),
            DEFAULT_NUM_CTX
        );
    }

    public String suggestBestTimesForStudentBooking(String psychologueName, String acceptanceStats) {
        String prompt = "Tu es un assistant pour une application de psychologie.\n" +
            "Objectif: proposer au maximum 3 horaires recommandés pour augmenter la probabilité d'acceptation.\n" +
            "Psychologue: " + safe(psychologueName) + "\n" +
            "Statistiques historiques (créneaux -> acceptés/total et taux):\n" +
            safe(acceptanceStats) + "\n\n" +
            "Retourne une réponse courte en français avec:\n" +
            "1) 3 créneaux max\n" +
            "2) une phrase de justification\n" +
            "3) si les données sont faibles, le dire clairement.\n";
        return generate(prompt);
    }

    public String suggestNextSessionFromPatientFile(String studentName, PatientFile file) {
        if (isLikelyNoisyOrInsufficient(file)) {
            return "Les données du dossier semblent incomplètes ou incohérentes (caractères aléatoires / informations trop faibles). "
                + "Veuillez renseigner des informations cliniques plus structurées avant de demander une suggestion IA.";
        }

        StringBuilder patientSummary = new StringBuilder();
        patientSummary.append("Étudiant: ").append(safe(studentName)).append("\n");
        if (file != null) {
            patientSummary.append("Traitements en cours: ").append(safe(file.getTraitementsEnCours())).append("\n");
            patientSummary.append("Allergies: ").append(safe(file.getAllergies())).append("\n");
            patientSummary.append("Antécédents personnels: ").append(safe(file.getAntecedentsPersonnels())).append("\n");
            patientSummary.append("Antécédents familiaux: ").append(safe(file.getAntecedentsFamiliaux())).append("\n");
            patientSummary.append("Motif consultation: ").append(safe(file.getMotifConsultation())).append("\n");
            patientSummary.append("Objectifs thérapeutiques: ").append(safe(file.getObjectifsTherapeutiques())).append("\n");
            patientSummary.append("Notes générales: ").append(safe(file.getNotesGenerales())).append("\n");
            patientSummary.append("Niveau de risque: ").append(safe(file.getNiveauRisque())).append("\n");
        } else {
            patientSummary.append("Aucun dossier patient disponible.");
        }

        String prompt = "Tu es un assistant clinique d'aide à la préparation de séance pour psychologue.\n" +
            "En te basant uniquement sur les informations suivantes, propose une suggestion pour la prochaine séance.\n" +
            "Données patient:\n" + patientSummary + "\n\n" +
            "Réponds en français, format bref:\n" +
            "- Priorités de la prochaine séance (3 points max)\n" +
            "- Questions ciblées à poser (3 max)\n" +
            "- Précautions / points de vigilance\n" +
            "Important: c'est une aide à la réflexion, pas un diagnostic médical.\n";
        return generate(prompt);
    }

    private boolean isLikelyNoisyOrInsufficient(PatientFile file) {
        if (file == null) {
            return true;
        }
        String[] candidates = new String[] {
            file.getTraitementsEnCours(),
            file.getAllergies(),
            file.getAntecedentsPersonnels(),
            file.getAntecedentsFamiliaux(),
            file.getMotifConsultation(),
            file.getObjectifsTherapeutiques(),
            file.getNotesGenerales(),
            file.getNiveauRisque()
        };

        StringBuilder merged = new StringBuilder();
        int informativeFields = 0;
        for (String value : candidates) {
            if (value == null) {
                continue;
            }
            String cleaned = value.trim();
            if (cleaned.isEmpty() || "[Vide]".equalsIgnoreCase(cleaned)) {
                continue;
            }
            informativeFields++;
            merged.append(cleaned).append(' ');
        }

        if (informativeFields < 2) {
            return true;
        }

        String text = merged.toString().trim();
        if (text.length() < 40) {
            return true;
        }
        if (text.matches(".*(.)\\1{5,}.*")) {
            return true;
        }

        String lettersOnly = text.replaceAll("[^A-Za-zÀ-ÖØ-öø-ÿ]", "");
        if (lettersOnly.length() < 20) {
            return true;
        }

        long vowelCount = lettersOnly.toLowerCase(Locale.ROOT)
            .chars()
            .filter(ch -> "aeiouyàâäéèêëîïôöùûüÿ".indexOf(ch) >= 0)
            .count();
        double vowelRatio = (double) vowelCount / (double) lettersOnly.length();
        return vowelRatio < 0.18;
    }

    private String generate(String prompt) {
        try {
            HttpResponse<String> response = sendGenerateRequest(model, prompt);
            if (response.statusCode() == 404) {
                String fallbackModel = resolveFallbackModel();
                if (fallbackModel != null && !fallbackModel.equals(model)) {
                    response = sendGenerateRequest(fallbackModel, prompt);
                }
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() == 404) {
                    List<String> installedModels = getInstalledModels();
                    throw new IllegalStateException("Ollama model introuvable. Modèle configuré: "
                        + model + ". Modèles installés: " + String.join(", ", installedModels));
                }
                if (response.statusCode() == 500) {
                    String errorMsg = extractErrorMessage(response.body());
                    if (errorMsg.toLowerCase().contains("cuda") || errorMsg.toLowerCase().contains("gpu")) {
                        throw new IllegalStateException(
                            "Ollama CUDA/GPU error - Le processus Ollama a planté (problème GPU/VRAM). " +
                            "Solutions: 1) Redémarrez Ollama, 2) Utilisez CPU_ONLY=1 pour forcer CPU mode, " +
                            "3) Réduisez la taille du modèle. Erreur: " + errorMsg);
                    } else if (errorMsg.toLowerCase().contains("out of memory")) {
                        throw new IllegalStateException(
                            "Ollama OUT OF MEMORY - Mémoire GPU insuffisante pour le modèle " + model + ". " +
                            "Solutions: 1) Redémarrez Ollama, 2) Utilisez un modèle plus petit (mistral, neural-chat), " +
                            "3) Augmentez la VRAM ou passez en CPU mode. Erreur: " + errorMsg);
                    }
                }
                throw new IllegalStateException("Ollama request failed with status "
                    + response.statusCode() + " - " + extractErrorMessage(response.body()));
            }
            String text = extractResponseText(response.body());
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Ollama returned an empty response");
            }
            return text.trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to contact Ollama. Verify Ollama is running locally.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to contact Ollama. Verify Ollama is running locally.", exception);
        }
    }

    private HttpResponse<String> sendGenerateRequest(String targetModel, String prompt) throws IOException, InterruptedException {
        String payload = "{"
            + "\"model\":\"" + escapeJson(targetModel) + "\","
            + "\"prompt\":\"" + escapeJson(prompt) + "\","
            + "\"options\":{\"num_ctx\":" + numCtx + "},"
            + "\"stream\":false"
            + "}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/generate"))
            .timeout(Duration.ofSeconds(180))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String resolveFallbackModel() {
        List<String> installedModels = getInstalledModels();
        if (installedModels.isEmpty()) {
            return null;
        }
        if (installedModels.contains(model)) {
            return model;
        }
        if (installedModels.contains("mistral:latest")) {
            return "mistral:latest";
        }
        if (installedModels.contains("llama2:latest")) {
            return "llama2:latest";
        }
        return installedModels.get(0);
    }

    private List<String> getInstalledModels() {
        List<String> models = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return models;
            }
            Matcher matcher = MODEL_NAME_PATTERN.matcher(response.body());
            while (matcher.find()) {
                String modelName = matcher.group(1);
                if (modelName != null && !modelName.isBlank()) {
                    models.add(modelName);
                }
            }
        } catch (Exception ignored) {
            return models;
        }
        return models;
    }

    private String extractResponseText(String json) {
        if (json == null) {
            return null;
        }
        Matcher matcher = RESPONSE_PATTERN.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private String extractErrorMessage(String json) {
        if (json == null || json.isBlank()) {
            return "No response body";
        }
        Pattern errorPattern = Pattern.compile("\"error\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = errorPattern.matcher(json);
        if (!matcher.find()) {
            return json;
        }
        return unescapeJson(matcher.group(1));
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
