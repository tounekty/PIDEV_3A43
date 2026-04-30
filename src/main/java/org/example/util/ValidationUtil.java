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
        if (title.trim().length() < 3) {
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
        if (description.trim().length() < 10) {
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
        if (location.trim().length() < 2) {
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
     * Valide un nom ou prénom (lettres, espaces, tirets, apostrophes)
     */
    public static void validateName(String name, String fieldLabel) throws IllegalArgumentException {
        if (isEmpty(name)) {
            throw new IllegalArgumentException(fieldLabel + " ne peut pas être vide");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException(fieldLabel + " doit contenir au moins 2 caractères");
        }
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(fieldLabel + " ne peut pas dépasser 100 caractères");
        }
        if (!trimmed.matches("[\\p{L}\\s'\\-]+")) {
            throw new IllegalArgumentException(fieldLabel + " ne doit contenir que des lettres");
        }
    }

    /**
     * Valide un numéro de téléphone (8 à 15 chiffres, peut commencer par +)
     */
    public static void validatePhone(String phone) throws IllegalArgumentException {
        if (isEmpty(phone)) {
            throw new IllegalArgumentException("Le numéro de téléphone ne peut pas être vide");
        }
        String digits = phone.trim().replaceAll("[\\s\\-\\.\\(\\)]", "");
        if (!digits.matches("\\+?[0-9]{8,15}")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres)");
        }
    }

    /**
     * Valide une adresse email
     */
    public static void validateEmail(String email) throws IllegalArgumentException {
        if (isEmpty(email)) {
            throw new IllegalArgumentException("L'adresse email ne peut pas être vide");
        }
        String trimmed = email.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("L'adresse email ne peut pas dépasser 255 caractères");
        }
        if (!trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Adresse email invalide (ex: nom@domaine.com)");
        }
    }

    /**
     * Valide un nom d'utilisateur (login)
     */
    public static void validateUsername(String username) throws IllegalArgumentException {
        if (isEmpty(username)) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas être vide");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("Le nom d'utilisateur doit contenir au moins 3 caractères");
        }
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas dépasser 50 caractères");
        }
        if (!trimmed.matches("[A-Za-z0-9_\\-\\.]+")) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut contenir que des lettres, chiffres, _, - ou .");
        }
    }

    /**
     * Valide un mot de passe
     */
    public static void validatePassword(String password) throws IllegalArgumentException {
        if (isEmpty(password)) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        if (password.length() < 4) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 4 caractères");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas dépasser 100 caractères");
        }
    }

    /**
     * Valide un commentaire (optionnel, longueur max)
     */
    public static void validateComment(String comment) throws IllegalArgumentException {
        if (comment != null && comment.length() > 2000) {
            throw new IllegalArgumentException("Le commentaire ne peut pas dépasser 2000 caractères");
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
