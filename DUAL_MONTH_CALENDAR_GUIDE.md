# 📅 Calendrier à Deux Mois - Guide d'Utilisation

## Vue d'ensemble

Vous avez maintenant un calendrier moderne et professionnel à deux mois côte à côte avec:
- **Affichage dual**: Deux mois affichés simultanément pour une meilleure vue d'ensemble
- **Système de disponibilité coloré**: Code couleur pour indiquer la disponibilité des événements
- **Navigation intuitive**: Boutons de navigation pour parcourir les mois
- **Légende complète**: Symboles de couleur expliqués
- **Intégration réservation**: Intégré avec le système de réservation existant

## Fichiers créés

### 1. **DualMonthCalendarView.java**
Composant principal qui affiche deux mois côte à côte.

**Localisation**: `src/main/java/org/example/util/DualMonthCalendarView.java`

**Caractéristiques**:
- Affichage de deux mois consécutifs
- Code couleur pour la disponibilité:
  - 🟠 Orange (1-25%) - Peu d'options
  - 🔵 Cyan (25-99%) - Beaucoup d'options
  - ⚫ Gris (0%) - Complet
  - 🟢 Vert (100%) - Disponible
- Navigation entre les mois
- Sélection de date
- Chargement dynamique des données de disponibilité

### 2. **DualMonthCalendarApp.java**
Application de démonstration pour tester le calendrier.

**Localisation**: `src/main/java/org/example/util/DualMonthCalendarApp.java`

**Utilisation**:
```bash
java -cp "target/classes:path/to/jars/*" org.example.util.DualMonthCalendarApp
```

### 3. **DualMonthReservationController.java**
Contrôleur pour intégrer le calendrier dans le flux de réservation.

**Localisation**: `src/main/java/org/example/util/DualMonthReservationController.java`

### 4. **dual_month_calendar.fxml**
Fichier FXML pour une intégration basée sur FXML.

**Localisation**: `src/main/resources/dual_month_calendar.fxml`

### 5. **calendar-styles.css (mise à jour)**
Styles améliorés pour le calendrier.

**Localisation**: `src/main/resources/calendar-styles.css`

## Comment utiliser

### Utilisation basique en Java

```java
import org.example.util.DualMonthCalendarView;
import org.example.event.EventService;
import org.example.reservation.ReservationService;
import javafx.scene.layout.VBox;

// Créer les services
EventService eventService = new EventService();
ReservationService reservationService = new ReservationService();

// Créer le calendrier avec callback
DualMonthCalendarView calendar = new DualMonthCalendarView(
    eventService,
    reservationService,
    selectedDate -> {
        System.out.println("Date sélectionnée: " + selectedDate);
        // Votre logique ici
    }
);

// Ajouter au conteneur
VBox container = new VBox();
container.getChildren().add(calendar);
```

### Utilisation avec FXML

```fxml
<?xml version="1.0" encoding="UTF-8"?>
<?import org.example.util.DualMonthCalendarView?>
<?import javafx.scene.layout.VBox?>

<VBox xmlns="http://javafx.com/javafx/25" xmlns:fx="http://javafx.com/fxml/1">
    <DualMonthCalendarView fx:id="calendar" />
</VBox>
```

### Intégration avec le contrôleur de réservation

```java
import org.example.util.DualMonthReservationController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ReservationApp {
    public void showReservationWindow(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/dual_month_calendar.fxml")
        );
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.show();
    }
}
```

## Système de disponibilité

Le calendrier calcule automatiquement la disponibilité en fonction de:

1. **Nombre total de places**: Capacité de l'événement
2. **Nombre réservé**: Réservations existantes
3. **Pourcentage disponible**: (Total - Réservé) / Total × 100

### Calcul des couleurs

```java
if (availabilityPercentage >= 100)     → Vert (100%)
else if (availabilityPercentage >= 25) → Cyan (25-99%)
else if (availabilityPercentage > 0)   → Orange (1-25%)
else                                    → Gris (0%)
```

## Customisation

### Changer les couleurs

Modifiez dans `DualMonthCalendarView.java` la méthode `applyAvailabilityColor()`:

```java
case 0: // Peu d'options
    style += "-fx-background-color: #YOUR_COLOR; -fx-text-fill: white;";
    break;
```

### Ajouter des événements au clic

```java
calendar.setOnDateSelected(date -> {
    // Afficher les détails de l'événement
    displayEventDetails(date);
});
```

### Changer le format du mois

Modifiez la méthode `updateTitleLabel()`:

```java
private void updateTitleLabel(Label titleLabel) {
    // Votre format personnalisé
    titleLabel.setText(String.format("%s %d", month, year));
}
```

## Architecture du système

```
┌─────────────────────────────────┐
│  DualMonthCalendarView          │
│  (Composant principal)          │
├─────────────────────────────────┤
│ - EventService                  │
│ - ReservationService            │
│ - Navigation                    │
│ - Sélection de date             │
│ - Code couleur                  │
└─────────────────────────────────┘
         │
         ├─→ DualMonthCalendarApp (Démo)
         │
         ├─→ DualMonthReservationController
         │   (Intégration réservation)
         │
         └─→ dual_month_calendar.fxml
             (Intégration FXML)
```

## Dépendances

Le calendrier utilise:
- **JavaFX 25**: Pour l'interface graphique
- **EventService**: Pour récupérer les événements
- **ReservationService**: Pour calculer la disponibilité
- **LocalDate/YearMonth**: Pour gérer les dates

## Fonctionnalités principales

### ✅ Implémentées

- [x] Affichage de deux mois côte à côte
- [x] Navigation entre les mois
- [x] Code couleur de disponibilité
- [x] Légende explicative
- [x] Bouton "Aujourd'hui"
- [x] Sélection de date
- [x] Intégration avec base de données
- [x] Styles CSS professionnels

### 🔄 Futures améliorations possibles

- [ ] Animation de transition entre les mois
- [ ] Vue semaine
- [ ] Filtre par catégorie d'événement
- [ ] Export du calendrier (PDF, ICS)
- [ ] Synchronisation avec calendrier externe
- [ ] Mode sombre
- [ ] Responsif mobile

## Dépannage

### Le calendrier n'affiche pas de couleurs

Vérifiez que:
1. Les services (EventService, ReservationService) sont correctement initialisés
2. La base de données contient des événements
3. Les dates des événements sont dans la plage de trois mois

### Les événements ne s'affichent pas

1. Vérifiez les connexions à la base de données
2. Assurez-vous que la table `event` existe et contient des données
3. Vérifiez les logs pour les erreurs SQL

### Les couleurs ne correspondent pas

Vérifiez le calcul de la disponibilité dans `loadAvailabilityData()` et ajustez les seuils si nécessaire.

## Support

Pour toute question ou problème, consultez:
- Documentation JavaFX: https://openjfx.io
- Code source: `src/main/java/org/example/util/`
- Ressources FXML: `src/main/resources/`

---

**Auteur**: Système de Réservation
**Version**: 1.0
**Date**: Avril 2026
