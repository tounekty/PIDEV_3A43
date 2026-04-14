package org.example.service;

import org.example.model.Event;
import org.example.repository.EventRepository;
import org.example.repository.impl.EventRepositoryImpl;

import java.sql.SQLException;
import java.util.List;

public class EventService {
    private final EventRepository eventRepository;

    public EventService() {
        this.eventRepository = new EventRepositoryImpl();
    }

    public void createTableIfNotExists() throws SQLException {
        eventRepository.createTableIfNotExists();
    }

    public void addEvent(Event event) throws SQLException {
        if (event == null || event.getTitre() == null || event.getTitre().isBlank()) {
            throw new SQLException("Event title is required.");
        }
        eventRepository.save(event);
    }

    public List<Event> getAllEvents() throws SQLException {
        return eventRepository.findAll();
    }

    public List<Event> getEvents(String query, String sortBy) throws SQLException {
        return eventRepository.findByQuery(query, sortBy);
    }

    public void updateEvent(Event event) throws SQLException {
        if (event == null || event.getId() <= 0) {
            throw new SQLException("Valid event ID is required.");
        }
        eventRepository.update(event);
    }

    public void deleteEvent(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Valid event ID is required.");
        }
        eventRepository.delete(id);
    }
}
