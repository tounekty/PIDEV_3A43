package org.example.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Text-to-Speech service using gTTS (Google Translate TTS) - FREE
 * Falls back to eSpeak if Google request fails.
 */
public class GTTSTextToSpeechService {

    private static final int ESPEAK_SPEECH_RATE = 140;
    private static final String ESPEAK_DEFAULT_VOICE = "fr+f3";

    /**
     * Synthesize text to MP3 using gTTS or fallback to eSpeak
     */
    public static String synthesizeToFile(String text, String languageCode) throws IOException {
        return synthesizeToFile(text, languageCode, ESPEAK_DEFAULT_VOICE, ESPEAK_SPEECH_RATE);
    }

    /**
     * Synthesize text with configurable eSpeak voice and rate
     */
    public static String synthesizeToFile(String text, String languageCode, String voice, int speechRate) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        // Prefer the local engine when it is installed on Windows.
        String executable = resolveESpeakExecutable();
        if (executable != null) {
            return synthesizeWithESpeak(text, languageCode, executable, voice, speechRate);
        }

        // Last resort: try gTTS if no local engine is available.
        return synthesizeWithGTTS(text, languageCode);
    }

    /**
     * Synthesize using gTTS with improved headers and error handling
     */
    private static String synthesizeWithGTTS(String text, String languageCode) throws IOException {
        // Limit text length (Google limit ~100 chars per request)
        String truncatedText = text.length() > 200 ? text.substring(0, 200) + "..." : text;
        
        // Extract language code
        String lang = languageCode.split("-")[0];
        String encodedText = URLEncoder.encode(truncatedText, "UTF-8");

        // gTTS URL - improved format
        String url = "https://translate.google.com/translate_tts?ie=UTF-8&q=" + encodedText + "&tl=" + lang + "&client=gtx";

        File tempFile = File.createTempFile("tts_", ".mp3");
        tempFile.deleteOnExit();

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setRequestProperty("Accept", "audio/mpeg");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();
            if (status != 200) {
                throw new IOException("gTTS HTTP " + status);
            }

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            if (tempFile.length() == 0) {
                throw new IOException("Empty gTTS response");
            }

            return tempFile.getAbsolutePath();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Fallback: Synthesize using eSpeak (requires: choco install espeak)
     */
    private static String synthesizeWithESpeak(String text, String languageCode, String executable, String voice, int speechRate) throws IOException {
        String lang = resolveESpeakVoice(languageCode, voice);
        File tempFile = File.createTempFile("tts_", ".wav");
        tempFile.deleteOnExit();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                executable, "-v", lang, "-s", String.valueOf(speechRate), "-w", tempFile.getAbsolutePath(), text
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode != 0 || tempFile.length() == 0) {
                throw new IOException("eSpeak failed with exit code " + exitCode);
            }

            return tempFile.getAbsolutePath();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("eSpeak interrupted: " + e.getMessage());
        }
    }

    private static String resolveESpeakVoice(String languageCode, String voice) {
        if (voice != null && !voice.isBlank()) {
            return voice;
        }

        if (languageCode == null || languageCode.isBlank()) {
            return ESPEAK_DEFAULT_VOICE;
        }

        String lang = languageCode.split("-")[0].toLowerCase();
        if ("fr".equals(lang)) {
            return ESPEAK_DEFAULT_VOICE;
        }

        return lang;
    }

    private static String resolveESpeakExecutable() {
        String[] candidates = {
                "espeak-ng",
                "C:\\Program Files\\eSpeak NG\\espeak-ng.exe",
                "C:\\Program Files (x86)\\eSpeak NG\\espeak-ng.exe"
        };

        for (String candidate : candidates) {
            if (candidate.contains("\\") || candidate.contains(":")) {
                if (new File(candidate).exists()) {
                    return candidate;
                }
                continue;
            }

            try {
                Process p = new ProcessBuilder(candidate, "--version").start();
                if (p.waitFor() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // Try next candidate.
            }
        }
        return null;
    }

    private static boolean isESpeakInstalled() {
        return resolveESpeakExecutable() != null;
    }

    public static byte[] synthesizeToBytes(String text, String languageCode) throws IOException {
        String filePath = synthesizeToFile(text, languageCode);
        return Files.readAllBytes(Paths.get(filePath));
    }
}
