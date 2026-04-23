# Documentation du Contrôle de Saisie (Validation)

## Vue d'ensemble
Un système complet de validation a été implémenté pour garantir l'intégrité des données lors de l'ajout et la modification d'événements, ainsi que lors des réservations.

## Classe ValidationUtil

La classe `ValidationUtil` contient toutes les méthodes de validation centralisées.

### Validations pour les Événements

#### 1. **Validation du Titre** (`validateTitle`)
- **Règles:**
  - Obligatoire (non-vide)
  - Minimum 3 caractères
  - Maximum 255 caractères
- **Message d'erreur:** "Le titre doit contenir au moins 3 caractères"

#### 2. **Validation de la Description** (`validateDescription`)
- **Règles:**
  - Obligatoire (non-vide)
  - Minimum 10 caractères
  - Maximum 5000 caractères
- **Message d'erreur:** "La description doit contenir au moins 10 caractères"

#### 3. **Validation du Lieu** (`validateLocation`)
- **Règles:**
  - Obligatoire (non-vide)
  - Minimum 2 caractères
  - Maximum 255 caractères
- **Message d'erreur:** "Le lieu doit contenir au moins 2 caractères"

#### 4. **Validation de la Capacité** (`validateCapacity`)
- **Règles:**
  - Doit être supérieure à 0
  - Maximum 100000
- **Message d'erreur:** "La capacité doit être supérieure à 0"

#### 5. **Validation de la Date** (`validateEventDate`)
- **Règles:**
  - Obligatoire
  - Doit être dans le futur
- **Message d'erreur:** "La date de l'événement doit être dans le futur"

#### 6. **Validation de la Catégorie** (`validateCategory`)
- **Règles:**
  - Optionnelle
  - Si fournie, maximum 100 caractères
- **Message d'erreur:** "La catégorie ne peut pas dépasser 100 caractères"

#### 7. **Validation Complète** (`validateEventComplete`)
Exécute toutes les validations ci-dessus en une seule appel.

### Validations pour les Réservations

#### **Validation de Réservation** (`validateReservation`)
- **Vérifie:**
  - Event ID positif
  - User ID positif
- **Messages d'erreur:**
  - "L'ID d'événement doit être positif"
  - "L'ID utilisateur doit être positif"

### Validations Utilitaires
- `isEmpty(String)` - Vérifie si une chaîne est vide ou null
- `isValid(String)` - Vérifie si une chaîne est valide
- `validateUserId(Integer)` - Valide un ID utilisateur
- `validateEventId(int)` - Valide un ID d'événement

## Intégration dans les Services

### EventService - Ajout d'Événement
```java
public void addEvent(Event event) throws SQLException {
    // Validation automatique avant insertion
    try {
        ValidationUtil.validateEventComplete(
            event.getTitre(),
            event.getDescription(),
            event.getDateEvent(),
            event.getLieu(),
            event.getCapacite(),
            event.getCategorie(),
            event.getIdUser()
        );
    } catch (IllegalArgumentException e) {
        throw new SQLException("Erreur de validation : " + e.getMessage(), e);
    }
    // ... insertion dans la base de données
}
```

### EventService - Modification d'Événement
La même validation s'applique à la modification d'événements via `updateEvent()`.

### ReservationService - Réservation d'Événement
```java
public void reserveEvent(Event event, int userId) throws SQLException {
    // Validation des données
    try {
        ValidationUtil.validateReservation(event.getId(), userId);
    } catch (IllegalArgumentException e) {
        throw new SQLException("Erreur de validation : " + e.getMessage(), e);
    }
    
    // Vérifications métier existantes:
    // - Double réservation
    // - Dépassement de capacité
}
```

## Gestion des Erreurs

### Exceptions
Les erreurs de validation sont lancées en tant que `IllegalArgumentException` et converties en `SQLException` avec le message "Erreur de validation: [message détaillé]"

### Messages d'Erreur Formatés
La méthode `formatErrorMessage()` normalise l'affichage des erreurs pour l'interface utilisateur.

## Avantages du Système

1. **Centralisation:** Toutes les validations au même endroit
2. **Réutilisabilité:** Les mêmes méthodes utilisées dans add/update/delete
3. **Maintenabilité:** Modifications faciles en un seul endroit
4. **Clarté:** Messages d'erreur explicites en français
5. **Sécurité:** Prévention des données invalides
6. **Performance:** Validation avant les opérations BD coûteuses

## Exemple d'Utilisation

```java
try {
    Event event = new Event(
        "Concert Jazz",           // titre (min 3 chars)
        "Un magnifique concert de jazz avec des musiciens internationaux",  // description (min 10 chars)
        LocalDateTime.now().plusDays(7),  // date (futur)
        "Salle de Concerts ABC",  // lieu (min 2 chars)
        100,                       // capacité (> 0)
        "Musique",                // catégorie (optionnel)
        "concert.jpg",
        userId
    );
    
    eventService.addEvent(event);
    // Si les données sont valides, l'événement est ajouté
} catch (SQLException e) {
    // Afficher le message d'erreur à l'utilisateur
    System.out.println(e.getMessage());
}
```

## Résumé des Contrôles de Saisie Ajoutés

| Domaine | Contrôles | Où |
|---------|-----------|-----|
| **Événement - Ajout** | Titre, Description, Lieu, Capacité, Date, Catégorie | addEvent() |
| **Événement - Modification** | Titre, Description, Lieu, Capacité, Date, Catégorie | updateEvent() |
| **Réservation** | Event ID, User ID | reserveEvent() |

Le système est maintenant prêt à rejeter les données invalides avant qu'elles ne soient stockées en base de données.
