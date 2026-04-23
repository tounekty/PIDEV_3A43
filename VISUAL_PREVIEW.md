# 📅 Aperçu visuel - Calendrier à Deux Mois

## Vue d'ensemble de l'application

```
┌──────────────────────────────────────────────────────────────────────────┐
│  📅 Calendrier de Réservation - Vue à Deux Mois                          │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ◀ Précédent    Aujourd'hui    Suivant ▶           Avril 2026 - Mai 2026 │
│                                                                          │
│  ┌─────────────────────────────┬──────────────────────────────┐        │
│  │       AVRIL 2026            │       MAI 2026              │        │
│  ├─────────────────────────────┼──────────────────────────────┤        │
│  │ Lun Mar Mer Jeu Ven Sam Dim │ Lun Mar Mer Jeu Ven Sam Dim │        │
│  ├─────────────────────────────┼──────────────────────────────┤        │
│  │  -   -   1   2   3   4   5  │  -   -   -   1   2   3   4  │        │
│  │                              │                              │        │
│  │  6   7   8   9  10  11  12  │  5   6   7   8   9  10  11  │        │
│  │ 🟠  🔵  🔵  🟠  🟠  🔵  🔵  │ 🟢  🟢  🟢  🔵  🔵  🔵  🟢  │        │
│  │                              │                              │        │
│  │ 13  14  15  16  17  18  19  │ 12  13  14  15  16  17  18  │        │
│  │ 🟢  🟠  🔵  🟢  🟠  ⚫   🟠  │ 🔵  🟠  🟢  ⚫   🟠  🔵  🟢  │        │
│  │                              │                              │        │
│  │ 20  21  22  23  24  25  26  │ 19  20  21  22  23  24  25  │        │
│  │ 🟠  🔵  🟠  🟠  🟢  🔵  🔵  │ 🟠  🔵  🔵  ⚫   🟠  🔵  🟢  │        │
│  │                              │                              │        │
│  │ 27  28  29  30   -   -   -  │ 26  27  28  29  30  31   -  │        │
│  │ 🟢  ⚫   🔵  🟠              │ 🟢  🔵  🟢  🟠  🔵  ⚫       │        │
│  │                              │                              │        │
│  └─────────────────────────────┴──────────────────────────────┘        │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ 🟠 Peu d'options (1-25%)   🔵 Beaucoup (25-99%)                     ││
│  │ ⚫ Complet (0%)            🟢 Disponible (100%)                     ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ℹ️ Information sur la Date Sélectionnée                               │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ 📍 Date sélectionnée: Mardi 22 Avril 2026                           ││
│  │                                                                      ││
│  │ ✅ Événements disponibles:                                          ││
│  │    • Conférence Tech - Amphithéâtre A                               ││
│  │    • Atelier Programmation - Salle 101                              ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│                             [Annuler]  [Continuer]                      │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

## Détails des couleurs

### 🟠 Orange - Peu d'options (1-25%)
- Capacité: 100 places
- Réservées: 75-99 places
- Disponibles: 1-25 places
- Action: ⚠️ Dépêchez-vous!

### 🔵 Cyan - Beaucoup d'options (25-99%)
- Capacité: 100 places
- Réservées: 1-75 places
- Disponibles: 25-99 places
- Action: ✅ Réservation recommandée

### ⚫ Gris - Complet (0%)
- Capacité: 100 places
- Réservées: 100 places
- Disponibles: 0 places
- Action: ❌ Indisponible

### 🟢 Vert - Disponible (100%)
- Capacité: 100 places
- Réservées: 0 places
- Disponibles: 100 places
- Action: ✨ Meilleure disponibilité

## Interaction utilisateur

### Flux de réservation

```
1. L'utilisateur voit le calendrier avec deux mois
                    ↓
2. Clique sur une date (par ex: 22 avril)
                    ↓
3. Le système affiche:
   - Les événements de cette date
   - La disponibilité en temps réel
                    ↓
4. L'utilisateur sélectionne un événement
                    ↓
5. Le système affiche:
   - Détails de l'événement
   - Places disponibles
   - Prix (optionnel)
                    ↓
6. L'utilisateur confirme la réservation
                    ↓
7. ✅ Réservation complétée
```

## Exemple de données

### Événement du 22 Avril

```
Titre:              Conférence Tech 2026
Lieu:               Amphithéâtre A
Date/Heure:         22 Avril 2026, 14:00
Durée:              2 heures
Capacité totale:    150 places
Réservées:          45 places
Places disponibles: 105 places
Pourcentage:        70% disponibles → 🔵 Cyan

Catégorie:          Technologie
Description:        Découvrez les dernières tendances
                    de la technologie et de l'IA
```

## Statistiques de disponibilité

### Avril 2026 (exemple)

| Semaine | Disponibilité | Couleur | Détails |
|---------|---------------|--------|---------|
| 1-5 avr | 45% | 🔵 Cyan | 3 dates cyan, 2 orange |
| 6-12 avr | 60% | 🟢 Vert | 4 dates vertes, 3 cyan |
| 13-19 avr | 35% | 🟠 Orange | 2 dates grises, 4 orange |
| 20-26 avr | 50% | 🔵 Cyan | 3 dates cyan, 2 orange, 1 gris |
| 27-30 avr | 40% | 🟠 Orange | 1 date grise, 3 orange |

## Exemple de code d'utilisation

### Créer et afficher le calendrier

```java
// Initialiser les services
EventService eventService = new EventService();
ReservationService reservationService = new ReservationService();

// Créer le calendrier
DualMonthCalendarView calendar = new DualMonthCalendarView(
    eventService,
    reservationService,
    selectedDate -> {
        System.out.println("Date sélectionnée: " + selectedDate);
        // Charger les événements de cette date
        List<Event> events = eventService.getEventsByDate(selectedDate);
        // Afficher les détails
        displayEventDetails(events);
    }
);

// Créer la scène
VBox root = new VBox(20);
root.setPadding(new Insets(20));
root.getChildren().add(calendar);

Scene scene = new Scene(root, 1100, 750);
stage.setScene(scene);
stage.show();
```

## Statistiques d'utilisation (simulation)

### Taux de conversion

```
Visiteurs vus le calendrier:      1000
Sélectionné une date:             850 (85%)
Consulté les détails:             800 (80%)
Effectué une réservation:         600 (60%)
Réservation complétée:            580 (58%)
```

### Dates populaires

```
🥇 22 Avril    - 95 réservations
🥈 25 Avril    - 87 réservations
🥉 19 Avril    - 82 réservations
4️⃣  23 Avril    - 78 réservations
5️⃣  24 Avril    - 75 réservations
```

## Accessibilité

✅ Contraste élevé des couleurs
✅ Texte lisible et grand (13-18px)
✅ Navigation au clavier supportée
✅ Description claire des couleurs dans la légende
✅ Responsive sur différentes tailles d'écran

## Améliorations futures

- 🎬 Animations de transition entre les mois
- 🌙 Mode sombre
- 📱 Version mobile
- 🔔 Notifications de disponibilité
- 📊 Vue statistique
- 🌍 Support multilingue

---

**Note**: Ce document montre une représentation visuelle du calendrier.
L'application réelle affichera des boutons interactifs avec styles JavaFX.
