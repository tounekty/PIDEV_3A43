# 📅 Guide d'Utilisation du Calendrier Avancé

## Vue d'ensemble

Vous disposez maintenant d'un système de calendrier interactif et avancé pour votre application de gestion d'événements et de réservations. Le calendrier affiche les disponibilités avec des codes couleur pour une meilleure visibilité.

## Composants créés

### 1. **CalendarPicker** (`org.example.util.CalendarPicker`)
Composant principal affichant un calendrier interactif.

**Caractéristiques:**
- Navigation entre les mois (précédent/suivant)
- Sélection de date
- Indicateurs visuels de disponibilité par couleur
- Support pour les callbacks de sélection

**Utilisation basique:**
```java
CalendarPicker calendar = new CalendarPicker(date -> {
    System.out.println("Date sélectionnée: " + date);
});

// Définir la disponibilité
calendar.setDateAvailability(LocalDate.now(), 1); // 1 = beaucoup d'options

// Récupérer la date sélectionnée
LocalDate selected = calendar.getSelectedDate();
```

### 2. **CalendarDialog** (`org.example.util.CalendarDialog`)
Dialogue modale pour la sélection de date avec le calendrier.

**Utilisation:**
```java
CalendarDialog dialog = new CalendarDialog();

// Ajouter des disponibilités
Map<LocalDate, Integer> availabilities = new HashMap<>();
availabilities.put(LocalDate.now(), 0); // Peu d'options
availabilities.put(LocalDate.now().plusDays(1), 1); // Beaucoup

dialog.setDateAvailabilities(availabilities);

var result = dialog.showAndWait();
if (result.isPresent()) {
    LocalDate selectedDate = result.get();
    // Traiter la date sélectionnée
}
```

### 3. **CalendarReservationController** (`org.example.util.CalendarReservationController`)
Contrôleur pour intégrer le calendrier au système de réservation.

**Utilisation:**
```java
EventService eventService = new EventService();
CalendarReservationController controller = new CalendarReservationController(eventService);

// Créer le panneau de sélection
VBox datePanel = controller.createDateSelectionPanel();

// Ajouter au votre interface
root.getChildren().add(datePanel);

// Récupérer la date sélectionnée
LocalDate selected = controller.getSelectedDate();
```

### 4. **AdvancedReservationDialog** (`org.example.util.AdvancedReservationDialog`)
Exemple complet d'une boîte de dialogue de réservation avec calendrier.

**Utilisation:**
```java
AdvancedReservationDialog dialog = new AdvancedReservationDialog(
    eventService, 
    reservationService, 
    authService
);
dialog.show(primaryStage);
```

## Codes couleur de disponibilité

| Couleur | Code | Signification |
|---------|------|---------------|
| 🟠 Orange | 0 | Peu d'options disponibles (< 25%) |
| 🔵 Cyan/Turquoise | 1 | Beaucoup d'options disponibles (≥ 25%) |
| ⚫ Gris | 2 | Événement complet - Non disponible |

## Comment intégrer dans votre Main.java

Ajoutez cette méthode à votre classe `Main`:

```java
private void showAdvancedReservationDialog() {
    AdvancedReservationDialog dialog = new AdvancedReservationDialog(
        eventService, 
        reservationService, 
        authService
    );
    dialog.show(primaryStage);
}
```

Puis appelez-la depuis votre bouton "Ajouter une réservation":

```java
addReservationButton.setOnAction(e -> showAdvancedReservationDialog());
```

## Exemple avancé: Calculer les disponibilités

```java
// Récupérer tous les événements
List<Event> events = eventService.getAllEvents();
Map<LocalDate, Integer> availabilityMap = new HashMap<>();

for (Event event : events) {
    LocalDate eventDate = event.getDatetime().toLocalDateTime().toLocalDate();
    
    // Compter les places réservées
    int reserved = reservationService.countReservationsByEvent(event.getId());
    int available = event.getCapacity() - reserved;
    
    // Déterminer le code couleur
    int availability;
    if (available <= 0) {
        availability = 2; // Gris - Complet
    } else if (available < event.getCapacity() * 0.25) {
        availability = 0; // Orange - Peu d'options
    } else {
        availability = 1; // Cyan - Beaucoup d'options
    }
    
    availabilityMap.put(eventDate, availability);
}

// Afficher le calendrier avec ces disponibilités
CalendarDialog dialog = new CalendarDialog();
dialog.setDateAvailabilities(availabilityMap);
var result = dialog.showAndWait();
```

## Structure des fichiers

```
src/main/java/org/example/util/
├── CalendarPicker.java                 # Composant calendrier
├── CalendarDialog.java                 # Dialogue du calendrier
├── CalendarReservationController.java   # Contrôleur intégration
└── AdvancedReservationDialog.java       # Exemple complet

src/main/resources/
└── advanced_reservation_calendar.fxml   # Layout d'exemple FXML
```

## Prochaines étapes

1. **Intégrer dans Main.java**: Ajoutez le calendrier avancé à votre interface
2. **Connecter les événements**: Liez la sélection du calendrier au chargement des événements
3. **Valider les réservations**: Utilisez la date sélectionnée pour créer la réservation
4. **Personnaliser les styles**: Modifiez les couleurs et les styles CSS selon votre design

## Personnalisation

### Modifier les couleurs
Éditez les couleurs dans `CalendarPicker.java`:

```java
// Pour peu d'options
"-fx-background-color: #FFA500;  // Remplacer par votre couleur

// Pour beaucoup d'options
"-fx-background-color: #00BCD4;  // Remplacer par votre couleur

// Pour complet
"-fx-background-color: #CCCCCC;  // Remplacer par votre couleur
```

### Modifier les styles
Vous pouvez personnaliser:
- Taille du calendrier: Modifiez `setPrefSize(500, 450)`
- Espacement: Modifiez les paramètres `-fx-spacing`
- Polices: Modifiez `Font.font()`
- Bordures: Modifiez les propriétés `-fx-border-*`

## Troubleshooting

### Le calendrier ne s'affiche pas
- Vérifiez que JavaFX est correctement configuré dans `pom.xml`
- Assurez-vous que le composant est ajouté au parent VBox/HBox

### Les disponibilités ne s'affichent pas correctement
- Vérifiez que `setDateAvailability()` est appelée avant `showAndWait()`
- Assurez-vous que les valeurs de disponibilité sont 0, 1 ou 2

### Les mois ne changent pas
- Vérifiez que les boutons précédent/suivant ont les bons event handlers
- Assurez-vous que `refreshCalendar()` est appelée

## Support

Pour toute question ou problème, consultez les commentaires dans le code source ou les fichiers de classe.
