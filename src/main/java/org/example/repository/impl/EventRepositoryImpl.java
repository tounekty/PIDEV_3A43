package org.example.repository.impl;

import org.example.model.Event;
import org.example.repository.EventRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EventRepositoryImpl implements EventRepository {

    @Override
    public void createTableIfNotExists() throws SQLException {
        // TODO: Implement database table creation
    }

    @Override
    public void save(Event event) throws SQLException {
        // TODO: Implement event save
    }

    @Override
    public void update(Event event) throws SQLException {
        // TODO: Implement event update
    }

    @Override
    public void delete(int id) throws SQLException {
        // TODO: Implement event delete
    }

    @Override
    public List<Event> findAll() throws SQLException {
        return new ArrayList<>();
    }

    @Override
    public List<Event> findByQuery(String query, String sortBy) throws SQLException {
        return new ArrayList<>();
    }

    @Override
    public Event findById(int id) throws SQLException {
        return null;
    }
}
