# 📅 Guide d'Intégration du Calendrier - Exemple Complet

## Vue d'ensemble

Vous avez maintenant un **calendrier interactif avancé** complètement intégré à votre système de gestion d'événements et de réservations. Voici comment l'utiliser dans votre application.

## 1. Intégration basique dans Main.java

Ajoutez cette méthode à votre classe `Main`:

```java
/**
 * Afficher le dialogue de réservation avec calendrier avancé
 */
private void showAdvancedReservationDialog() {
    AdvancedReservationDialog dialog = new AdvancedReservationDialog(
        eventService, 
        reservationService, 
        authService
    );
    dialog.show(primaryStage);
}
```

Puis, connectez-la à votre bouton "Ajouter une réservation":

```java
// Dans votre méthode createUI() ou équivalent
Button addReservationButton = new Button("+ Ajouter une réservation");
addReservationButton.setStyle("-fx-background-color: #0f69ff; -fx-text-fill: white;");
addReservationButton.setOnAction(e -> showAdvancedReservationDialog());
```

## 2. Utilisation du CalendarPicker directement

Si vous voulez utiliser le calendrier directement sans le dialogue:

```java
// Créer le calendrier
CalendarPicker calendar = new CalendarPicker(selectedDate -> {
    System.out.println("Date sélectionnée: " + selectedDate);
});

// Ajouter à votre VBox
VBox container = new VBox(calendar);

// Définir les disponibilités
Map<LocalDate, Integer> availabilities = new HashMap<>();
availabilities.put(LocalDate.now(), 1);              // Beaucoup d'options (cyan)
availabilities.put(LocalDate.now().plusDays(1), 0); // Peu d'options (orange)
availabilities.put(LocalDate.now().plusDays(2), 2); // Complet (gris)

calendar.setDateAvailabilities(availabilities);
```

## 3. Codes couleur - Signification

| Couleur | Code | Signification | Exemple |
|---------|------|---------------|---------|
| 🟠 Orange | 0 | Peu de places (< 25%) | 2/10 places libres |
| 🔵 Cyan | 1 | Beaucoup de places (≥ 25%) | 8/10 places libres |
| ⚫ Gris | 2 | Complet | 0/10 places libres |

## 4. Exemple complet avec calcul automatique

```java
private void showCalendarWithAvailabilities() {
    CalendarDialog dialog = new CalendarDialog();
    
    try {
        // Récupérer tous les événements
        List<Event> allEvents = eventService.getAllEvents();
        Map<LocalDate, Integer> availabilityMap = new HashMap<>();
        
        // Analyser chaque événement
        for (Event event : allEvents) {
            LocalDate eventDate = event.getDateEvent().toLocalDate();
            
            // Compter les places réservées
            int reserved = reservationService.getReservationCountByEvent(event.getId());
            int totalCapacity = event.getCapacite();
            int available = totalCapacity - reserved;
            
            // Déterminer le code couleur
            int availability;
            if (available <= 0) {
                availability = 2; // Complet
            } else if (available < totalCapacity * 0.25) {
                availability = 0; // Peu d'options
            } else {
                availability = 1; // Beaucoup d'options
            }
            
            // Stocker la disponibilité (garder la plus restrictive si plusieurs événements)
            if (availabilityMap.containsKey(eventDate)) {
                int current = availabilityMap.get(eventDate);
                availabilityMap.put(eventDate, Math.max(current, availability));
            } else {
                availabilityMap.put(eventDate, availability);
            }
        }
        
        // Configurer le calendrier
        dialog.setDateAvailabilities(availabilityMap);
        
        // Afficher et traiter la sélection
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            LocalDate selectedDate = result.get();
            System.out.println("Date sélectionnée: " + selectedDate);
            // ... traiter la sélection
        }
        
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

## 5. Fichiers créés

```
src/main/java/org/example/util/
├── CalendarPicker.java                 # Composant calendrier principal
├── CalendarDialog.java                 # Dialogue pour la sélection
├── CalendarReservationController.java   # Contrôleur intégration
└── AdvancedReservationDialog.java       # Exemple complet

src/main/resources/
└── advanced_reservation_calendar.fxml   # Layout d'exemple
```

## 6. Propriétés principales de CalendarPicker

```java
// Créer avec callback
CalendarPicker calendar = new CalendarPicker(date -> { /* ... */ });

// Définir la disponibilité
calendar.setDateAvailability(LocalDate.now(), 1);      // Une date
calendar.setDateAvailabilities(map);                   // Plusieurs dates

// Récupérer la sélection
LocalDate selected = calendar.getSelectedDate();

// Écouter les changements
calendar.selectedDateProperty().addListener((obs, old, now) -> {
    System.out.println("Nouvelle sélection: " + now);
});

// Définir/effacer
calendar.setSelectedDate(LocalDate.now());
calendar.clearSelection();
```

## 7. Personnalisation des styles

### Modifier les couleurs dans CalendarPicker.java:

```java
// Peu d'options
"-fx-background-color: #FFA500;  // Orange par défaut

// Beaucoup d'options
"-fx-background-color: #00BCD4;  // Cyan par défaut

// Complet
"-fx-background-color: #CCCCCC;  // Gris par défaut
```

### Modifier les dimensions:

```java
this.setPrefSize(500, 450);  // Hauteur/largeur du calendrier
```

### Modifier les polices et espacement:

```java

monthYearLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
calendarGrid.setStyle("-fx-hgap: 8; -fx-vgap: 8;");  // Espacement des cases
```

## 8. Intégration avec le formulaire de réservation

```java
// Dans votre dialogue de réservation:
CalendarReservationController calendarController = 
    new CalendarReservationController(eventService, reservationService);

// Ajouter le panneau de sélection
VBox datePanel = calendarController.createDateSelectionPanel();
form.getChildren().add(datePanel);

// Au moment de la soumission:
LocalDate selectedDate = calendarController.getSelectedDate();
if (selectedDate == null) {
    showError("Veuillez sélectionner une date");
    return;
}

// Créer la réservation avec cette date
reservationService.addReservation(selectedDate, userId, eventId, quantity);
```

## 9. Points clés à retenir

✅ **Avantages du calendrier**
- Navigation facile entre les mois
- Indicateurs visuels de disponibilité
- Sélection intuitive
- Responsive design
- Intégration avec votre base de données

❌ **Pièges à éviter**
- Oublier de charger les disponibilités avant affichage
- Ne pas gérer les exceptions SQL
- Ne pas valider la sélection avant d'utiliser la date

## 10. Troubleshooting

### Le calendrier ne s'affiche pas
```java
// Assurez-vous que le composant est ajouté au parent
VBox parent = new VBox();
parent.getChildren().add(calendar);
scene.setRoot(parent);
```

### Les dates ne changent pas de couleur
```java
// Appelez setDateAvailabilities() AVANT showAndWait()
dialog.setDateAvailabilities(map);
var result = dialog.showAndWait();  // ✓ Bon ordre
```

### Les réservations ne se reflètent pas
```java
// Recharger les disponibilités après chaque réservation
List<Event> events = eventService.getAllEvents();
// Recalculer les disponibilités...
calendar.setDateAvailabilities(newMap);
```

## 11. Prochaines étapes

1. Testez le calendrier avec vos données réelles
2. Personnalisez les couleurs selon votre design
3. Intégrez la sélection au formulaire complet
4. Ajoutez des validations métier
5. Optimisez les performances pour les grandes listes d'événements

---

**Besoin d'aide?** Consultez les commentaires dans le code source des classes ou executez les exemples ci-dessus.
