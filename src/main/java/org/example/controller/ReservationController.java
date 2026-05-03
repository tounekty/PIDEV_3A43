package org.example.controller;

import org.example.model.Event;
import org.example.model.ReservationRecord;
import org.example.service.ReservationService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReservationController {
    private final ReservationService reservationService = new ReservationService();

    public void initializeReservations() throws SQLException {
        reservationService.initializeReservations();
    }

    public void reserveEvent(Event event, int userId) throws SQLException {
        reservationService.reserveEvent(event, userId);
    }

    public boolean hasReservation(int eventId, int userId) throws SQLException {
        return reservationService.hasReservation(eventId, userId);
    }

    public Set<Integer> getReservedEventIdsByUser(int userId) throws SQLException {
        return reservationService.getReservedEventIdsByUser(userId);
    }

    public Map<Integer, Integer> getReservationCountsByEvent() throws SQLException {
        return reservationService.getReservationCountsByEvent();
    }

    public int getReservationCountByEvent(int eventId) throws SQLException {
        return reservationService.getReservationCountByEvent(eventId);
    }

    public List<ReservationRecord> getAllReservations() throws SQLException {
        return reservationService.getAllReservations();
    }

    public Map<Integer, java.util.List<ReservationRecord>> getReservationsGroupedByEvent() throws SQLException {
        return reservationService.getReservationsGroupedByEvent();
    }

    public void deleteReservationsForEvent(int eventId) throws SQLException {
        reservationService.deleteReservationsForEvent(eventId);
    }

    public Map<String, Map<String, Integer>> getStatsByCategory() throws SQLException {
        return reservationService.getStatsByCategory();
    }

    public List<Map<String, Object>> getEventReservationStats() throws SQLException {
        return reservationService.getEventReservationStats();
    }

    public int getTotalReservations() throws SQLException {
        return reservationService.getTotalReservations();
    }

    public Map<String, Integer> getReservationCountByCategory() throws SQLException {
        return reservationService.getReservationCountByCategory();
    }

    public Map<String, Integer> getEventCountByCategory() throws SQLException {
        return reservationService.getEventCountByCategory();
    }
}
