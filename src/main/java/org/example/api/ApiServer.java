package org.example.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.auth.AppUser;
import org.example.auth.AuthService;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.event.ReminderService;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApiServer {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_PORT = 8080;

    private final AuthService authService = new AuthService();
    private final EventService eventService = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final ReminderService reminderService = new ReminderService();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        ApiServer app = new ApiServer();
        app.initializeDatabase();
        app.start(port);
    }

    private void initializeDatabase() {
        try {
            authService.initializeUsers();
            eventService.createTableIfNotExists();
            reservationService.initializeReservations();
            reminderService.initializeReminderTable();
        } catch (SQLException e) {
            throw new IllegalStateException("Database init failed: " + e.getMessage(), e);
        }
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/events", new EventsHandler());
        server.createContext("/api/reservations", new ReservationsHandler());
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/calendar", new CalendarHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("API running on http://localhost:" + port);
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isPreflight(exchange)) {
                sendEmpty(exchange, 204);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            sendJson(exchange, 200, Map.of("status", "ok"));
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isPreflight(exchange)) {
                sendEmpty(exchange, 204);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                LoginRequest payload = fromJson(exchange, LoginRequest.class);
                if (payload == null || isBlank(payload.username) || isBlank(payload.password)) {
                    sendError(exchange, 400, "username and password are required");
                    return;
                }

                AppUser user = authService.login(payload.username.trim(), payload.password.trim());
                if (user == null) {
                    sendError(exchange, 401, "Invalid credentials");
                    return;
                }

                sendJson(exchange, 200, Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "role", user.getRole()
                ));
            } catch (JsonSyntaxException e) {
                sendError(exchange, 400, "Invalid JSON payload");
            } catch (SQLException e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    private class CalendarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isPreflight(exchange)) {
                sendEmpty(exchange, 204);
                return;
            }

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetCalendar(exchange, path);
                    return;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    handlePostCalendar(exchange, path);
                    return;
                }
                sendError(exchange, 405, "Method not allowed");
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (SQLException e) {
                sendError(exchange, 500, e.getMessage());
            }
        }

        // ==================== FRONT OFFICE (USER) ====================
        
        private void handleGetCalendar(HttpExchange exchange, String path) throws SQLException, IOException {
            // PUBLIC: Voir le calendrier complet
            if (path.equals("/api/calendar")) {
                String mode = queryValue(exchange, "mode");
                String dateRaw = queryValue(exchange, "date");
                LocalDate date = isBlank(dateRaw) ? LocalDate.now() : LocalDate.parse(dateRaw);
                String safeMode = isBlank(mode) ? "month" : mode.trim().toLowerCase();
                Range range = toRange(safeMode, date);

                List<Event> events = eventService.getEventsInRange(range.start(), range.end());
                sendJson(exchange, 200, Map.of(
                        "type", "calendar",
                        "mode", safeMode,
                        "date", date.toString(),
                        "rangeStart", range.start().toString(),
                        "rangeEnd", range.end().toString(),
                        "events", events.stream().map(this::toEventMap).toList()
                ));
                return;
            }

            // USER: Voir les événements d'un jour spécifique
            if (path.equals("/api/calendar/day")) {
                String dateRaw = queryValue(exchange, "date");
                LocalDate date = isBlank(dateRaw) ? LocalDate.now() : LocalDate.parse(dateRaw);
                
                List<Event> events = eventService.getEventsInRange(date.atStartOfDay(), date.atTime(23, 59, 59));
                sendJson(exchange, 200, Map.of(
                        "type", "day_view",
                        "date", date.toString(),
                        "events", events.stream().map(this::toEventMap).toList()
                ));
                return;
            }

            // USER: Planning personnel /api/calendar/user/{id}
            if (path.startsWith("/api/calendar/user/")) {
                String idStr = path.substring("/api/calendar/user/".length());
                Integer userId = tryParseInt(idStr);
                if (userId == null) {
                    sendError(exchange, 400, "Invalid user ID");
                    return;
                }
                
                String mode = queryValue(exchange, "mode");
                String dateRaw = queryValue(exchange, "date");
                LocalDate date = isBlank(dateRaw) ? LocalDate.now() : LocalDate.parse(dateRaw);
                String safeMode = isBlank(mode) ? "month" : mode.trim().toLowerCase();
                Range range = toRange(safeMode, date);

                List<Event> events = eventService.getUserPlanning(userId, range.start(), range.end());
                sendJson(exchange, 200, Map.of(
                        "type", "user_planning",
                        "userId", userId,
                        "mode", safeMode,
                        "date", date.toString(),
                        "events", events.stream().map(this::toEventMap).toList()
                ));
                return;
            }

            // USER: Voir disponibilités /api/calendar/free-busy
            if (path.equals("/api/calendar/free-busy")) {
                String userIdRaw = queryValue(exchange, "userId");
                String dateRaw = queryValue(exchange, "date");
                
                Integer userId = tryParseInt(userIdRaw);
                LocalDate date = isBlank(dateRaw) ? LocalDate.now() : LocalDate.parse(dateRaw);
                
                List<Event> events = userId != null 
                    ? eventService.getUserPlanning(userId, date.atStartOfDay(), date.plusDays(6).atTime(23, 59, 59))
                    : eventService.getEventsInRange(date.atStartOfDay(), date.plusDays(6).atTime(23, 59, 59));

                Map<String, Object> freeBusy = new LinkedHashMap<>();
                freeBusy.put("type", "free_busy");
                freeBusy.put("userId", userId);
                freeBusy.put("date", date.toString());
                freeBusy.put("availableSlots", calculateAvailableSlots(date, events));
                freeBusy.put("busyTimes", events.stream().map(this::toEventMap).toList());
                
                sendJson(exchange, 200, freeBusy);
                return;
            }

            // USER: Rappels /api/calendar/reminders?userId=1
            if (path.equals("/api/calendar/reminders")) {
                String userIdRaw = queryValue(exchange, "userId");
                Integer userId = tryParseInt(userIdRaw);
                if (userId == null) {
                    sendError(exchange, 400, "userId is required");
                    return;
                }
                
                List<Map<String, Object>> reminders = reminderService.getUserReminders(userId).stream()
                    .map(r -> Map.of(
                        "eventId", r.get("eventId"),
                        "minutesBefore", r.get("minutesBefore"),
                        "enabled", r.get("enabled")
                    ))
                    .toList();

                sendJson(exchange, 200, Map.of(
                    "type", "reminders",
                    "userId", userId,
                    "reminders", reminders
                ));
                return;
            }

            sendError(exchange, 404, "Route not found");
        }

        // ==================== BACK OFFICE (ADMIN) ====================
        
        private void handlePostCalendar(HttpExchange exchange, String path) throws SQLException, IOException {
            // ADMIN: Proposer des créneaux /api/calendar/admin/suggest-slot
            if (path.equals("/api/calendar/admin/suggest-slot")) {
                SuggestSlotRequest payload = fromJson(exchange, SuggestSlotRequest.class);
                if (payload == null || isBlank(payload.title) || payload.durationMinutes == null) {
                    sendError(exchange, 400, "title and durationMinutes are required");
                    return;
                }

                List<Map<String, String>> suggestedSlots = new ArrayList<>();
                LocalDateTime baseTime = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0);
                
                for (int day = 0; day < 7; day++) {
                    LocalDateTime slotTime = baseTime.plusDays(day);
                    Event testEvent = new Event("test", "", slotTime, "", 0, "", "", 1);
                    List<Event> conflicts = eventService.getConflictingEvents(testEvent, null);
                    
                    if (conflicts.isEmpty()) {
                        suggestedSlots.add(Map.of(
                            "date", slotTime.toLocalDate().toString(),
                            "time", slotTime.toLocalTime().toString(),
                            "duration", payload.durationMinutes.toString()
                        ));
                        if (suggestedSlots.size() >= 5) break;
                    }
                }

                sendJson(exchange, 200, Map.of(
                    "type", "suggested_slots",
                    "query", payload.title,
                    "slots", suggestedSlots
                ));
                return;
            }

            // ADMIN: Vérifier conflits /api/calendar/admin/conflicts
            if (path.equals("/api/calendar/admin/conflicts")) {
                EventPayload payload = fromJson(exchange, EventPayload.class);
                if (payload == null) {
                    sendError(exchange, 400, "Event data is required");
                    return;
                }

                Event event = payloadToEvent(payload);
                List<Event> conflicts = eventService.getConflictingEvents(event, payload.id);

                sendJson(exchange, 200, Map.of(
                    "type", "conflict_check",
                    "hasConflicts", !conflicts.isEmpty(),
                    "count", conflicts.size(),
                    "conflicts", conflicts.stream().map(this::toEventMap).toList()
                ));
                return;
            }

            // ADMIN: Envoyer rappels /api/calendar/admin/reminders/send
            if (path.equals("/api/calendar/admin/reminders/send")) {
                reminderService.sendAllReminders();
                sendJson(exchange, 200, Map.of(
                    "type", "reminders_sent",
                    "message", "Reminders sent successfully"
                ));
                return;
            }

            sendError(exchange, 404, "Route not found");
        }

        // ==================== HELPERS ====================

        private List<Map<String, String>> calculateAvailableSlots(LocalDate date, List<Event> events) {
            List<Map<String, String>> slots = new ArrayList<>();
            LocalDateTime current = date.atTime(9, 0);
            LocalDateTime endOfDay = date.atTime(18, 0);

            while (current.isBefore(endOfDay)) {
                LocalDateTime slotEnd = current.plusHours(1);
                final LocalDateTime finalCurrent = current;
                final LocalDateTime finalSlotEnd = slotEnd;
                boolean available = events.stream().noneMatch(e -> {
                    LocalDateTime eventStart = e.getDateEvent();
                    LocalDateTime eventEnd = eventStart.plusMinutes(e.getDurationMinutes());
                    return !(finalSlotEnd.isBefore(eventStart) || finalCurrent.isAfter(eventEnd));
                });

                if (available) {
                    slots.add(Map.of(
                        "start", current.toLocalTime().toString(),
                        "end", slotEnd.toLocalTime().toString(),
                        "available", "true"
                    ));
                }
                current = slotEnd;
            }

            return slots;
        }

        private Map<String, Object> toEventMap(Event event) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", event.getId());
            map.put("titre", event.getTitre());
            map.put("description", event.getDescription());
            map.put("dateEvent", event.getDateEvent() == null ? null : event.getDateEvent().toString());
            map.put("durationMinutes", event.getDurationMinutes());
            map.put("lieu", event.getLieu());
            map.put("capacite", event.getCapacite());
            map.put("categorie", event.getCategorie());
            map.put("status", event.getStatus());
            return map;
        }

        private Event payloadToEvent(EventPayload payload) {
            if (payload == null) {
                throw new IllegalArgumentException("Payload is required");
            }
            if (isBlank(payload.titre) || isBlank(payload.dateEvent) || isBlank(payload.lieu)) {
                throw new IllegalArgumentException("titre, dateEvent and lieu are required");
            }
            if (payload.capacite == null) {
                throw new IllegalArgumentException("capacite is required");
            }

            LocalDateTime dateTime = LocalDateTime.parse(payload.dateEvent);
            Event event = new Event(
                    payload.titre.trim(),
                    payload.description == null ? "" : payload.description.trim(),
                    dateTime,
                    payload.lieu.trim(),
                    payload.capacite,
                    payload.categorie,
                    payload.image,
                    payload.idUser
            );
            event.setDurationMinutes(payload.durationMinutes == null ? 60 : payload.durationMinutes);
            if (!isBlank(payload.status)) {
                event.setStatus(payload.status.trim().toUpperCase());
            }
            return event;
        }
    }

    private class EventsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isPreflight(exchange)) {
                sendEmpty(exchange, 204);
                return;
            }

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            EventRoute route = parseEventRoute(path);

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetEventRoutes(exchange, route);
                    return;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    handlePostEventRoutes(exchange, route);
                    return;
                }
                if ("PUT".equalsIgnoreCase(method)) {
                    handlePutEventRoutes(exchange, route);
                    return;
                }
                if ("PATCH".equalsIgnoreCase(method)) {
                    handlePatchEventRoutes(exchange, route);
                    return;
                }
                if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteEventRoutes(exchange, route);
                    return;
                }
                sendError(exchange, 405, "Method not allowed");
            } catch (JsonSyntaxException e) {
                sendError(exchange, 400, "Invalid JSON payload");
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("Conflit detecte")) {
                    sendError(exchange, 409, e.getMessage());
                    return;
                }
                sendError(exchange, 500, e.getMessage());
            }
        }

        private void handleGetEventRoutes(HttpExchange exchange, EventRoute route) throws SQLException, IOException {
            if (route.isRoot()) {
                String query = queryValue(exchange, "query");
                String sortBy = queryValue(exchange, "sortBy");
                List<Event> events = eventService.getEvents(query, sortBy);
                sendJson(exchange, 200, events.stream().map(this::toEventMap).toList());
                return;
            }
            if ("search".equals(route.keyword)) {
                List<Event> events = eventService.searchEvents(queryValue(exchange, "query"));
                sendJson(exchange, 200, events.stream().map(this::toEventMap).toList());
                return;
            }
            if ("filter".equals(route.keyword)) {
                String category = queryValue(exchange, "category");
                String date = queryValue(exchange, "date");
                String location = queryValue(exchange, "location");
                LocalDate localDate = isBlank(date) ? null : LocalDate.parse(date);
                List<Event> events = eventService.filterEvents(category, localDate, location);
                sendJson(exchange, 200, events.stream().map(this::toEventMap).toList());
                return;
            }
            if ("full".equals(route.keyword)) {
                List<Event> events = eventService.getFullEvents();
                sendJson(exchange, 200, events.stream().map(this::toEventMap).toList());
                return;
            }
            if ("upcoming".equals(route.keyword)) {
                List<Event> events = eventService.getUpcomingEvents();
                sendJson(exchange, 200, events.stream().map(this::toEventMap).toList());
                return;
            }
            if ("past".equals(route.keyword)) {
                List<Event> events = eventService.getPastEvents();
                sendJson(exchange, 200, events.stream().map(this::toEventMap).toList());
                return;
            }
            if (route.id != null && route.action == null) {
                Event event = eventService.getEventById(route.id);
                if (event == null) {
                    sendError(exchange, 404, "Event not found");
                    return;
                }
                sendJson(exchange, 200, toEventMap(event));
                return;
            }
            if (route.id != null && "participants".equals(route.action)) {
                List<ReservationRecord> participants = reservationService.getParticipantsByEvent(route.id);
                sendJson(exchange, 200, participants.stream().map(this::toReservationMap).toList());
                return;
            }
            if (route.id != null && "availability".equals(route.action)) {
                Event event = eventService.getEventById(route.id);
                if (event == null) {
                    sendError(exchange, 404, "Event not found");
                    return;
                }
                int reserved = reservationService.getReservationCountByEvent(route.id);
                int remaining = Math.max(event.getCapacite() - reserved, 0);
                sendJson(exchange, 200, Map.of(
                        "eventId", event.getId(),
                        "capacity", event.getCapacite(),
                        "reserved", reserved,
                        "remaining", remaining,
                        "full", remaining == 0
                ));
                return;
            }
            if (route.id != null && "conflicts".equals(route.action)) {
                Event target = eventService.getEventById(route.id);
                if (target == null) {
                    sendError(exchange, 404, "Event not found");
                    return;
                }
                List<Event> conflicts = eventService.getConflictingEvents(target, route.id);
                sendJson(exchange, 200, Map.of(
                        "eventId", route.id,
                        "hasConflict", !conflicts.isEmpty(),
                        "conflicts", conflicts.stream().map(this::toEventMap).toList()
                ));
                return;
            }
            sendError(exchange, 404, "Route not found");
        }

        private void handlePostEventRoutes(HttpExchange exchange, EventRoute route) throws SQLException, IOException {
            if (route.isRoot()) {
                EventPayload payload = fromJson(exchange, EventPayload.class);
                Event event = payloadToEvent(payload);
                eventService.addEvent(event);
                sendJson(exchange, 201, toEventMap(event));
                return;
            }
            if ("conflicts".equals(route.keyword) && "check".equals(route.action)) {
                EventPayload payload = fromJson(exchange, EventPayload.class);
                Event event = payloadToEvent(payload);
                Integer excludeId = payload == null ? null : payload.id;
                List<Event> conflicts = eventService.getConflictingEvents(event, excludeId);
                sendJson(exchange, 200, Map.of(
                        "hasConflict", !conflicts.isEmpty(),
                        "conflicts", conflicts.stream().map(this::toEventMap).toList()
                ));
                return;
            }
            if (route.id != null && ("join".equals(route.action) || "reserve".equals(route.action))) {
                UserActionPayload payload = fromJson(exchange, UserActionPayload.class);
                if (payload == null || payload.userId == null) {
                    sendError(exchange, 400, "userId is required");
                    return;
                }
                Event event = eventService.getEventById(route.id);
                if (event == null) {
                    sendError(exchange, 404, "Event not found");
                    return;
                }
                if ("CANCELLED".equalsIgnoreCase(event.getStatus())) {
                    sendError(exchange, 409, "Event is cancelled");
                    return;
                }
                reservationService.reserveEvent(event, payload.userId);
                reminderService.createDefaultReminderIfMissing(payload.userId, route.id);
                sendJson(exchange, 201, Map.of("message", "Participation confirmed"));
                return;
            }
            if (route.id != null && "leave".equals(route.action)) {
                UserActionPayload payload = fromJson(exchange, UserActionPayload.class);
                if (payload == null || payload.userId == null) {
                    sendError(exchange, 400, "userId is required");
                    return;
                }
                boolean removed = reservationService.leaveEvent(route.id, payload.userId);
                if (!removed) {
                    sendError(exchange, 404, "Participation not found");
                    return;
                }
                sendJson(exchange, 200, Map.of("message", "Participation cancelled"));
                return;
            }
            if (route.id != null && "reminders".equals(route.action)) {
                ReminderPayload payload = fromJson(exchange, ReminderPayload.class);
                if (payload == null || payload.userId == null) {
                    sendError(exchange, 400, "userId is required");
                    return;
                }
                int minutesBefore = payload.minutesBefore == null ? 30 : payload.minutesBefore;
                boolean enabled = payload.enabled == null || payload.enabled;
                reminderService.upsertReminder(payload.userId, route.id, minutesBefore, enabled);
                sendJson(exchange, 200, Map.of("message", "Reminder configured"));
                return;
            }
            sendError(exchange, 404, "Route not found");
        }

        private void handlePutEventRoutes(HttpExchange exchange, EventRoute route) throws SQLException, IOException {
            if (route.id == null || route.action != null) {
                sendError(exchange, 404, "Route not found");
                return;
            }
            EventPayload payload = fromJson(exchange, EventPayload.class);
            Event event = payloadToEvent(payload);
            event.setId(route.id);
            eventService.updateEvent(event);
            sendJson(exchange, 200, toEventMap(event));
        }

        private void handlePatchEventRoutes(HttpExchange exchange, EventRoute route) throws SQLException, IOException {
            if (route.id == null || route.action == null) {
                sendError(exchange, 404, "Route not found");
                return;
            }
            if ("cancel".equals(route.action)) {
                eventService.updateEventStatus(route.id, "CANCELLED");
                sendJson(exchange, 200, Map.of("message", "Event cancelled"));
                return;
            }
            if ("activate".equals(route.action)) {
                eventService.updateEventStatus(route.id, "ACTIVE");
                sendJson(exchange, 200, Map.of("message", "Event activated"));
                return;
            }
            sendError(exchange, 404, "Route not found");
        }

        private void handleDeleteEventRoutes(HttpExchange exchange, EventRoute route) throws SQLException, IOException {
            if (route.id == null || route.action != null) {
                sendError(exchange, 404, "Route not found");
                return;
            }
            reservationService.deleteReservationsForEvent(route.id);
            eventService.deleteEvent(route.id);
            sendEmpty(exchange, 204);
        }

        private Map<String, Object> toEventMap(Event event) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", event.getId());
            map.put("idUser", event.getIdUser());
            map.put("titre", event.getTitre());
            map.put("description", event.getDescription());
            map.put("dateEvent", event.getDateEvent() == null ? null : event.getDateEvent().toString());
            map.put("durationMinutes", event.getDurationMinutes());
            map.put("lieu", event.getLieu());
            map.put("capacite", event.getCapacite());
            map.put("categorie", event.getCategorie());
            map.put("image", event.getImage());
            map.put("title", event.getTitle());
            map.put("eventDate", event.getEventDate() == null ? null : event.getEventDate().toString());
            map.put("location", event.getLocation());
            map.put("status", event.getStatus());
            return map;
        }

        private Map<String, Object> toReservationMap(ReservationRecord reservation) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reservation.getId());
            map.put("eventId", reservation.getEventId());
            map.put("userId", reservation.getUserId());
            map.put("eventTitle", reservation.getEventTitle());
            map.put("username", reservation.getUsername());
            map.put("reservedAt", reservation.getReservedAt() == null ? null : reservation.getReservedAt().toString());
            return map;
        }

        private Event payloadToEvent(EventPayload payload) {
            if (payload == null) {
                throw new IllegalArgumentException("Payload is required");
            }
            if (isBlank(payload.titre) || isBlank(payload.description) || isBlank(payload.dateEvent) || isBlank(payload.lieu)) {
                throw new IllegalArgumentException("titre, description, dateEvent and lieu are required");
            }
            if (payload.capacite == null) {
                throw new IllegalArgumentException("capacite is required");
            }

            LocalDateTime dateTime = LocalDateTime.parse(payload.dateEvent);
            Event event = new Event(
                    payload.titre.trim(),
                    payload.description.trim(),
                    dateTime,
                    payload.lieu.trim(),
                    payload.capacite,
                    payload.categorie,
                    payload.image,
                    payload.idUser
            );
            if (!isBlank(payload.title)) {
                event.setTitle(payload.title.trim());
            }
            if (!isBlank(payload.location)) {
                event.setLocation(payload.location.trim());
            }
            if (!isBlank(payload.eventDate)) {
                event.setEventDate(LocalDateTime.parse(payload.eventDate));
            }
            if (!isBlank(payload.status)) {
                event.setStatus(payload.status.trim().toUpperCase());
            }
            event.setDurationMinutes(payload.durationMinutes == null ? 60 : payload.durationMinutes);
            return event;
        }
    }

    private class ReservationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isPreflight(exchange)) {
                sendEmpty(exchange, 204);
                return;
            }

            try {
                String method = exchange.getRequestMethod();
                if ("GET".equalsIgnoreCase(method)) {
                    List<ReservationRecord> reservations = reservationService.getAllReservations();
                    sendJson(exchange, 200, reservations.stream().map(this::toReservationMap).toList());
                    return;
                }

                if ("POST".equalsIgnoreCase(method)) {
                    ReservationPayload payload = fromJson(exchange, ReservationPayload.class);
                    if (payload == null || payload.eventId == null || payload.userId == null) {
                        sendError(exchange, 400, "eventId and userId are required");
                        return;
                    }
                    Event event = eventService.getEventById(payload.eventId);
                    if (event == null) {
                        sendError(exchange, 404, "Event not found");
                        return;
                    }
                    reservationService.reserveEvent(event, payload.userId);
                    reminderService.createDefaultReminderIfMissing(payload.userId, payload.eventId);
                    sendJson(exchange, 201, Map.of("message", "Reservation created"));
                    return;
                }

                sendError(exchange, 405, "Method not allowed");
            } catch (JsonSyntaxException e) {
                sendError(exchange, 400, "Invalid JSON payload");
            } catch (SQLException e) {
                sendError(exchange, 500, e.getMessage());
            }
        }

        private Map<String, Object> toReservationMap(ReservationRecord reservation) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reservation.getId());
            map.put("eventId", reservation.getEventId());
            map.put("userId", reservation.getUserId());
            map.put("eventTitle", reservation.getEventTitle());
            map.put("username", reservation.getUsername());
            map.put("reservedAt", reservation.getReservedAt() == null ? null : reservation.getReservedAt().toString());
            return map;
        }
    }

    private class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isPreflight(exchange)) {
                sendEmpty(exchange, 204);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                UserRoute route = parseUserRoute(exchange.getRequestURI().getPath());
                if (route == null) {
                    sendError(exchange, 404, "Route not found");
                    return;
                }
                if ("events".equals(route.action())) {
                    handleUserEvents(exchange, route.userId());
                    return;
                }
                if ("planning".equals(route.action())) {
                    handleUserPlanning(exchange, route.userId());
                    return;
                }
                if ("suggest-slots".equals(route.action())) {
                    handleSuggestSlots(exchange, route.userId());
                    return;
                }
                if ("reminders".equals(route.action())) {
                    handleUserReminders(exchange, route.userId());
                    return;
                }
                sendError(exchange, 404, "Route not found");
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (SQLException e) {
                sendError(exchange, 500, e.getMessage());
            }
        }

        private void handleUserEvents(HttpExchange exchange, int userId) throws SQLException, IOException {
            Set<Integer> reservedIds = reservationService.getReservedEventIdsByUser(userId);
            List<Map<String, Object>> userEvents = new ArrayList<>();
            for (Integer eventId : reservedIds) {
                Event event = eventService.getEventById(eventId);
                if (event != null) {
                    userEvents.add(toEventMap(event));
                }
            }
            sendJson(exchange, 200, userEvents);
        }

        private void handleUserPlanning(HttpExchange exchange, int userId) throws SQLException, IOException {
            String fromRaw = queryValue(exchange, "from");
            String toRaw = queryValue(exchange, "to");
            LocalDateTime from = isBlank(fromRaw) ? LocalDate.now().atStartOfDay() : LocalDateTime.parse(fromRaw);
            LocalDateTime to = isBlank(toRaw) ? from.plusDays(30) : LocalDateTime.parse(toRaw);
            List<Event> planning = eventService.getUserPlanning(userId, from, to);
            sendJson(exchange, 200, Map.of(
                    "userId", userId,
                    "from", from.toString(),
                    "to", to.toString(),
                    "events", planning.stream().map(this::toEventMap).toList()
            ));
        }

        private void handleSuggestSlots(HttpExchange exchange, int userId) throws SQLException, IOException {
            String dateRaw = queryValue(exchange, "date");
            String durationRaw = queryValue(exchange, "durationMinutes");
            String startHourRaw = queryValue(exchange, "startHour");
            String endHourRaw = queryValue(exchange, "endHour");

            LocalDate date = isBlank(dateRaw) ? LocalDate.now() : LocalDate.parse(dateRaw);
            int durationMinutes = parseIntOrDefault(durationRaw, 60);
            int startHour = parseIntOrDefault(startHourRaw, 8);
            int endHour = parseIntOrDefault(endHourRaw, 18);
            if (startHour < 0 || endHour > 23 || startHour >= endHour) {
                throw new IllegalArgumentException("Invalid startHour/endHour values");
            }

            LocalDateTime dayStart = date.atTime(startHour, 0);
            LocalDateTime dayEnd = date.atTime(endHour, 0);
            List<Event> planning = eventService.getUserPlanning(userId, dayStart, dayEnd);
            List<Map<String, Object>> slots = suggestSlots(planning, dayStart, dayEnd, durationMinutes);

            sendJson(exchange, 200, Map.of(
                    "userId", userId,
                    "date", date.toString(),
                    "durationMinutes", durationMinutes,
                    "slots", slots
            ));
        }

        private void handleUserReminders(HttpExchange exchange, int userId) throws SQLException, IOException {
            String mode = queryValue(exchange, "mode");
            if ("config".equalsIgnoreCase(mode)) {
                sendJson(exchange, 200, reminderService.getConfiguredRemindersByUser(userId));
                return;
            }
            int withinMinutes = parseIntOrDefault(queryValue(exchange, "withinMinutes"), 120);
            sendJson(exchange, 200, reminderService.getUserUpcomingReminders(userId, withinMinutes));
        }

        private List<Map<String, Object>> suggestSlots(List<Event> planning, LocalDateTime dayStart, LocalDateTime dayEnd, int durationMinutes) {
            List<Map<String, Object>> slots = new ArrayList<>();
            LocalDateTime cursor = dayStart;

            for (Event event : planning) {
                LocalDateTime eventStart = event.getDateEvent();
                LocalDateTime eventEnd = eventStart.plusMinutes(Math.max(1, event.getDurationMinutes()));
                if (eventEnd.isBefore(dayStart) || eventStart.isAfter(dayEnd)) {
                    continue;
                }
                LocalDateTime boundedStart = eventStart.isBefore(dayStart) ? dayStart : eventStart;
                LocalDateTime boundedEnd = eventEnd.isAfter(dayEnd) ? dayEnd : eventEnd;

                if (cursor.isBefore(boundedStart)) {
                    long freeMinutes = java.time.Duration.between(cursor, boundedStart).toMinutes();
                    if (freeMinutes >= durationMinutes) {
                        slots.add(slotMap(cursor, boundedStart, (int) freeMinutes));
                    }
                }
                if (cursor.isBefore(boundedEnd)) {
                    cursor = boundedEnd;
                }
            }

            if (cursor.isBefore(dayEnd)) {
                long freeMinutes = java.time.Duration.between(cursor, dayEnd).toMinutes();
                if (freeMinutes >= durationMinutes) {
                    slots.add(slotMap(cursor, dayEnd, (int) freeMinutes));
                }
            }
            return slots;
        }

        private Map<String, Object> slotMap(LocalDateTime start, LocalDateTime end, int duration) {
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("start", start.toString());
            slot.put("end", end.toString());
            slot.put("freeMinutes", duration);
            return slot;
        }

        private Map<String, Object> toEventMap(Event event) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", event.getId());
            map.put("titre", event.getTitre());
            map.put("dateEvent", event.getDateEvent() == null ? null : event.getDateEvent().toString());
            map.put("durationMinutes", event.getDurationMinutes());
            map.put("lieu", event.getLieu());
            map.put("categorie", event.getCategorie());
            map.put("status", event.getStatus());
            return map;
        }
    }

    private static boolean isPreflight(HttpExchange exchange) {
        return "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static String queryValue(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            int i = part.indexOf('=');
            if (i <= 0) {
                continue;
            }
            String key = part.substring(0, i);
            if (name.equals(key)) {
                return decode(part.substring(i + 1));
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static UserRoute parseUserRoute(String path) {
        String prefix = "/api/users/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        String[] parts = path.substring(prefix.length()).split("/");
        if (parts.length != 2) {
            return null;
        }
        Integer userId = tryParseInt(parts[0]);
        if (userId == null) {
            return null;
        }
        return new UserRoute(userId, parts[1]);
    }

    private static EventRoute parseEventRoute(String path) {
        String base = "/api/events";
        if (base.equals(path) || (base + "/").equals(path)) {
            return EventRoute.root();
        }
        if (!path.startsWith(base + "/")) {
            return EventRoute.invalid();
        }

        String[] parts = path.substring((base + "/").length()).split("/");
        if (parts.length == 0 || isBlank(parts[0])) {
            return EventRoute.root();
        }
        Integer id = tryParseInt(parts[0]);
        if (id != null) {
            String action = parts.length > 1 ? parts[1] : null;
            return new EventRoute(id, null, action);
        }
        String keyword = parts[0];
        String action = parts.length > 1 ? parts[1] : null;
        return new EventRoute(null, keyword, action);
    }

    private static Range toRange(String mode, LocalDate date) {
        if ("day".equals(mode)) {
            return new Range(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        if ("week".equals(mode)) {
            LocalDate monday = date.with(DayOfWeek.MONDAY);
            return new Range(monday.atStartOfDay(), monday.plusDays(7).atStartOfDay());
        }
        if ("month".equals(mode)) {
            LocalDate first = date.withDayOfMonth(1);
            return new Range(first.atStartOfDay(), first.plusMonths(1).atStartOfDay());
        }
        throw new IllegalArgumentException("Unsupported mode");
    }

    private static Integer tryParseInt(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseIntOrDefault(String value, int fallback) {
        Integer parsed = tryParseInt(value);
        return parsed == null ? fallback : parsed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> T fromJson(HttpExchange exchange, Class<T> type) throws IOException {
        String body = readBody(exchange);
        if (body.isBlank()) {
            return null;
        }
        return GSON.fromJson(body, type);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        byte[] data = inputStream.readAllBytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", message));
    }

    private static void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static class LoginRequest {
        private String username;
        private String password;
    }

    private static class EventPayload {
        private Integer id;
        private Integer idUser;
        private String titre;
        private String description;
        private String dateEvent;
        private Integer durationMinutes;
        private String lieu;
        private Integer capacite;
        private String categorie;
        private String image;
        private String title;
        private String eventDate;
        private String location;
        private String status;
    }

    private static class ReservationPayload {
        private Integer eventId;
        private Integer userId;
    }

    private static class UserActionPayload {
        private Integer userId;
    }

    private static class ReminderPayload {
        private Integer userId;
        private Integer minutesBefore;
        private Boolean enabled;
    }

    private static class SuggestSlotRequest {
        private String title;
        private Integer durationMinutes;
        private String preferredDate;
    }

    private record EventRoute(Integer id, String keyword, String action) {
        private static EventRoute root() {
            return new EventRoute(null, null, null);
        }

        private static EventRoute invalid() {
            return new EventRoute(null, "__invalid__", null);
        }

        private boolean isRoot() {
            return id == null && keyword == null && action == null;
        }
    }

    private record UserRoute(int userId, String action) {}
    private record Range(LocalDateTime start, LocalDateTime end) {}
}
