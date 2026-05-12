package org.example.repository;

import org.example.model.ReservationRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReservationRepository {
    void createTableIfNotExists() throws SQLException;
    
    void save(int eventId, int userId) throws SQLException;
    
    void deleteByEventId(int eventId) throws SQLException;
    
    boolean existsByEventAndUser(int eventId, int userId) throws SQLException;
    
    Set<Integer> findReservedEventsByUser(int userId) throws SQLException;
    
    Map<Integer, Integer> findReservationCounts() throws SQLException;
    
    int countByEventId(int eventId) throws SQLException;
    
    List<ReservationRecord> findAll() throws SQLException;
    
    int countTotal() throws SQLException;
    
    Map<String, Integer> countByCategory() throws SQLException;
    
    Map<String, Integer> countEventsByCategory() throws SQLException;
}
