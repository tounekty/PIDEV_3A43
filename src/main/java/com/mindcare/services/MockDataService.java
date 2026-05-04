package com.mindcare.service;

import com.mindcare.model.*;
import java.util.*;

/**
 * MockDataService – provides sample data for all modules.
 * Replace these methods with real API calls when the backend is ready.
 */
public class MockDataService {

    private static MockDataService instance;

    private MockDataService() {}

    public static MockDataService getInstance() {
        if (instance == null) instance = new MockDataService();
        return instance;
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        users.add(makeUser(1, "Alice",   "Martin",  "alice@mindcare.io",  User.Role.CLIENT));
        users.add(makeUser(2, "Bob",     "Chen",    "bob@mindcare.io",    User.Role.PSYCHOLOGUE));
        users.add(makeUser(3, "Clara",   "Dubois",  "clara@mindcare.io",  User.Role.PSYCHOLOGUE));
        users.add(makeUser(4, "David",   "Smith",   "david@mindcare.io",  User.Role.CLIENT));
        users.add(makeUser(5, "Emma",    "Johnson", "emma@mindcare.io",   User.Role.PSYCHOLOGUE));
        users.add(makeUser(6, "Frank",   "Müller",  "frank@mindcare.io",  User.Role.CLIENT));
        users.add(makeUser(7, "Grace",   "Lee",     "grace@mindcare.io",  User.Role.PSYCHOLOGUE));
        users.add(makeUser(8, "Henry",   "Brown",   "henry@mindcare.io",  User.Role.ADMIN));
        return users;
    }

    private User makeUser(int id, String first, String last, String email, User.Role role) {
        User u = new User(id, first, last, email, role);
        u.setCreatedAt("2025-0" + id + "-15");
        u.setPhone("+1 555-" + (1000 + id * 111));
        u.setLocation("Paris, France");
        u.setBio("Experienced professional on the MindCare platform.");
        return u;
    }

    public User getClientUser()  { return makeUser(1, "Alice",  "Martin", "alice@mindcare.io",  User.Role.CLIENT); }
    public User getPsychologueUser()  { return makeUser(2, "Bob",    "Chen",   "bob@mindcare.io",    User.Role.PSYCHOLOGUE); }
    public User getAdminUser()   { return makeUser(8, "Henry",  "Brown",  "henry@mindcare.io",  User.Role.ADMIN); }

    // ─── Service Requests ─────────────────────────────────────────────────────

    public List<ServiceRequest> getServiceRequests() {
        List<ServiceRequest> list = new ArrayList<>();
        list.add(sr(1, "Build a React Dashboard",   "Web Development",    1500, "2026-04-01", ServiceRequest.Status.OPEN,        "Alice Martin", 3));
        list.add(sr(2, "Mobile App UI Design",       "UI/UX Design",        800, "2026-04-15", ServiceRequest.Status.IN_PROGRESS, "Alice Martin", 5));
        list.add(sr(3, "API Integration Project",    "Backend Development", 2200, "2026-05-01", ServiceRequest.Status.OPEN,        "David Smith",  2));
        list.add(sr(4, "SEO Content Writing",        "Content Creation",    400, "2026-04-10", ServiceRequest.Status.COMPLETED,   "Frank Müller", 4));
        list.add(sr(5, "Machine Learning Model",     "Data Science",       3500, "2026-06-01", ServiceRequest.Status.OPEN,        "Alice Martin", 1));
        list.add(sr(6, "WordPress Landing Page",     "Web Development",    600,  "2026-04-20", ServiceRequest.Status.IN_PROGRESS, "David Smith",  3));
        list.add(sr(7, "Logo & Brand Identity",      "Design",             750,  "2026-04-08", ServiceRequest.Status.OPEN,        "Frank Müller", 6));
        return list;
    }

    private ServiceRequest sr(int id, String title, String cat, double budget,
                              String deadline, ServiceRequest.Status status, String client, int offers) {
        ServiceRequest r = new ServiceRequest(id, title, cat, budget, deadline, status, client);
        r.setCreatedAt("2026-03-0" + id);
        r.setOffersCount(offers);
        r.setDescription("Detailed description for: " + title + ". Looking for experienced professionals.");
        return r;
    }

    // ─── Offers ───────────────────────────────────────────────────────────────

    public List<Offer> getOffers() {
        List<Offer> list = new ArrayList<>();
        list.add(offer(1, "Build a React Dashboard",   "Bob Chen",     1200, "10 days",  Offer.Status.PENDING));
        list.add(offer(2, "Mobile App UI Design",       "Clara Dubois",  750, "7 days",   Offer.Status.ACCEPTED));
        list.add(offer(3, "API Integration Project",    "Bob Chen",     2000, "21 days",  Offer.Status.PENDING));
        list.add(offer(4, "SEO Content Writing",        "Emma Johnson",  380, "5 days",   Offer.Status.REJECTED));
        list.add(offer(5, "Machine Learning Model",     "Grace Lee",    3200, "45 days",  Offer.Status.PENDING));
        list.add(offer(6, "Build a React Dashboard",   "Grace Lee",    1400, "14 days",  Offer.Status.PENDING));
        return list;
    }

    private Offer offer(int id, String reqTitle, String worker, double price, String delivery, Offer.Status status) {
        Offer o = new Offer(id, reqTitle, worker, price, delivery, status);
        o.setCoverLetter("I am highly experienced in this field and ready to deliver quality work on time.");
        o.setCreatedAt("2026-03-0" + id);
        return o;
    }

    // ─── Contracts ────────────────────────────────────────────────────────────

    public List<Contract> getContracts() {
        List<Contract> list = new ArrayList<>();
        list.add(contract(1, "Mobile App UI Design",    "Alice Martin", "Clara Dubois", 750,  "2026-03-01", "2026-03-31", Contract.Status.ACTIVE,    75));
        list.add(contract(2, "SEO Content Writing",     "Frank Müller", "Emma Johnson", 380,  "2026-02-15", "2026-03-15", Contract.Status.COMPLETED, 100));
        list.add(contract(3, "WordPress Landing Page",  "David Smith",  "Bob Chen",     600,  "2026-03-05", "2026-04-05", Contract.Status.ACTIVE,    40));
        list.add(contract(4, "Logo & Brand Identity",   "Frank Müller", "Grace Lee",    750,  "2026-03-08", "2026-04-08", Contract.Status.ACTIVE,    20));
        list.add(contract(5, "API Security Audit",      "Alice Martin", "Bob Chen",    1500,  "2026-02-01", "2026-02-28", Contract.Status.DISPUTED,   60));
        return list;
    }

    private Contract contract(int id, String title, String client, String worker,
                              double amount, String start, String end, Contract.Status status, int progress) {
        Contract c = new Contract(id, title, client, worker, amount, start, end, status);
        c.setProgress(progress);
        return c;
    }

    // ─── Tickets ──────────────────────────────────────────────────────────────

    public List<Ticket> getTickets() {
        List<Ticket> list = new ArrayList<>();
        list.add(ticket(1, "Cannot accept payment",       "Alice Martin", Ticket.Status.OPEN,        Ticket.Priority.HIGH,   "2026-03-10"));
        list.add(ticket(2, "Profile picture not uploading","Bob Chen",    Ticket.Status.IN_PROGRESS,  Ticket.Priority.MEDIUM, "2026-03-09"));
        list.add(ticket(3, "Contract dispute question",   "David Smith",  Ticket.Status.RESOLVED,    Ticket.Priority.HIGH,   "2026-03-08"));
        list.add(ticket(4, "2FA is not working",          "Clara Dubois", Ticket.Status.OPEN,        Ticket.Priority.URGENT, "2026-03-11"));
        list.add(ticket(5, "How to withdraw funds?",       "Emma Johnson", Ticket.Status.CLOSED,      Ticket.Priority.LOW,    "2026-03-07"));
        return list;
    }

    private Ticket ticket(int id, String subject, String user, Ticket.Status status, Ticket.Priority priority, String date) {
        Ticket t = new Ticket(id, subject, user, status, priority, date);
        t.setDescription("Detailed description of the issue. Needs urgent attention.");
        t.setUpdatedAt(date);
        return t;
    }

    // ─── Certificates ─────────────────────────────────────────────────────────

    public List<Certificate> getCertificates() {
        List<Certificate> list = new ArrayList<>();
        list.add(cert(1, "Bob Chen",    "AWS Solutions Architect",        "Amazon",      "2023-05-01", Certificate.Status.APPROVED));
        list.add(cert(2, "Clara Dubois","Adobe Certified Expert",         "Adobe",       "2024-01-15", Certificate.Status.PENDING));
        list.add(cert(3, "Emma Johnson","Google Analytics Certification", "Google",      "2024-03-01", Certificate.Status.APPROVED));
        list.add(cert(4, "Grace Lee",   "TensorFlow Developer Cert.",     "Google",      "2025-01-10", Certificate.Status.PENDING));
        list.add(cert(5, "Bob Chen",    "Kubernetes Administrator",       "CNCF",        "2024-08-20", Certificate.Status.REJECTED));
        return list;
    }

    private Certificate cert(int id, String worker, String name, String issuer, String issued, Certificate.Status status) {
        Certificate c = new Certificate(id, worker, name, issuer, issued, status);
        c.setUploadedAt("2026-03-0" + id);
        c.setAiAnalysis("AI Analysis: Certificate appears authentic. Issuer verified. Recommended for approval.");
        return c;
    }

    // ─── Messages ─────────────────────────────────────────────────────────────

    public List<Conversation> getConversations() {
        List<Conversation> list = new ArrayList<>();
        list.add(new Conversation(1, "Mobile App UI Design",   "Clara Dubois", "Great! I'll start on Monday.",  "10:32", 2));
        list.add(new Conversation(2, "WordPress Landing Page", "Bob Chen",     "Can we schedule a call?",       "Yesterday", 0));
        list.add(new Conversation(3, "Logo & Brand Identity",  "Grace Lee",    "I've sent the first draft.",    "Mon", 1));
        return list;
    }

    public List<Message> getMessages(int conversationId) {
        List<Message> list = new ArrayList<>();
        list.add(new Message(1, 1, "Alice Martin", "Hi! When can you start the project?",                   "10:15", true));
        list.add(new Message(2, 2, "Clara Dubois", "Hello! I'm ready to begin. Can we discuss details?",    "10:18", false));
        list.add(new Message(3, 1, "Alice Martin", "Sure! I'll send the brief document shortly.",            "10:22", true));
        list.add(new Message(4, 2, "Clara Dubois", "Perfect. Looking forward to it.",                        "10:25", false));
        list.add(new Message(5, 1, "Alice Martin", "Brief sent! Great! I'll start on Monday.",               "10:30", true));
        list.add(new Message(6, 2, "Clara Dubois", "Great! I'll start on Monday.",                           "10:32", false));
        return list;
    }

    // ─── Admin Dashboard Stats ────────────────────────────────────────────────

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers",       getAllUsers().size());
        stats.put("activeContracts",  getContracts().stream().filter(c -> c.getStatus() == Contract.Status.ACTIVE).count());
        stats.put("openTickets",      getTickets().stream().filter(t -> t.getStatus() == Ticket.Status.OPEN || t.getStatus() == Ticket.Status.IN_PROGRESS).count());
        stats.put("serviceRequests",  getServiceRequests().size());
        stats.put("pendingCerts",     getCertificates().stream().filter(c -> c.getStatus() == Certificate.Status.PENDING).count());
        stats.put("totalRevenue",     "€ 18,340");
        return stats;
    }

    public Map<String, Object> getClientStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeRequests",   3);
        stats.put("receivedOffers",   6);
        stats.put("activeContracts",  2);
        stats.put("openTickets",      1);
        return stats;
    }

    public Map<String, Object> getWorkerStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeOffers",     3);
        stats.put("activeContracts",  2);
        stats.put("certificates",     2);
        stats.put("openTickets",      1);
        return stats;
    }
}
