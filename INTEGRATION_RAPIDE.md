# Guide d'Intégration Rapide - Système de Calendrier

## ⚡ Intégration en 5 minutes

### Étape 1 : Ajouter les dépendances (déjà fait)
Le fichier `pom.xml` a été mis à jour avec :
- `javafx-controls` (déjà présent)
- `javafx-fxml` (ajouté)

### Étape 2 : Utiliser le CalendarPicker simple

```java
import org.example.util.CalendarPicker;
import java.time.LocalDate;

// Dans votre méthode d'initialisation
CalendarPicker simpleCalendar = new CalendarPicker(date -> {
    // Callback appelé quand une date est sélectionnée
    System.out.println("Date sélectionnée: " + date);
    
    // Ajoutez votre logique ici
    // Ex: charger les événements pour cette date
    // Ex: mettre à jour un formulaire de réservation
});

// Ajouter à votre interface
yourContainer.getChildren().add(simpleCalendar);
```

### Étape 3 : Définir les disponibilités (optionnel)

```java
// Exemple avec des données statiques
Map<LocalDate, Integer> availabilities = new HashMap<>();
availabilities.put(LocalDate.now().plusDays(1), 0); // Peu d'options
availabilities.put(LocalDate.now().plusDays(2), 1); // Beaucoup d'options
availabilities.put(LocalDate.now().plusDays(3), 2); // Complet
availabilities.put(LocalDate.now().plusDays(4), 3); // Disponible

simpleCalendar.setDateAvailabilities(availabilities);
```

### Étape 4 : Intégration avec vos services

```java
import org.example.util.AdvancedCalendarController;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

// Initialiser vos services
EventService eventService = new EventService();
ReservationService reservationService = new ReservationService();

// Créer le contrôleur avancé
AdvancedCalendarController advancedController = 
    new AdvancedCalendarController(eventService, reservationService);

// Obtenir le panneau complet
VBox calendarPanel = advancedController.createAdvancedCalendarPanel();

// Ajouter à votre interface
yourMainContainer.getChildren().add(calendarPanel);
```

### Étape 5 : Récupérer la date sélectionnée

```java
// Pour CalendarPicker simple
LocalDate selectedDate = simpleCalendar.getSelectedDate();

// Pour AdvancedCalendarController
LocalDate selectedDate = advancedController.getSelectedDate();

// Utiliser la date
if (selectedDate != null) {
    // Votre logique métier
    List<Event> events = eventService.getEventsByDate(selectedDate);
    // ...
}
```

## 🎯 Exemples concrets d'intégration

### Dans un dialogue d'ajout de réservation

```java
public class AddReservationDialog {
    private CalendarReservationController calendarController;
    
    public AddReservationDialog(EventService eventService) {
        // Créer le contrôleur de calendrier
        calendarController = new CalendarReservationController(eventService);
    }
    
    public VBox createDialogContent() {
        VBox content = new VBox(15);
        
        // Ajouter le panneau de calendrier
        VBox calendarPanel = calendarController.createDateSelectionPanel();
        content.getChildren().add(calendarPanel);
        
        // Autres champs du formulaire...
        // ...
        
        return content;
    }
    
    public LocalDate getSelectedDate() {
        return calendarController.getSelectedDate();
    }
}
```

### Dans une vue d'administration

```java
public class AdminEventsView {
    private AdvancedCalendarController advancedController;
    
    public AdminEventsView(EventService eventService, 
                          ReservationService reservationService) {
        // Créer le contrôleur avancé
        advancedController = new AdvancedCalendarController(
            eventService, reservationService
        );
        
        // Définir un callback pour les actions d'administration
        advancedController.setOnDateSelected(date -> {
            showAdminActionsForDate(date);
        });
    }
    
    public VBox createView() {
        return advancedController.createAdvancedCalendarPanel();
    }
    
    private void showAdminActionsForDate(LocalDate date) {
        // Afficher les actions d'admin pour cette date
        // Ex: modifier/supprimer des événements
        // Ex: voir les statistiques détaillées
    }
}
```

## 🔧 Configuration rapide

### Styles par défaut
Les styles sont déjà configurés dans :
- `src/main/resources/styles.css`
- `src/main/resources/calendar-styles.css`

### Personnalisation rapide
Pour changer les couleurs, modifiez `calendar-styles.css` :

```css
/* Changer la couleur "Peu d'options" */
.availability-low {
    -fx-background-color: linear-gradient(to bottom, #FF5722, #D84315);
}

/* Changer la couleur "Disponible" */
.availability-high {
    -fx-background-color: linear-gradient(to bottom, #00C853, #00A152);
}
```

## 🚨 Dépannage rapide

### Problème : Le calendrier ne s'affiche pas
**Solution :** Vérifiez que JavaFX est correctement initialisé.

### Problème : Les couleurs ne s'affichent pas
**Solution :** Appelez `setDateAvailabilities()` avec des données.

### Problème : Erreur de compilation
**Solution :** Exécutez `mvn clean compile` pour nettoyer et recompiler.

### Problème : Les boutons ne sont pas cliquables
**Solution :** Vérifiez que la disponibilité n'est pas à 2 (complet).

## 📞 Support rapide

### Pour tester rapidement
```bash
# Lancer l'application de démonstration
mvn javafx:run -DmainClass=org.example.util.CalendarExampleApp

# Lancer l'exemple d'intégration
mvn javafx:run -DmainClass=org.example.util.CalendarIntegrationExample
```

### Fichiers de référence
1. **`CalendarExampleApp.java`** - Application complète de démonstration
2. **`CalendarIntegrationExample.java`** - Exemple d'intégration simple
3. **`DEMONSTRATION_CALENDRIER.md`** - Guide de démonstration
4. **`CALENDAR_SYSTEM_GUIDE.md`** - Documentation complète

## ✅ Vérification rapide

Vérifiez que tout fonctionne :

1. **Compilation** : `mvn compile` (doit réussir)
2. **Styles** : Les fichiers CSS sont dans `src/main/resources/`
3. **Composants** : Toutes les classes sont dans `org.example.util`
4. **Dépendances** : JavaFX FXML est ajouté dans `pom.xml`

## 🎉 Félicitations !

Votre système de calendrier interactif est maintenant prêt à être utilisé. Vous pouvez :

1. **Intégrer immédiatement** dans vos dialogues existants
2. **Personnaliser** l'apparence via CSS
3. **Étendre** les fonctionnalités selon vos besoins
4. **Tester** avec les applications de démonstration

Pour toute question, référez-vous à la documentation complète ou exécutez les exemples fournis.