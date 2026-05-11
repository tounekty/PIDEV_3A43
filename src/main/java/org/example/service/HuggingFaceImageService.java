package org.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service de generation d'image avec Hugging Face Inference API.
 */
public class HuggingFaceImageService {
	private static final String ROUTER_API_URL = "https://router.huggingface.co/hf-inference/models/";
	private static final String LEGACY_API_URL = "https://api-inference.huggingface.co/models/";
	private static final String DEFAULT_MODEL = "black-forest-labs/FLUX.1-schnell";
	private static final String BACKUP_MODEL = "stabilityai/stable-diffusion-3-medium-diffusers";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

	private final OkHttpClient client = new OkHttpClient.Builder()
			.connectTimeout(20, TimeUnit.SECONDS)
			.readTimeout(120, TimeUnit.SECONDS)
			.writeTimeout(20, TimeUnit.SECONDS)
			.build();
	private final Gson gson = new Gson();

	public String generateImage(String prompt) throws IOException {
		String apiKey = readRequiredEnv("HUGGING_FACE_API_KEY");
		String model = readEnvOrDefault("HUGGING_FACE_MODEL", DEFAULT_MODEL);
		String enrichedPrompt = enrichPrompt(prompt);

		IOException lastError = null;
		for (String currentModel : new String[]{model, BACKUP_MODEL}) {
			try {
				return generateWithModel(currentModel, apiKey, enrichedPrompt);
			} catch (IOException modelError) {
				lastError = modelError;
			}
		}

		throw new IOException("Echec Hugging Face: " + (lastError == null ? "erreur inconnue" : lastError.getMessage()));
	}

	private String generateWithModel(String model, String apiKey, String prompt) throws IOException {
		Request requestRouter = buildRequest(ROUTER_API_URL + model, apiKey, prompt);
		try {
			return executeAndSaveImage(requestRouter);
		} catch (IOException routerError) {
			// Fallback endpoint for accounts/environments where router is blocked.
			Request requestLegacy = buildRequest(LEGACY_API_URL + model, apiKey, prompt);
			try {
				return executeAndSaveImage(requestLegacy);
			} catch (IOException legacyError) {
				throw new IOException("modele '" + model + "' indisponible: " + legacyError.getMessage(), routerError);
			}
		}
	}

	private Request buildRequest(String url, String apiKey, String prompt) {
		JsonObject root = new JsonObject();
		root.addProperty("inputs", prompt);

		JsonObject parameters = new JsonObject();
		parameters.addProperty("guidance_scale", 7.5);
		parameters.addProperty("num_inference_steps", 30);
		parameters.addProperty("negative_prompt", "blurry, low quality, text, watermark, logo");
		root.add("parameters", parameters);

		JsonObject options = new JsonObject();
		options.addProperty("wait_for_model", true);
		root.add("options", options);

		return new Request.Builder()
				.url(url)
				.post(RequestBody.create(gson.toJson(root), JSON))
				.addHeader("Authorization", "Bearer " + apiKey)
				.addHeader("Accept", "image/png")
				.addHeader("Content-Type", "application/json")
				.build();
	}

	private String executeAndSaveImage(Request request) throws IOException {
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				String body = response.body() == null ? "" : response.body().string();
				throw new IOException("HTTP " + response.code() + ": " + extractErrorMessage(body));
			}

			if (response.body() == null) {
				throw new IOException("Reponse vide de Hugging Face.");
			}

			String contentType = response.header("Content-Type", "");
			byte[] bytes = response.body().bytes();

			if (contentType.contains("application/json")) {
				String body = new String(bytes);
				throw new IOException(extractErrorMessage(body));
			}

			if (bytes.length == 0) {
				throw new IOException("Image vide retournee par Hugging Face.");
			}

			return saveImage(bytes, contentType);
		}
	}

	private String saveImage(byte[] bytes, String contentType) throws IOException {
		Path outputDir = Path.of(System.getProperty("user.dir"), "generated-images");
		Files.createDirectories(outputDir);

		String extension = contentType != null && contentType.toLowerCase().contains("jpeg") ? ".jpg" : ".png";
		Path outputFile = outputDir.resolve("hugging-face-" + LocalDateTime.now().format(FILE_TS) + extension);
		Files.write(outputFile, bytes);
		return outputFile.toUri().toString();
	}

	private String enrichPrompt(String prompt) {
		String safePrompt = (prompt == null || prompt.isBlank())
				? "Modern educational illustration"
				: prompt.trim();
		return safePrompt + ", modern, professional, clean style, educational context, no text, no watermark";
	}

	private String extractErrorMessage(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "reponse vide";
		}

		try {
			JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
			if (root.has("error") && !root.get("error").isJsonNull()) {
				return root.get("error").getAsString();
			}
			if (root.has("estimated_time")) {
				return "Modele en chargement. Reessayez dans quelques secondes.";
			}
		} catch (Exception ignored) {
			return truncate(responseBody);
		}

		return truncate(responseBody);
	}

	private String truncate(String value) {
		return value.length() <= 200 ? value : value.substring(0, 200);
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
