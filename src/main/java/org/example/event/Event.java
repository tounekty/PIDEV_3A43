package org.example.event;

import java.time.LocalDateTime;

public class Event {
    private int id;
    private Integer idUser;
    private String titre;
    private String description;
    private LocalDateTime dateEvent;
    private String lieu;
    private int capacite;
    private String categorie;
    private String image;
    private String title;
    private LocalDateTime eventDate;
    private String location;
    private String status;
    private int durationMinutes;

    public Event() {
    }

    public Event(String titre, String description, LocalDateTime dateEvent, String lieu,
                 int capacite, String categorie, String image, Integer idUser) {
        this.titre = titre;
        this.description = description;
        this.dateEvent = dateEvent;
        this.lieu = lieu;
        this.capacite = capacite;
        this.categorie = categorie;
        this.image = image;
        this.idUser = idUser;
        this.title = titre;
        this.eventDate = dateEvent;
        this.location = lieu;
        this.status = "ACTIVE";
        this.durationMinutes = 60;
    }

    public Event(int id, Integer idUser, String titre, String description, LocalDateTime dateEvent, String lieu,
                 int capacite, String categorie, String image, String title, LocalDateTime eventDate, String location) {
        this.id = id;
        this.idUser = idUser;
        this.titre = titre;
        this.description = description;
        this.dateEvent = dateEvent;
        this.lieu = lieu;
        this.capacite = capacite;
        this.categorie = categorie;
        this.image = image;
        this.title = title;
        this.eventDate = eventDate;
        this.location = location;
        this.status = "ACTIVE";
        this.durationMinutes = 60;
    }

    public Event(int id, Integer idUser, String titre, String description, LocalDateTime dateEvent, String lieu,
                 int capacite, String categorie, String image, String title, LocalDateTime eventDate, String location, String status) {
        this(id, idUser, titre, description, dateEvent, lieu, capacite, categorie, image, title, eventDate, location);
        this.status = status == null || status.isBlank() ? "ACTIVE" : status;
    }

    public Event(int id, Integer idUser, String titre, String description, LocalDateTime dateEvent, String lieu,
                 int capacite, String categorie, String image, String title, LocalDateTime eventDate, String location,
                 String status, int durationMinutes) {
        this(id, idUser, titre, description, dateEvent, lieu, capacite, categorie, image, title, eventDate, location, status);
        this.durationMinutes = durationMinutes <= 0 ? 60 : durationMinutes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateEvent() {
        return dateEvent;
    }

    public void setDateEvent(LocalDateTime dateEvent) {
        this.dateEvent = dateEvent;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
