# Comment Voir le Calendrier dans l'Interface

## 🚀 Méthodes pour voir le calendrier

### Option 1 : Lancer l'application de test (Recommandé)

```bash
mvn javafx:run -DmainClass=org.example.util.TestCalendarInReservation
```

Cette application charge directement le dialogue d'ajout de réservation avec le calendrier intégré.

### Option 2 : Lancer l'application de démonstration complète

```bash
mvn javafx:run -DmainClass=org.example.util.CalendarExampleApp
```

Cette application montre toutes les fonctionnalités du calendrier dans une interface complète avec onglets.

### Option 3 : Lancer l'exemple d'intégration simple

```bash
mvn javafx:run -DmainClass=org.example.util.CalendarIntegrationExample
```

Cette application montre une intégration simple du calendrier sans FXML.

## 📋 Ce que vous verrez

### Dans le dialogue de réservation (`TestCalendarInReservation`)
1. **En-tête** avec titre "Ajouter une Réservation"
2. **Calendrier interactif** en haut du formulaire avec :
   - Navigation entre mois (◀ ▶)
   - Bouton "Aujourd'hui"
   - Codes couleur pour la disponibilité
   - Sélection de date
3. **Formulaire de réservation** avec :
   - Sélection d'événement (mis à jour selon la date)
   - Détails de l'événement
   - Sélection d'utilisateur
   - Nombre de places
4. **Boutons** Annuler et Confirmer

### Fonctionnalités visibles
- ✅ **Calendrier complet** avec navigation
- ✅ **Codes couleur** (Vert, Cyan, Orange, Gris)
- ✅ **Sélection de date** interactive
- ✅ **Mise à jour automatique** des événements selon la date
- ✅ **Validation du formulaire**
- ✅ **Interface moderne** avec styles CSS

## 🔧 Intégration dans votre code existant

### Si vous voulez utiliser le dialogue modifié :

Le fichier `add_reservation_dialog.fxml` a été mis à jour avec :
1. **Calendrier intégré** en haut du formulaire
2. **Contrôleur FXML** (`AddReservationController`) pour gérer la logique
3. **Styles améliorés** avec fond bleu clair

### Pour l'utiliser dans votre application :

```java
// Charger le dialogue avec calendrier
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/add_reservation_dialog.fxml")
);
VBox dialogContent = loader.load();
AddReservationController controller = loader.getController();

// Afficher dans une nouvelle fenêtre
Stage dialogStage = new Stage();
dialogStage.setScene(new Scene(dialogContent));
dialogStage.setTitle("Ajouter une Réservation");
dialogStage.show();
```

## 🎨 Personnalisation

### Modifier l'apparence du calendrier

Les styles sont dans :
- `src/main/resources/styles.css` - Styles généraux
- `src/main/resources/calendar-styles.css` - Styles du calendrier

### Changer les couleurs

Éditez `calendar-styles.css` :

```css
/* Changer la couleur "Disponible" */
.availability-high {
    -fx-background-color: linear-gradient(to bottom, #00C853, #00A152);
}

/* Changer la couleur "Peu d'options" */
.availability-low {
    -fx-background-color: linear-gradient(to bottom, #FF5722, #D84315);
}
```

## 🐛 Dépannage

### Si l'application ne démarre pas :
1. Vérifiez que Maven est installé : `mvn --version`
2. Vérifiez que Java 25 est installé : `java --version`
3. Essayez de nettoyer et recompiler : `mvn clean compile`

### Si le calendrier ne s'affiche pas :
1. Vérifiez que les fichiers FXML sont dans `src/main/resources/`
2. Vérifiez que la compilation a réussi
3. Vérifiez les logs d'erreur dans la console

### Si les couleurs ne s'affichent pas :
1. Vérifiez que les fichiers CSS sont chargés
2. Vérifiez que `setDateAvailabilities()` est appelé (dans le contrôleur)

## 📊 Fonctionnalités du calendrier intégré

### 1. Navigation
- **Mois précédent/suivant** : Boutons ◀ ▶
- **Aujourd'hui** : Bouton dédié
- **Sélection** : Cliquez sur une date

### 2. Codes couleur
- **🟢 Vert** : Disponible (100%)
- **🔵 Cyan** : Beaucoup d'options (25-99%)
- **🟠 Orange** : Peu d'options (1-25%)
- **⚫ Gris** : Complet (0%)

### 3. Interaction
- **Survol** : Effet d'agrandissement
- **Sélection** : Bordure bleue
- **Aujourd'hui** : Bordure orange
- **Désactivé** : Dates complètes non cliquables

### 4. Intégration formulaire
- **Événements** : Mis à jour selon la date sélectionnée
- **Validation** : Vérification des champs obligatoires
- **Confirmation** : Message de succès

## ✅ Vérification rapide

Pour vérifier que tout fonctionne :

1. **Compilation** : `mvn compile` (doit réussir)
2. **Fichiers** : Vérifiez que `add_reservation_dialog.fxml` existe
3. **Contrôleur** : Vérifiez que `AddReservationController.java` est compilé
4. **Styles** : Vérifiez que les fichiers CSS sont présents

## 🎉 Prêt à l'emploi !

Le calendrier est maintenant **entièrement intégré** dans votre dialogue de réservation. Vous pouvez :

1. **Tester immédiatement** avec `TestCalendarInReservation`
2. **Intégrer directement** dans votre application
3. **Personnaliser** l'apparence via CSS
4. **Étendre** les fonctionnalités selon vos besoins

Pour toute question, exécutez les applications de test ou consultez la documentation complète.