package org.example.repository;

import org.example.model.Event;

import java.sql.SQLException;
import java.util.List;

public interface EventRepository {
    void createTableIfNotExists() throws SQLException;
    
    void save(Event event) throws SQLException;
    
    void update(Event event) throws SQLException;
    
    void delete(int id) throws SQLException;
    
    List<Event> findAll() throws SQLException;
    
    List<Event> findByQuery(String query, String sortBy) throws SQLException;
    
    Event findById(int id) throws SQLException;
}
