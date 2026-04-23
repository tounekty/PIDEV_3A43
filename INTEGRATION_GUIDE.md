# 🔧 Guide d'Intégration - Calendrier à Deux Mois

## Comment intégrer le calendrier à votre projet existant

### Option 1: Utilisation directe en Java (Plus simple)

```java
import org.example.util.DualMonthCalendarView;
import org.example.event.EventService;
import org.example.reservation.ReservationService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MyReservationApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialiser les services
        EventService eventService = new EventService();
        ReservationService reservationService = new ReservationService();
        
        // Créer le calendrier avec callback
        DualMonthCalendarView calendar = new DualMonthCalendarView(
            eventService,
            reservationService,
            date -> handleDateSelected(date)
        );
        
        // Ajouter au conteneur principal
        VBox root = new VBox(20);
        root.setPadding(new javafx.geometry.Insets(20));
        root.getChildren().add(calendar);
        
        Scene scene = new Scene(root, 1100, 750);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Réservation d'Événements");
        primaryStage.show();
    }
    
    private void handleDateSelected(java.time.LocalDate date) {
        System.out.println("Date sélectionnée: " + date);
        // Votre logique ici
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

### Option 2: Intégration dans FXML existant

#### Étape 1: Créer un contrôleur

```java
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.example.util.DualMonthCalendarView;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

public class ReservationController {
    
    @FXML
    private VBox calendarContainer;
    
    private DualMonthCalendarView calendarView;
    
    @FXML
    public void initialize() {
        EventService eventService = new EventService();
        ReservationService reservationService = new ReservationService();
        
        // Créer le calendrier
        calendarView = new DualMonthCalendarView(
            eventService,
            reservationService,
            this::onDateSelected
        );
        
        // Ajouter au conteneur
        calendarContainer.getChildren().add(calendarView);
    }
    
    private void onDateSelected(java.time.LocalDate date) {
        System.out.println("Date sélectionnée: " + date);
    }
}
```

#### Étape 2: Mettre à jour le FXML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.geometry.Insets?>
<?import javafx.scene.layout.VBox?>
<?import javafx.scene.control.Label?>

<VBox xmlns="http://javafx.com/javafx/25" 
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="your.package.ReservationController"
      spacing="15" padding="20">
    
    <Label text="Sélectionnez une date pour réserver" 
           style="-fx-font-size: 16; -fx-font-weight: bold;" />
    
    <VBox fx:id="calendarContainer" VBox.vgrow="ALWAYS" />
    
</VBox>
```

#### Étape 3: Charger le FXML dans votre Application

```java
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MyApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/reservation.fxml")
        );
        
        Scene scene = new Scene(loader.load(), 1100, 750);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Réservation");
        primaryStage.show();
    }
}
```

### Option 3: Intégration complète avec réservation

```java
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class CompleteReservationFlow {
    
    public VBox createReservationInterface() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        
        // En-tête
        Label title = new Label("Réservation d'Événements");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        root.getChildren().add(title);
        
        // Calendrier
        DualMonthCalendarView calendar = new DualMonthCalendarView(
            new EventService(),
            new ReservationService(),
            date -> updateEventList(date)
        );
        root.getChildren().add(calendar);
        
        // Liste des événements
        ListView<String> eventList = new ListView<>();
        root.getChildren().add(eventList);
        
        // Formulaire de réservation
        VBox reservationForm = createReservationForm(eventList);
        root.getChildren().add(reservationForm);
        
        return root;
    }
    
    private void updateEventList(java.time.LocalDate date) {
        // Charger les événements pour cette date
    }
    
    private VBox createReservationForm(ListView<String> eventList) {
        VBox form = new VBox(10);
        
        ComboBox<String> eventCombo = new ComboBox<>();
        ComboBox<String> userCombo = new ComboBox<>();
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 10, 1);
        Button submitBtn = new Button("Réserver");
        
        submitBtn.setOnAction(e -> submitReservation(
            eventCombo.getValue(),
            userCombo.getValue(),
            quantitySpinner.getValue()
        ));
        
        form.getChildren().addAll(
            new Label("Événement:"), eventCombo,
            new Label("Utilisateur:"), userCombo,
            new Label("Quantité:"), quantitySpinner,
            submitBtn
        );
        
        return form;
    }
    
    private void submitReservation(String event, String user, int quantity) {
        System.out.println("Réservation: " + event + " pour " + user + " x" + quantity);
    }
}
```

## Intégration avec la base de données

### Configuration automatique

Le calendrier utilise automatiquement:
- `EventService` pour récupérer les événements
- `ReservationService` pour calculer la disponibilité

Aucune configuration supplémentaire n'est nécessaire!

### Personnalisation de la source de données

Si vous voulez utiliser une source de données différente:

```java
// Créer une classe personnalisée
public class CustomCalendarView extends DualMonthCalendarView {
    
    @Override
    protected void loadAvailabilityData() {
        // Votre logique personnalisée
        Map<LocalDate, Integer> customAvailability = getCustomData();
        // Appliquer les données
    }
}
```

## Ajouter des événements personnalisés

### Méthode 1: Callback simple

```java
calendar.setOnDateSelected(date -> {
    System.out.println("Date cliquée: " + date);
    // Afficher les détails
    showEventDetails(date);
});
```

### Méthode 2: EventHandler avancé

```java
calendar.setOnDateSelected(date -> {
    try {
        List<Event> events = eventService.getEventsByDate(date);
        
        for (Event event : events) {
            System.out.println("Événement: " + event.getTitle());
            System.out.println("Lieu: " + event.getLieu());
            System.out.println("Capacité: " + event.getCapacite());
        }
    } catch (Exception e) {
        showError("Erreur de chargement", e);
    }
});
```

## Styles et personnalisation

### Ajouter des styles CSS personnalisés

```css
/* Dans votre fichier CSS */

/* Modifier les couleurs */
.date-button-low-availability {
    -fx-background-color: #your-color;
}

.date-button-medium-availability {
    -fx-background-color: #your-color;
}

/* Ajouter des animations */
@keyframes dateButtonHover {
    0% { -fx-scale: 1; }
    50% { -fx-scale: 1.1; }
    100% { -fx-scale: 1; }
}

.date-button:hover {
    -fx-animation: dateButtonHover 0.3s ease;
}
```

### Appliquer les styles

```java
scene.getStylesheets().add(
    getClass().getResource("/calendar-styles.css").toExternalForm()
);
```

## Dépannage courant

### Le calendrier ne s'affiche pas

**Problème**: Le VBox est vide
**Solution**:
```java
// Vérifier que calendarView n'est pas null
if (calendarView != null) {
    calendarContainer.getChildren().add(calendarView);
} else {
    System.out.println("Erreur: calendarView est null");
}
```

### Les couleurs de disponibilité ne s'affichent pas

**Problème**: Les données de disponibilité ne sont pas chargées
**Solution**:
```java
// Vérifier les logs
try {
    calendar.loadAvailabilityData();
} catch (Exception e) {
    System.err.println("Erreur: " + e.getMessage());
    e.printStackTrace();
}
```

### Les événements ne s'affichent pas

**Problème**: EventService ne retourne pas de résultats
**Solution**:
```java
// Vérifier la connexion à la BD
try {
    List<Event> events = eventService.getAllEvents();
    System.out.println("Nombre d'événements: " + events.size());
} catch (Exception e) {
    System.err.println("Erreur BD: " + e.getMessage());
}
```

## Performance

### Optimisations appliquées

- ✅ Caching des calculs de disponibilité
- ✅ Chargement au démarrage (pas de requête par clic)
- ✅ Mise en cache de 3 mois
- ✅ Pas de requête répétée pour la même date

### Tuning avancé

```java
// Augmenter la plage de cache
calendar.preloadMonths(-3, 3); // -3 à +3 mois

// Désactiver le cache
calendar.setCacheEnabled(false);

// Rafraîchir les données manuellement
calendar.refreshData();
```

## Tests unitaires

### Exemple de test

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DualMonthCalendarTest {
    
    @Test
    public void testDateSelection() {
        // Créer un calendrier
        DualMonthCalendarView calendar = new DualMonthCalendarView(
            new EventService(),
            new ReservationService()
        );
        
        // Sélectionner une date
        LocalDate testDate = LocalDate.of(2026, 4, 22);
        calendar.setOnDateSelected(date -> {
            assertEquals(testDate, date);
        });
        
        // Simuler le clic
        // calendar.simulateClick(testDate);
    }
}
```

## Support et documentation

- 📖 [Guide complet](DUAL_MONTH_CALENDAR_GUIDE.md)
- 📊 [Aperçu visuel](VISUAL_PREVIEW.md)
- ✅ [Résumé d'implémentation](IMPLEMENTATION_SUMMARY.md)

---

**Questions?** Consultez la documentation ou ouvrez une issue.
