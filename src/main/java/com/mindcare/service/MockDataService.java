package com.mindcare.service;

import com.mindcare.model.Certificate;
import com.mindcare.model.Contract;
import com.mindcare.model.Conversation;
import com.mindcare.model.Message;
import com.mindcare.model.Offer;
import com.mindcare.model.ServiceRequest;
import com.mindcare.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal stub for the legacy-screen mock data dependency. The active
 * application uses real services (AuthController, AppointmentService, etc.)
 * for every navigable page, so this class only needs to provide empty
 * collections and no-op users so the legacy-screen sources still compile.
 */
public final class MockDataService {

    private static final MockDataService INSTANCE = new MockDataService();

    private MockDataService() { }

    public static MockDataService getInstance() {
        return INSTANCE;
    }

    public User getClientUser() {
        return null;
    }

    public User getPsychologueUser() {
        return null;
    }

    public User getAdminUser() {
        return null;
    }

    public List<Conversation> getConversations() {
        return Collections.emptyList();
    }

    public List<Message> getMessages(int conversationId) {
        return Collections.emptyList();
    }

    public List<Certificate> getCertificates() {
        return Collections.emptyList();
    }

    public List<Offer> getOffers() {
        return Collections.emptyList();
    }

    public List<Contract> getContracts() {
        return Collections.emptyList();
    }

    public List<ServiceRequest> getServiceRequests() {
        return Collections.emptyList();
    }

    public Map<String, Object> getWorkerStats() {
        return new HashMap<>();
    }
}
