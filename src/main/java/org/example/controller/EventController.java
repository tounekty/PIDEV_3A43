package org.example.controller;

import org.example.model.Event;
import org.example.service.EventService;

import java.sql.SQLException;
import java.util.List;

public class EventController {
    private final EventService eventService = new EventService();

    public void createTableIfNotExists() throws SQLException {
        eventService.createTableIfNotExists();
    }

    public void addEvent(Event event) throws SQLException {
        eventService.addEvent(event);
    }

    public List<Event> getAllEvents() throws SQLException {
        return eventService.getAllEvents();
    }


    public List<Event> getEvents(String query, String sortBy) throws SQLException {
        return eventService.getEvents(query, sortBy);
    }

    public void updateEvent(Event event) throws SQLException {
        eventService.updateEvent(event);
    }

    public void deleteEvent(int id) throws SQLException {
        eventService.deleteEvent(id);
    }
}
