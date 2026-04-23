# Guide du Système de Calendrier Interactif Avancé

## 📋 Vue d'ensemble

Le système de calendrier interactif avancé fournit une interface visuelle pour la gestion d'événements et de réservations avec des codes couleur pour la disponibilité.

## 🎨 Composants principaux

### 1. CalendarPicker (`org.example.util.CalendarPicker`)
Composant JavaFX personnalisé affichant un calendrier interactif.

**Caractéristiques :**
- Navigation entre les mois (précédent/suivant)
- Bouton "Aujourd'hui" pour revenir au mois actuel
- Sélection de date avec mise en évidence
- Indicateurs visuels de disponibilité par couleur
- Support pour les callbacks de sélection
- Effets de survol et animations

### 2. CalendarDialog (`org.example.util.CalendarDialog`)
Dialogue modale affichant le CalendarPicker pour la sélection de date.

### 3. CalendarReservationController (`org.example.util.CalendarReservationController`)
Contrôleur pour intégrer le calendrier dans les dialogues de réservation.

### 4. AdvancedCalendarController (`org.example.util.AdvancedCalendarController`)
Contrôleur avancé avec fonctionnalités étendues :
- Filtres par type d'événement
- Statistiques de disponibilité
- Affichage des événements par date
- Panneau d'informations détaillées

### 5. AdvancedReservationDialog (`org.example.util.AdvancedReservationDialog`)
Exemple d'utilisation du calendrier dans une réservation avancée.

### 6. CalendarExampleApp (`org.example.util.CalendarExampleApp`)
Application de démonstration complète montrant toutes les fonctionnalités.

## 🎯 Codes Couleur de Disponibilité

| Couleur | Signification | Pourcentage de disponibilité |
|---------|---------------|------------------------------|
| 🟢 **Vert** | Disponible | 100% des places disponibles |
| 🔵 **Cyan** | Beaucoup d'options | 25-99% des places disponibles |
| 🟠 **Orange** | Peu d'options | 1-25% des places disponibles |
| ⚫ **Gris** | Complet | 0% des places disponibles |

## 🚀 Utilisation de base

### Intégration simple du CalendarPicker

```java
// Créer un CalendarPicker avec callback
CalendarPicker calendar = new CalendarPicker(date -> {
    System.out.println("Date sélectionnée: " + date);
    // Traiter la date sélectionnée
});

// Définir des disponibilités
calendar.setDateAvailability(LocalDate.now().plusDays(1), 0); // Peu d'options
calendar.setDateAvailability(LocalDate.now().plusDays(2), 1); // Beaucoup d'options
calendar.setDateAvailability(LocalDate.now().plusDays(3), 2); // Complet
calendar.setDateAvailability(LocalDate.now().plusDays(4), 3); // Disponible

// Ajouter à votre interface
VBox container = new VBox();
container.getChildren().add(calendar);
```

### Utilisation du CalendarDialog

```java
// Créer et afficher le dialogue
CalendarDialog dialog = new CalendarDialog();
dialog.setDateAvailabilities(availabilityMap); // Map<LocalDate, Integer>

Optional<LocalDate> result = dialog.showAndWait();
result.ifPresent(date -> {
    // Traiter la date sélectionnée
});
```

### Intégration avec le système de réservation

```java
// Créer le contrôleur de réservation avec calendrier
CalendarReservationController controller = new CalendarReservationController(eventService);

// Obtenir le panneau de sélection de date
VBox datePanel = controller.createDateSelectionPanel();

// La date sélectionnée est disponible via
LocalDate selectedDate = controller.getSelectedDate();
```

## 🔧 Fonctionnalités avancées

### AdvancedCalendarController

```java
// Créer un contrôleur de calendrier avancé
AdvancedCalendarController advancedController = new AdvancedCalendarController(
    eventService, 
    reservationService
);

// Créer le panneau complet
VBox calendarPanel = advancedController.createAdvancedCalendarPanel();

// Définir un callback pour la sélection
advancedController.setOnDateSelected(date -> {
    System.out.println("Date sélectionnée: " + date);
    List<Event> events = advancedController.getEventsForDate(date);
    // Traiter les événements de cette date
});

// Obtenir le CalendarPicker sous-jacent
CalendarPicker picker = advancedController.getCalendarPicker();
```

### Filtres disponibles

Le `AdvancedCalendarController` propose plusieurs filtres :
- Tous les événements
- Événements disponibles
- Événements complets
- Événements à venir
- Événements passés

### Statistiques en temps réel

Le contrôleur affiche automatiquement :
- Nombre total d'événements
- Événements avec places disponibles
- Événements complets
- Détails des événements par date sélectionnée

## 🎨 Personnalisation

### Styles CSS

Le système utilise deux fichiers CSS :
- `styles.css` - Styles généraux
- `calendar-styles.css` - Styles spécifiques au calendrier

**Classes CSS disponibles :**
- `.calendar-date-button` - Boutons de date
- `.availability-low` - Peu d'options (orange)
- `.availability-medium` - Beaucoup d'options (cyan)
- `.availability-high` - Disponible (vert)
- `.availability-full` - Complet (gris)
- `.date-selected` - Date sélectionnée
- `.date-today` - Date d'aujourd'hui

### Configuration des couleurs

Modifiez les dégradés dans `calendar-styles.css` :

```css
.availability-low {
    -fx-background-color: linear-gradient(to bottom, #FF9800, #F57C00);
}

.availability-medium {
    -fx-background-color: linear-gradient(to bottom, #00BCD4, #0097A7);
}

.availability-high {
    -fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C);
}
```

## 📊 Intégration avec les services existants

### Avec EventService

```java
// Charger les événements et calculer la disponibilité
List<Event> events = eventService.getAllEvents();
Map<LocalDate, Integer> availabilityMap = new HashMap<>();

for (Event event : events) {
    LocalDate eventDate = event.getDateEvent().toLocalDate();
    int availableSpots = event.getCapacite() - 
                        reservationService.getReservationCountByEvent(event.getId());
    
    // Déterminer le niveau de disponibilité
    int availabilityLevel = calculateAvailabilityLevel(availableSpots, event.getCapacite());
    availabilityMap.put(eventDate, availabilityLevel);
}

// Appliquer au calendrier
calendarPicker.setDateAvailabilities(availabilityMap);
```

### Méthode de calcul de disponibilité

```java
private int calculateAvailabilityLevel(int availableSpots, int totalCapacity) {
    if (availableSpots <= 0) {
        return 2; // Complet
    } else if (availableSpots < totalCapacity * 0.25) {
        return 0; // Peu d'options
    } else if (availableSpots < totalCapacity) {
        return 1; // Beaucoup d'options
    } else {
        return 3; // Disponible (100%)
    }
}
```

## 🧪 Exemples d'utilisation

### 1. Dans un dialogue de réservation

```java
public class ReservationDialog {
    private CalendarReservationController calendarController;
    
    public ReservationDialog(EventService eventService) {
        calendarController = new CalendarReservationController(eventService);
    }
    
    public VBox createDialogContent() {
        VBox content = new VBox(15);
        
        // Ajouter le panneau de calendrier
        VBox calendarPanel = calendarController.createDateSelectionPanel();
        content.getChildren().add(calendarPanel);
        
        // Autres champs du formulaire...
        
        return content;
    }
}
```

### 2. Dans une vue d'administration

```java
public class AdminCalendarView {
    private AdvancedCalendarController advancedController;
    
    public AdminCalendarView(EventService eventService, 
                           ReservationService reservationService) {
        advancedController = new AdvancedCalendarController(
            eventService, reservationService
        );
    }
    
    public VBox createView() {
        return advancedController.createAdvancedCalendarPanel();
    }
}
```

## 🔍 Dépannage

### Problèmes courants

1. **Le calendrier n'affiche pas les couleurs**
   - Vérifiez que `setDateAvailabilities()` est appelé avec des données
   - Vérifiez que les dates dans la Map correspondent aux dates affichées

2. **Les boutons de date ne sont pas cliquables**
   - Vérifiez que la disponibilité n'est pas définie à 2 (complet)
   - Vérifiez que les callbacks sont correctement définis

3. **Problèmes de performance avec beaucoup d'événements**
   - Utilisez le filtrage pour limiter les données affichées
   - Chargez les données de manière asynchrone

### Logs de débogage

Activez les logs pour suivre le comportement :

```java
// Dans CalendarReservationController.showCalendarDialog()
try {
    List<Event> events = eventService.getAllEvents();
    System.out.println("Événements chargés: " + events.size());
    // ...
} catch (Exception e) {
    System.err.println("Erreur: " + e.getMessage());
    e.printStackTrace();
}
```

## 📈 Améliorations futures

### Fonctionnalités potentielles
- Sélection de plage de dates
- Vue semaine/jour
- Synchronisation avec Google Calendar/Outlook
- Export PDF du calendrier
- Notifications pour les événements à venir
- Graphiques de statistiques avancés
- Mode sombre

### Optimisations
- Chargement paresseux des données
- Mise en cache des disponibilités
- Mise à jour en temps réel via WebSocket
- Support pour les fuseaux horaires
- Internationalisation (multi-langues)

## 📚 Références

- [JavaFX DatePicker Documentation](https://openjfx.io/javadoc/17/javafx.controls/javafx/scene/control/DatePicker.html)
- [Java Time API](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)
- [JavaFX CSS Reference](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/doc-files/cssref.html)

---

**Dernière mise à jour :** 21 Avril 2026  
**Version du système :** 2.0  
**Auteur :** Système de Gestion d'Événements