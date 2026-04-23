# 📋 Guide Complet: Heure et Image avec ID

## ✅ Étape 1: Gestion des Heures (RÉEL)

### Avant
- TextField simple avec placeholder "10:00"
- Utilisateur doit écrire au format HH:mm manuellement
- Erreur facile si format incorrect

### Après - TimePickerSpinner
- **2 Spinners** pour heures (0-23) et minutes (0-59)
- L'utilisateur **sélectionne** l'heure avec ⬆️ ⬇️
- Format garanti correct automatiquement
- Voir: `src/main/java/org/example/util/TimePickerSpinner.java`

**Utilisation:**
```java
timePicker = new TimePickerSpinner(LocalTime.of(10, 0));
LocalTime time = timePicker.getTimeAsLocalTime();
String timeString = timePicker.getTimeAsString(); // "10:30"
```

---

## ✅ Étape 2: Gestion des Images avec ID

### Avant
- Juste le chemin du fichier en TextInput
- Pas d'ID
- Pas de stockage structuré

### Après - ImageManager avec ID Unique
```
images_uploaded/
├── IMG_20260423_150245_a1b2c3d4.jpg
├── IMG_20260423_150530_e5f6g7h8.jpg
└── IMG_20260423_160000_i9j0k1l2.png
```

**Structure de chaque image:**
- **ID**: `IMG_20260423_150245_a1b2c3d4`
- **Path**: `images_uploaded/IMG_20260423_150245_a1b2c3d4.jpg`
- **FileName**: `IMG_20260423_150245_a1b2c3d4.jpg`

Voir: `src/main/java/org/example/util/ImageManager.java`

---

## 📝 Formulaire Amélioré

### Champs Image (Affichage Complet)

```
┌─────────────────────────────────┐
│ Image                           │
├─────────────────────────────────┤
│ Image ID  │ IMG_20260423_15024 │ [Parcourir]
│ [Auto-généré, lecture seule]    │
├─────────────────────────────────┤
│ Chemin: images_uploaded/IMG_... │
│ [Lecture seule]                 │
├─────────────────────────────────┤
│ ✓ Image chargée: photo.jpg      │
│ [Aperçu - mise à jour auto]     │
└─────────────────────────────────┘
```

### Flux Utilisateur
1. **Cliquer "Parcourir"**
   - FileChooser s'ouvre
   - Sélectionner image JPG/PNG/GIF

2. **ImageManager automatique:**
   - Génère ID unique: `IMG_20260423_150245_a1b2c3d4`
   - Copie fichier vers `images_uploaded/`
   - Retourne ImageInfo (id, path, fileName)

3. **Formulaire mis à jour:**
   - ✓ Image ID rempli (auto-généré)
   - ✓ Path rempli (chemin stocké)
   - ✓ Aperçu affiche le nom du fichier

---

## 🔧 Code Principal

### TimePickerSpinner en formulaire
```java
// buildFormPage()
timePicker = new TimePickerSpinner(LocalTime.of(10, 0));
addRow(form, 4, "Heure", timePicker); // Au lieu de timeField

// buildEventFromForm()
LocalTime time = timePicker.getTimeAsLocalTime();
LocalDateTime eventDateTime = LocalDateTime.of(datePicker.getValue(), time);
```

### ImageManager en chooseImage()
```java
private void chooseImage() {
    FileChooser chooser = new FileChooser();
    File file = chooser.showOpenDialog(primaryStage);
    
    if (file != null) {
        try {
            // Upload avec ID unique
            ImageManager.ImageInfo imageInfo = ImageManager.uploadImage(file);
            
            // Mettre à jour champs
            imageIdField.setText(imageInfo.id);
            imagePathField.setText(imageInfo.path);
            updateImagePreview(imageInfo.path);
            
        } catch (IOException e) {
            showError("Erreur upload", e.getMessage());
        }
    }
}
```

---

## 📊 Base de Données

Champ `image` dans table `event` stocke maintenant:
- **Anciennement**: Juste le chemin ou rien
- **Maintenant**: Chemin complet et structuré

```
ID | Titre        | Image Path
---|--------------|------------------------------------------
1  | Yoga Morning | images_uploaded/IMG_20260423_150245.jpg
2  | Fitness      | images_uploaded/IMG_20260423_160000.png
3  | Méditation   | (vide - pas d'image)
```

---

## 🚀 Fonctionnalités Complètes

### TimePickerSpinner
✅ Sélection visuelle heures (0-23)  
✅ Sélection visuelle minutes (0-59, incréments 5)  
✅ Format garanti HH:mm  
✅ Méthode `getTimeAsLocalTime()` pour récupérer  

### ImageManager
✅ ID unique généré automatiquement  
✅ Dossier centralisé: `images_uploaded/`  
✅ Upload avec copie de fichier  
✅ Vérification existence fichier  
✅ Suppression image  
✅ Classe ImageInfo pour retourner les 3 données  

### Formulaire Amélioré
✅ TimePickerSpinner au lieu de TextField  
✅ Image ID (lecture seule, auto-générée)  
✅ Image Path (lecture seule, stocké)  
✅ Aperçu image (mise à jour auto)  
✅ Bouton Parcourir intégré  

---

## 📂 Fichiers Créés/Modifiés

### Nouveaux Fichiers
- `src/main/java/org/example/util/TimePickerSpinner.java` ✨
- `src/main/java/org/example/util/ImageManager.java` ✨

### Modifiés
- `src/main/java/org/example/Main.java`
  - buildFormPage(): Ajout TimePickerSpinner et ImageManager
  - buildEventFromForm(): Utilise timePicker.getTimeAsLocalTime()
  - chooseImage(): Utilise ImageManager.uploadImage()
  - edit(): Charge heure avec timePicker.setTime()
  - resetForm(): Réinitialise tout correctement

---

## ✅ Test Rapide

1. **Compiler**: `mvn clean compile`
2. **Lancer**: `mvn javafx:run`
3. **Admin ajoute événement:**
   - Heure: Utiliser ⬆️ ⬇️ Spinners
   - Image: Cliquer Parcourir → Choisir → ID généré auto
   - Voir ID et Path remplies automatiquement
4. **Base données**: Chemin correct stocké

---

## 🎯 Avantages

✅ **Heure RÉELLE**: Pas d'erreur format HH:mm  
✅ **Image avec ID**: Traçabilité complète  
✅ **Stockage structuré**: Dossier centralisé  
✅ **Affichage complet**: ID + Path visibles  
✅ **Automatisation**: ID généré sans action utilisateur  
✅ **Scalabilité**: Prêt pour gérer des centaines d'images  

