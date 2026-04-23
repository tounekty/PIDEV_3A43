package org.example.util;

import java.time.LocalDateTime;

/**
 * Classe utilitaire pour la validation des données
 */
public class ValidationUtil {

    /**
     * Vérifie si une chaîne est vide ou null
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Vérifie si une chaîne est valide (non-vide)
     */
    public static boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Valide un titre d'événement
     */
    public static void validateTitle(String title) throws IllegalArgumentException {
        if (isEmpty(title)) {
            throw new IllegalArgumentException("Le titre ne peut pas être vide");
        }
        if (title.length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 255 caractères");
        }
    }

    /**
     * Valide une description
     */
    public static void validateDescription(String description) throws IllegalArgumentException {
        if (isEmpty(description)) {
            throw new IllegalArgumentException("La description ne peut pas être vide");
        }
        if (description.length() < 10) {
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères");
        }
        if (description.length() > 5000) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 5000 caractères");
        }
    }

    /**
     * Valide un lieu
     */
    public static void validateLocation(String location) throws IllegalArgumentException {
        if (isEmpty(location)) {
            throw new IllegalArgumentException("Le lieu ne peut pas être vide");
        }
        if (location.length() < 2) {
            throw new IllegalArgumentException("Le lieu doit contenir au moins 2 caractères");
        }
        if (location.length() > 255) {
            throw new IllegalArgumentException("Le lieu ne peut pas dépasser 255 caractères");
        }
    }

    /**
     * Valide la capacité d'un événement
     */
    public static void validateCapacity(int capacity) throws IllegalArgumentException {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacité doit être supérieure à 0");
        }
        if (capacity > 100000) {
            throw new IllegalArgumentException("La capacité ne peut pas dépasser 100000");
        }
    }

    /**
     * Valide la date d'un événement
     */
    public static void validateEventDate(LocalDateTime date) throws IllegalArgumentException {
        if (date == null) {
            throw new IllegalArgumentException("La date de l'événement ne peut pas être vide");
        }
        if (date.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de l'événement doit être dans le futur");
        }
    }

    /**
     * Valide une catégorie (optionnelle mais avec contraintes)
     */
    public static void validateCategory(String category) throws IllegalArgumentException {
        if (category != null && !category.isEmpty()) {
            if (category.length() > 100) {
                throw new IllegalArgumentException("La catégorie ne peut pas dépasser 100 caractères");
            }
        }
    }

    /**
     * Valide un ID d'utilisateur
     */
    public static void validateUserId(Integer userId) throws IllegalArgumentException {
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur doit être positif");
        }
    }

    /**
     * Valide un ID d'événement
     */
    public static void validateEventId(int eventId) throws IllegalArgumentException {
        if (eventId <= 0) {
            throw new IllegalArgumentException("L'ID d'événement doit être positif");
        }
    }

    /**
     * Valide complètement un événement avant ajout/modification
     */
    public static void validateEventComplete(String titre, String description, LocalDateTime dateEvent,
                                            String lieu, int capacite, String categorie, Integer idUser) 
            throws IllegalArgumentException {
        validateTitle(titre);
        validateDescription(description);
        validateEventDate(dateEvent);
        validateLocation(lieu);
        validateCapacity(capacite);
        validateCategory(categorie);
        validateUserId(idUser);
    }

    /**
     * Valide une réservation
     */
    public static void validateReservation(int eventId, int userId) throws IllegalArgumentException {
        validateEventId(eventId);
        if (userId <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur doit être positif");
        }
    }

    /**
     * Formate un message d'erreur pour l'affichage
     */
    public static String formatErrorMessage(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        return "Une erreur s'est produite : " + e.getMessage();
    }
}
