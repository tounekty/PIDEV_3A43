# 🚀 Guide de Lancement et Test

## Lancer l'application de démonstration

### Windows

**Méthode 1: Double-cliquez sur le script batch**
```bash
lancer_calendrier_dual.bat
```

**Méthode 2: Ligne de commande**
```powershell
cd "c:\Users\sarah\OneDrive - ESPRIT\Bureau\projetjava"
mvn clean compile
mvn exec:java -Dexec.mainClass="org.example.util.DualMonthCalendarApp"
```

### Linux/Mac

```bash
cd ~/projetjava
mvn clean compile
mvn exec:java -Dexec.mainClass="org.example.util.DualMonthCalendarApp"
```

## Étapes de lancement

### Étape 1: Compilation
```bash
mvn clean compile
```
✅ Devrait afficher: `BUILD SUCCESS`

### Étape 2: Exécution
```bash
mvn exec:java -Dexec.mainClass="org.example.util.DualMonthCalendarApp"
```
✅ Devrait lancer la fenêtre du calendrier

## Tests manuels

### Test 1: Navigation entre les mois

```
1. Lancer l'application
   ✅ Devrait afficher Avril et Mai 2026 côte à côte

2. Cliquer sur "Suivant ▶"
   ✅ Devrait afficher Mai et Juin 2026

3. Cliquer sur "Précédent ◀"
   ✅ Devrait afficher Avril et Mai 2026

4. Cliquer sur "Aujourd'hui"
   ✅ Devrait revenir au mois actuel
```

### Test 2: Sélection de date

```
1. Cliquer sur le 22 Avril
   ✅ La date devrait être surlignée
   ✅ Les informations devraient s'afficher en bas

2. Cliquer sur une autre date (ex: 5 Mai)
   ✅ La sélection devrait changer

3. Cliquer sur un jour grisé
   ✅ Rien ne devrait se passer (bouton désactivé)
```

### Test 3: Vérifier les couleurs

```
1. Observer le calendrier
   ✅ Chaque date doit avoir une couleur

2. Vérifier la légende
   ✅ Les 4 couleurs doivent être expliquées

3. Comparer avec la disponibilité réelle
   ✅ Les couleurs doivent correspondre aux places disponibles
```

### Test 4: Performance

```
1. Naviguer rapidement entre les mois
   ✅ Devrait être fluide, pas de freeze

2. Ouvrir/fermer l'application plusieurs fois
   ✅ Devrait toujours fonctionner correctement

3. Ouvrir 2-3 instances de l'application
   ✅ Chaque instance devrait être indépendante
```

## Cas de test - Scénarios

### Scénario 1: Consulter la disponibilité

```
Utilisateur:
1. Ouvre l'application
2. Cherche une date avec beaucoup de places (🟢 ou 🔵)
3. Sélectionne cette date
4. Voit les événements disponibles

Résultat attendu: ✅ Succès
```

### Scénario 2: Trouver une date complète

```
Utilisateur:
1. Parcourt le calendrier en cherchant les dates complètes (⚫)
2. Sélectionne une date grise
3. Essaie de réserver (bouton Continuer désactivé)

Résultat attendu: ✅ Succès - Pas possible de réserver
```

### Scénario 3: Navigation du mois suivant

```
Utilisateur:
1. Voit Avril/Mai
2. Clique "Suivant" 3 fois
3. Devrait voir Juillet/Août

Résultat attendu: ✅ Succès - Navigation correcte
```

## Vérifier les logs

Pendant l'exécution, vous devriez voir:

```
[INFO] Starting DualMonthCalendarApp...
[INFO] Initializing services...
✅ Services initialized
[INFO] Loading availability data...
✅ Availability data loaded
[INFO] Rendering calendar...
✅ Calendar rendered - 2 months displayed
[INFO] Application ready
```

## Dépannage

### Problème: "APPLICATION LAUNCH FAILED"

**Cause**: Dépendances manquantes
**Solution**:
```bash
mvn clean install
mvn compile
```

### Problème: "Cannot find symbol"

**Cause**: Code compilé obsolète
**Solution**:
```bash
mvn clean compile
```

### Problème: Fenêtre ne s'affiche pas

**Cause**: Display server non disponible
**Solution** (Linux):
```bash
export DISPLAY=:0
mvn exec:java -Dexec.mainClass="org.example.util.DualMonthCalendarApp"
```

### Problème: Pas de couleurs sur le calendrier

**Cause**: Pas d'événements dans la BD ou requête échouée
**Vérification**:
```java
// Dans le code, ajouter des logs
System.out.println("Events found: " + eventService.getAllEvents().size());
```

## Commandes utiles

### Nettoyer les fichiers compilés
```bash
mvn clean
```

### Recompiler le projet
```bash
mvn compile
```

### Lancer les tests (si disponibles)
```bash
mvn test
```

### Créer un JAR exécutable
```bash
mvn package
java -jar target/projetjava-1.0-SNAPSHOT.jar
```

### Voir tous les logs Maven
```bash
mvn clean compile -X
```

## Comportement attendu à chaque action

| Action | Résultat attendu |
|--------|-----------------|
| Lancer app | Voir Avril/Mai 2026 avec couleurs |
| Cliquer date | Voir détails en bas |
| Cliquer "Suivant" | Mois suivant s'affiche |
| Cliquer "Précédent" | Mois précédent s'affiche |
| Cliquer "Aujourd'hui" | Revenir au mois actuel |
| Cliquer date grise | Aucun effet (bouton grisé) |
| Redimensionner fenêtre | Calendrier s'adapte |

## Métriques de succès

✅ **Application se lance**: Moins de 3 secondes
✅ **Calendrier s'affiche**: 2 mois visibles
✅ **Couleurs correctes**: Correspondent aux % de disponibilité
✅ **Navigation fluide**: Changement de mois sans délai
✅ **Sélection réactive**: Clic enregistré instantanément
✅ **Pas d'erreurs**: Console sans erreurs
✅ **Responsive**: Redimensionnement adapte la taille

## Intégration dans votre projet

Après validation, vous pouvez:

1. **Ajouter à votre interface principale**
```java
DualMonthCalendarView calendar = new DualMonthCalendarView(...);
mainPane.setCenter(calendar);
```

2. **Utiliser avec votre réservation**
```java
calendar.setOnDateSelected(date -> {
    loadEventsForDate(date);
    showReservationForm();
});
```

3. **Customizer le style**
```css
.date-button-high-availability {
    -fx-background-color: #your-color;
}
```

## Prochaines étapes

1. ✅ Tester l'application de démo
2. ⏭️ Intégrer dans votre interface principale
3. ⏭️ Customiser les couleurs selon vos préférences
4. ⏭️ Ajouter des événements à la BD pour tester
5. ⏭️ Déployer dans votre application

---

**Besoin d'aide?** Consultez les guides:
- [Guide complet](DUAL_MONTH_CALENDAR_GUIDE.md)
- [Guide d'intégration](INTEGRATION_GUIDE.md)
- [Aperçu visuel](VISUAL_PREVIEW.md)
