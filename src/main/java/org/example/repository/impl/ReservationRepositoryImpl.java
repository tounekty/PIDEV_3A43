package org.example.repository.impl;

import org.example.model.ReservationRecord;
import org.example.repository.ReservationRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReservationRepositoryImpl implements ReservationRepository {

    @Override
    public void createTableIfNotExists() throws SQLException {
        // TODO: Implement
    }

    @Override
    public void save(int eventId, int userId) throws SQLException {
        // TODO: Implement
    }

    @Override
    public void deleteByEventId(int eventId) throws SQLException {
        // TODO: Implement
    }

    @Override
    public boolean existsByEventAndUser(int eventId, int userId) throws SQLException {
        return false;
    }

    @Override
    public Set<Integer> findReservedEventsByUser(int userId) throws SQLException {
        return new HashSet<>();
    }

    @Override
    public Map<Integer, Integer> findReservationCounts() throws SQLException {
        return new HashMap<>();
    }

    @Override
    public int countByEventId(int eventId) throws SQLException {
        return 0;
    }

    @Override
    public List<ReservationRecord> findAll() throws SQLException {
        return new ArrayList<>();
    }

    @Override
    public int countTotal() throws SQLException {
        return 0;
    }

    @Override
    public Map<String, Integer> countByCategory() throws SQLException {
        return new HashMap<>();
    }

    @Override
    public Map<String, Integer> countEventsByCategory() throws SQLException {
        return new HashMap<>();
    }
}
