package org.example.service;

import org.example.model.Event;
import org.example.model.ReservationRecord;
import org.example.repository.ReservationRepository;
import org.example.repository.impl.ReservationRepositoryImpl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReservationService {
    private final ReservationRepository reservationRepository;

    public ReservationService() {
        this.reservationRepository = new ReservationRepositoryImpl();
    }

    public void initializeReservations() throws SQLException {
        reservationRepository.createTableIfNotExists();
    }

    public void reserveEvent(Event event, int userId) throws SQLException {
        if (event == null || event.getId() <= 0) {
            throw new SQLException("Valid event is required.");
        }

        if (reservationRepository.existsByEventAndUser(event.getId(), userId)) {
            throw new SQLException("Vous avez deja reserve cet evenement.");
        }

        int currentReservations = reservationRepository.countByEventId(event.getId());
        if (currentReservations >= event.getCapacite()) {
            throw new SQLException("La capacite maximale de cet evenement est atteinte.");
        }

        reservationRepository.save(event.getId(), userId);
    }

    public boolean hasReservation(int eventId, int userId) throws SQLException {
        return reservationRepository.existsByEventAndUser(eventId, userId);
    }

    public Set<Integer> getReservedEventIdsByUser(int userId) throws SQLException {
        return reservationRepository.findReservedEventsByUser(userId);
    }

    public Map<Integer, Integer> getReservationCountsByEvent() throws SQLException {
        return reservationRepository.findReservationCounts();
    }

    public int getReservationCountByEvent(int eventId) throws SQLException {
        return reservationRepository.countByEventId(eventId);
    }

    public List<ReservationRecord> getAllReservations() throws SQLException {
        return reservationRepository.findAll();
    }

    public void deleteReservationsForEvent(int eventId) throws SQLException {
        reservationRepository.deleteByEventId(eventId);
    }

    public int getTotalReservations() throws SQLException {
        return reservationRepository.countTotal();
    }

    public Map<String, Integer> getReservationCountByCategory() throws SQLException {
        return reservationRepository.countByCategory();
    }

    public Map<String, Integer> getEventCountByCategory() throws SQLException {
        return reservationRepository.countEventsByCategory();
    }

    public Map<Integer, List<ReservationRecord>> getReservationsGroupedByEvent() throws SQLException {
        Map<Integer, List<ReservationRecord>> grouped = new java.util.LinkedHashMap<>();
        for (ReservationRecord reservation : getAllReservations()) {
            grouped.computeIfAbsent(reservation.getEventId(), ignored -> new java.util.ArrayList<>()).add(reservation);
        }
        return grouped;
    }

    public Map<String, Map<String, Integer>> getStatsByCategory() throws SQLException {
        Map<String, Map<String, Integer>> stats = new java.util.LinkedHashMap<>();
       
        return stats;
    }

    public List<Map<String, Object>> getEventReservationStats() throws SQLException {
        List<Map<String, Object>> stats = new java.util.ArrayList<>();
        
        return stats;
    }
}
