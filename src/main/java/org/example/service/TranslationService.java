package org.example.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service de traduction utilisant LibreTranslate API (gratuit, open-source)
 * Alternative: MyMemory, Google Translate
 */
public class TranslationService {
    // API LibreTranslate (instance publique)
    private static final String LIBRETRANSLATE_API = "https://libretranslate.de/translate";
    // Fallback API MyMemory
    private static final String MYMEMORY_API = "https://api.mymemory.translated.net/get";
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
    
    /**
     * Langues supportées
     */
    public enum Language {
        FR("fr", "Français"),
        EN("en", "English"),
        ES("es", "Español"),
        DE("de", "Deutsch"),
        IT("it", "Italiano"),
        PT("pt", "Português"),
        RU("ru", "Русский"),
        JA("ja", "日本語"),
        ZH("zh", "中文"),
        AR("ar", "العربية");
        
        public final String code;
        public final String label;
        
        Language(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }
    
    /**
     * Traduit un texte d'une langue à une autre
     */
    public static String translate(String text, String fromLang, String toLang) {
        if (text == null || text.isBlank() || fromLang.equals(toLang)) {
            return text;
        }
        
        try {
            // Essayer LibreTranslate d'abord
            String result = translateWithLibreTranslate(text, fromLang, toLang);
            if (result != null && !result.equals(text)) {
                return result;
            }
        } catch (Exception e) {
            System.err.println("LibreTranslate failed: " + e.getMessage());
        }
        
        try {
            // Fallback sur MyMemory
            String result = translateWithMyMemory(text, fromLang, toLang);
            if (result != null && !result.equals(text)) {
                return result;
            }
        } catch (Exception e) {
            System.err.println("MyMemory failed: " + e.getMessage());
        }
        
        return text;
    }
    
    /**
     * Traduction avec LibreTranslate (plus fiable)
     */
    private static String translateWithLibreTranslate(String text, String fromLang, String toLang) throws IOException {
        try {
            System.out.println("🔄 Trying LibreTranslate...");
            
            String jsonBody = String.format("{\"q\":\"%s\",\"source\":\"%s\",\"target\":\"%s\"}", 
                escapeJson(text), 
                fromLang.toLowerCase(), 
                toLang.toLowerCase());
            
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(LIBRETRANSLATE_API)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("LibreTranslate HTTP " + response.code());
                    return null;
                }
                
                String responseStr = response.body().string();
                System.out.println("📨 LibreTranslate response: " + responseStr);
                
                JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
                
                if (json.has("translatedText")) {
                    String translated = json.get("translatedText").getAsString().trim();
                    if (!translated.isEmpty() && !translated.equalsIgnoreCase(text)) {
                        System.out.println("✅ LibreTranslate success: " + translated);
                        return translated;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ LibreTranslate error: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Traduction avec MyMemory API (fallback)
     */
    private static String translateWithMyMemory(String text, String fromLang, String toLang) throws IOException {
        try {
            System.out.println("🔄 Trying MyMemory...");
            
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = MYMEMORY_API + "?q=" + encoded + "&langpair=" + fromLang.toLowerCase() + "|" + toLang.toLowerCase();
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .get()
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("MyMemory HTTP " + response.code());
                    return null;
                }
                
                String responseStr = response.body().string();
                System.out.println("📨 MyMemory response: " + responseStr);
                
                JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
                
                if (json.has("responseStatus") && json.get("responseStatus").getAsInt() == 200) {
                    JsonObject data = json.getAsJsonObject("responseData");
                    if (data != null && data.has("translatedText")) {
                        String translated = data.get("translatedText").getAsString().trim();
                        if (!translated.isEmpty() && !translated.equalsIgnoreCase(text)) {
                            System.out.println("✅ MyMemory success: " + translated);
                            return translated;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ MyMemory error: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Échappe les caractères JSON
     */
    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Traduit vers multiple langues
     */
    public static Map<String, String> translateToMultiple(String text, String fromLang, String... targetLanguages) {
        Map<String, String> translations = new HashMap<>();
        translations.put(fromLang, text);
        
        for (String toLang : targetLanguages) {
            if (!fromLang.equals(toLang)) {
                String translated = translate(text, fromLang, toLang);
                translations.put(toLang, translated);
            }
        }
        
        return translations;
    }
    
    /**
     * Liste des langues disponibles
     */
    public static Language[] getAvailableLanguages() {
        return Language.values();
    }
}
