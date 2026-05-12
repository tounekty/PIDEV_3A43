package com.mindcare.services;

import com.mindcare.model.Appointment;
import com.mindcare.utils.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZoomMeetingService {

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MEETING_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern JOIN_URL_PATTERN = Pattern.compile("\"join_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CREATED_AT_PATTERN = Pattern.compile("\"created_at\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final ZoomConfig config;

    public ZoomMeetingService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        this.config = ZoomConfig.fromEnvironment();
    }

    public Optional<String> createJoinUrl(Appointment appointment) {
        return createMeeting(appointment).map(ZoomMeetingData::getJoinUrl);
    }

    public Optional<ZoomMeetingData> createMeeting(Appointment appointment) {
        if (appointment == null || !isOnline(appointment.getLocation()) || !config.isConfigured()) {
            return Optional.empty();
        }

        try {
            String accessToken = fetchAccessToken();
            String responseBody = createMeeting(accessToken, appointment);
            String meetingId = extractJsonValue(responseBody, MEETING_ID_PATTERN);
            String joinUrl = extractJsonValue(responseBody, JOIN_URL_PATTERN);
            LocalDateTime createdAt = parseCreatedAt(extractJsonValue(responseBody, CREATED_AT_PATTERN));
            if (joinUrl == null || joinUrl.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ZoomMeetingData(meetingId, joinUrl, createdAt));
        } catch (Exception exception) {
            System.err.println("[ZoomMeetingService] Unable to create Zoom meeting: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private String fetchAccessToken() throws IOException, InterruptedException {
        String authValue = java.util.Base64.getEncoder().encodeToString((config.clientId + ":" + config.clientSecret).getBytes(StandardCharsets.UTF_8));
        String formBody = "grant_type=account_credentials&account_id=" + URLEncoder.encode(config.accountId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://zoom.us/oauth/token"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Basic " + authValue)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Zoom token request failed with status " + response.statusCode());
        }

        String token = extractJsonValue(response.body(), ACCESS_TOKEN_PATTERN);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Zoom token response did not contain an access token");
        }
        return token;
    }

    private String createMeeting(String accessToken, Appointment appointment) throws IOException, InterruptedException {
        String startTime = appointment.getDateTime() == null
            ? null
            : ZonedDateTime.of(appointment.getDateTime(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String topic = buildTopic(appointment);
        String body = "{" +
            "\"topic\":\"" + escapeJson(topic) + "\"," +
            "\"type\":2," +
            (startTime == null ? "" : "\"start_time\":\"" + escapeJson(startTime) + "\",") +
            "\"duration\":60," +
            "\"timezone\":\"" + escapeJson(config.timezone) + "\"," +
            "\"settings\":{\"waiting_room\":false,\"join_before_host\":false}" +
            "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.zoom.us/v2/users/" + URLEncoder.encode(config.userId, StandardCharsets.UTF_8) + "/meetings"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Zoom meeting request failed with status " + response.statusCode());
        }
        return response.body();
    }

    private String buildTopic(Appointment appointment) {
        StringBuilder builder = new StringBuilder("MindCare appointment");
        if (appointment.getStudentName() != null && !appointment.getStudentName().isBlank()) {
            builder.append(" - ").append(appointment.getStudentName());
        }
        if (appointment.getPsyName() != null && !appointment.getPsyName().isBlank()) {
            builder.append(" / ").append(appointment.getPsyName());
        }
        return builder.toString();
    }

    private String extractJsonValue(String payload, Pattern pattern) {
        if (payload == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\/", "/");
    }

    private LocalDateTime parseCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private boolean isOnline(String location) {
        return location != null && location.trim().equalsIgnoreCase("online");
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

    private static final class ZoomConfig {
        private final String accountId;
        private final String clientId;
        private final String clientSecret;
        private final String userId;
        private final String timezone;

        private ZoomConfig(String accountId, String clientId, String clientSecret, String userId, String timezone) {
            this.accountId = accountId;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.userId = userId;
            this.timezone = timezone;
        }

        static ZoomConfig fromEnvironment() {
            return new ZoomConfig(
                AppConfig.get("zoom.accountId", "ZOOM_ACCOUNT_ID", ""),
                AppConfig.get("zoom.clientId", "ZOOM_CLIENT_ID", ""),
                AppConfig.get("zoom.clientSecret", "ZOOM_CLIENT_SECRET", ""),
                AppConfig.get("zoom.userId", "ZOOM_USER_ID", "me"),
                AppConfig.get("zoom.timezone", "ZOOM_TIMEZONE", ZoneId.systemDefault().getId())
            );
        }

        boolean isConfigured() {
            return !accountId.isBlank() && !clientId.isBlank() && !clientSecret.isBlank();
        }

    }
}