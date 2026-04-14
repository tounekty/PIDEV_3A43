# 🔧 Documentation Technique - CRUD Ressources et Commentaires

## 📋 Table des Matières
1. [Architecture](#architecture)
2. [Modèles](#modèles)
3. [DAOs](#daos)
4. [Services](#services)
5. [Controllers](#controllers)
6. [FXML UI](#fxml-ui)
7. [Flux de Données](#flux-de-données)
8. [Cas d'Usage](#cas-dusage)

---

## 🏗️ Architecture

### Architecture Générale

```
┌─────────────────────────────────────┐
│        JavaFX Application           │
│  (Main.java + Header avec bouton)   │
└───────────────┬─────────────────────┘
                │
         [📚 Ressources Button]
                │
┌───────────────▼─────────────────────┐
│   ResourceListController (FXML)     │
│  ├─ Liste ressources (TableView)    │
│  ├─ Recherche en temps réel         │
│  ├─ CRUD buttons                    │
└───────────────┬─────────────────────┘
                │
        ┌───────┼────────┬──────────┐
        │       │        │          │
   [View]  [Edit]  [Detail]  [Delete]
        │       │        │          │
┌───────▼──┐ ┌──▼───┐ ┌──▼───────┐ │
│Form Window│ │Form  │ │Detail    │ │
│   (New)   │ │      │ │ Window   │ │
└───────────┘ └──────┘ └──────────┘ │
                                    │
            ┌───────────────────────┘
            │
    ┌───────▼──────────────┐
    │ ResourceService      │
    │ ├─ Validation        │
    │ ├─ Business Logic    │
    │ └─ CRUD Operations   │
    └───────┬──────────────┘
            │
    ┌───────▼──────────────┐
    │ ResourceDAO          │
    │ ├─ SQL Queries       │
    │ ├─ ResultSet Mapping │
    │ └─ Connection Pool   │
    └───────┬──────────────┘
            │
    ┌───────▼──────────────┐
    │ MySQL Database       │
    │ └─ Tables:           │
    │    ├─ resource       │
    │    └─ commentaire    │
    └──────────────────────┘
```

---

## 📦 Modèles

### Resource.java

```java
public class Resource {
    // Type constants
    public static final String TYPE_ARTICLE = "article";
    public static final String TYPE_VIDEO = "video";
    
    // Fields
    private int id;
    private String title;           // 3-255 chars
    private String description;     // 10-5000 chars
    private String type;            // article | video
    private String filePath;        // Optional
    private String videoUrl;        // Optional URL
    private String imageUrl;        // Optional URL
    private LocalDateTime createdAt;
    private int userId;
    
    // Constructors & Getters/Setters
}
```

**Validations:**
- Titre: 3-255 caractères, obligatoire
- Description: 10-5000 caractères, obligatoire
- Type: "article" ou "video" uniquement
- URLs: optionnelles, validations au format

---

### Commentaire.java

```java
public class Commentaire {
    private int id;
    private int resourceId;        // FK → resource.id
    private int userId;            // Author user ID
    private String authorName;     // 2-100 chars
    private String authorEmail;    // Valid email format
    private String content;        // 5-2000 chars
    private int rating;            // 1-5 stars
    private LocalDateTime createdAt;
    private boolean approved;      // Moderation flag
}
```

**Validations:**
- Nom: 2-100 caractères
- Email: format valide regex
- Contenu: 5-2000 caractères
- Rating: 1-5 obligatoire
- Approved: faux par défaut (en attente)

---

## 🗃️ DAOs

### ResourceDAO.java

```java
public class ResourceDAO {
    // CREATE
    public void create(Resource resource) throws SQLException
    // Insère une nouvelle ressource et récupère l'ID généré
    
    // READ
    public List<Resource> findAll() throws SQLException
    // Récupère toutes les ressources, ordonnées par date DESC
    
    public Resource findById(int id) throws SQLException
    // Récupère une ressource spécifique
    
    // UPDATE
    public void update(Resource resource) throws SQLException
    // Modifie les champs modifiables (ne change pas created_at)
    
    // DELETE
    public void delete(int id) throws SQLException
    // Supprime une ressource (cascade sur commentaires)
    
    // SEARCH
    public List<Resource> search(String query) throws SQLException
    // Recherche dans title ET description (LOWER LIKE)
}
```

**Requêtes SQL Principales:**

```sql
-- Create
INSERT INTO resource (title, description, type, file_path, video_url, image_url, created_at, id_user)
VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)

-- Read All
SELECT * FROM resource ORDER BY created_at DESC

-- Read by ID
SELECT * FROM resource WHERE id = ?

-- Update
UPDATE resource SET title=?, description=?, type=?, file_path=?, video_url=?, image_url=?
WHERE id = ?

-- Delete
DELETE FROM resource WHERE id = ?

-- Search
SELECT * FROM resource WHERE LOWER(title) LIKE ? OR LOWER(description) LIKE ?
ORDER BY created_at DESC
```

---

### CommentaireDAO.java

```java
public class CommentaireDAO {
    // CREATE
    public void create(Commentaire commentaire) throws SQLException
    
    // READ
    public List<Commentaire> findByResourceId(int resourceId) throws SQLException
    // Récupère seulement les commentaires approuvés
    
    public List<Commentaire> findByResourceIdAll(int resourceId) throws SQLException
    // Récupère TOUS les commentaires (admin)
    
    public Commentaire findById(int id) throws SQLException
    
    // UPDATE
    public void update(Commentaire commentaire) throws SQLException
    
    // DELETE
    public void delete(int id) throws SQLException
    
    // MODERATION
    public void approve(int id) throws SQLException
    // Met à jour approved = true
    
    public List<Commentaire> findUnapproved() throws SQLException
    // Récupère les commentaires en attente (pour modérateur)
}
```

---

## 🎯 Services

### ResourceService.java

```java
public class ResourceService {
    private ResourceDAO resourceDAO;
    private CommentaireDAO commentaireDAO;
    
    // Validation
    private void validateResource(Resource resource) throws IllegalArgumentException
    // Valide titre, description, type (3-255, 10-5000, enum)
    
    // Services
    public void createResource(Resource resource) throws SQLException
    public List<Resource> getAllResources() throws SQLException
    public Resource getResourceById(int id) throws SQLException
    public void updateResource(Resource resource) throws SQLException
    public void deleteResource(int id) throws SQLException
    // Supprime les commentaires associés en cascade
    
    public List<Resource> searchResources(String query) throws SQLException
}
```

**Logique Métier:**
- Valide toutes les données entrantes
- Gère la suppression en cascade
- Trims les inputs (remove whitespace)

---

### CommentaireService.java

```java
public class CommentaireService {
    private CommentaireDAO commentaireDAO;
    
    // Validation
    private void validateCommentaire(Commentaire comment) throws IllegalArgumentException
    // Valide: nom, email (regex), contenu, rating (1-5)
    
    // Services
    public void createCommentaire(Commentaire c) throws SQLException
    // Crée avec approved=false par défaut
    
    public void updateCommentaire(Commentaire c) throws SQLException
    public void deleteCommentaire(int id) throws SQLException
    public void approveCommentaire(int id) throws SQLException
    
    public List<Commentaire> getCommentairesByResource(int resourceId) throws SQLException
    // Seulement approved=true
    
    public List<Commentaire> getCommentairesByResourceAll(int resourceId) throws SQLException
    // Tous (admin view)
    
    public List<Commentaire> getUnapprovedCommentaires() throws SQLException
}
```

---

## 🎮 Controllers

### ResourceListController.java

**Événements:**
- `initialize()`: Charge le tableau et setup listeners
- `handleNewResource()`: Ouvre formulaire vide
- `handleEditResource()`: Ouvre formulaire pré-rempli
- `handleViewResource()`: Ouvre détail window
- `handleDeleteResource()`: Supprime avec confirmation

**TableView Columns:**
- id, title, type

**Interactions:**
- Recherche en temps réel via TextProperty listener
- Double-click sur row (optionnel)
- Sélection pour actions

---

### ResourceFormController.java

**Champs:**
- titleField (TextField)
- descriptionField (TextArea)
- typeCombo (ComboBox: article/video)
- filePathField, videoUrlField, imageUrlField (TextFields)

**Validations UI:**
- Check vides (showWarning)
- Appelle ResourceService pour validation métier

**OnSave Callback:**
- Prévient le parent que la ressource est sauvegardée
- Parent rafraîchit la liste
- Ferme la window

---

### ResourceDetailController.java

**Affichage:**
- En-tête ressource (title, type, created_at)
- Description complète
- URL vidéo
- **TableView des commentaires approuvés**

**Actions:**
- `handleAddComment()`: Ouvre CommentaireFormController
- `handleApproveComment()`: Approuve le comment sélectionné
- `handleDeleteComment()`: Supprime avec confirmation

**Chargement:**
- Charge détail ressource
- Charge commentaires approuvés
- Refresh possible via button

---

### CommentaireFormController.java

**Champs:**
- authorNameField (TextField)
- authorEmailField (TextField)
- contentArea (TextArea)
- ratingSpinner (Spinner 1-5)

**Validations:**
- CommentaireService.validateCommentaire()
- Affiche les erreurs en AlertDialog

**OnSave:**
- Crée Commentaire avec approved=false
- Affiche message "en attente d'approbation"
- Callback notifie parent
- Ferme window

---

## 🎨 FXML UI

### resource_list.fxml
```
VBox
├── HBox (Search bar + clear button)
├── TableView (id, title, type)
└── HBox (Buttons: New, View, Edit, Delete)
    ├─ "➕ Nouveau" → create
    ├─ "👁️ Consulter" → show detail
    ├─ "✏️ Modifier" → edit
    └─ "❌ Supprimer" → delete with confirm
```

### resource_form.fxml
```
VBox
├── Label + TextField (Title)
├── Label + TextArea (Description)
├── Label + ComboBox (Type: article/video)
├── Label + TextField (FilePath)
├── Label + TextField (VideoUrl)
├── Label + TextField (ImageUrl)
└── HBox (Buttons: Save, Cancel)
```

### resource_detail.fxml
```
VBox
├── VBox (Header: title, type, created_at)
├── Label + TextArea (Description)
├── Label + TextArea (VideoUrl)
├── Separator
├── Label "💬 Commentaires"
├── TableView (author, rating, content) - APPROVED ONLY
└── HBox (Buttons: Add, Approve, Delete)
```

### commentaire_form.fxml
```
VBox
├── Label + TextField (Author name)
├── Label + TextField (Author email)
├── Label + TextArea (Content)
├── Label + Spinner (Rating 1-5)
├── Label (Moderation warning)
└── HBox (Buttons: Publish, Cancel)
```

---

## 🔄 Flux de Données

### CREATE Resource
```
User Input (FXML form)
    ↓
ResourceFormController.handleSave()
    ↓
ResourceService.createResource(resource)
    ├─ Validate data
    └─ Throws IllegalArgumentException if invalid
    ↓
ResourceDAO.create(resource)
    ├─ PreparedStatement INSERT
    ├─ Retrieve generated ID
    └─ Set resource.id
    ↓
Database updated
    ↓
Callback notifies parent
    ↓
ResourceListController refreshes table
```

---

### READ Resource Detail
```
User clicks "👁️ Consulter"
    ↓
ResourceListController.openResourceDetailWindow(resource)
    ├─ Load & show FXML
    └─ Pass resource to controller
    ↓
ResourceDetailController.setResource(resource)
    ├─ Populate UI with resource data
    ├─ Call loadResourceData()
    └─ Call loadCommentaires()
    ↓
CommentaireService.getCommentairesByResource(id)
    ├─ SQL: SELECT WHERE approved=true
    └─ Return List<Commentaire>
    ↓
TableView populated with comments
```

---

### UPDATE Resource
```
User clicks "✏️ Modifier"
    ↓
ResourceListController.openResourceEditWindow(selected)
    ├─ Load form FXML
    └─ ResourceFormController.setResource(resource)
    ↓
ResourceFormController populates fields
    ↓
User modifies fields + handleSave()
    ↓
ResourceService.updateResource(resource)
    ├─ Validate
    └─ ResourceDAO.update()
    ↓
Database updated (created_at unchanged)
    ↓
Callback → Parent refreshes
```

---

### DELETE Resource
```
User selects resource + clicks "❌ Supprimer"
    ↓
Confirmation dialog
    ↓
ResourceService.deleteResource(id)
    ├─ Get all related commentaires
    ├─ Delete each commentaire
    └─ Delete resource
    ↓
Database: CASCADE deletes
    ↓
ResourceListController.loadResources()
    ↓
Table refreshed
```

---

## 📊 Cas d'Usage

### UC1: Créer une Ressource
**Acteurs:** Admin  
**Flux:**
1. Clique "➕ Nouveau"
2. Remplit formulaire
3. Clique "💾 Enregistrer"
4. Validation passe → Ressource créée
5. Liste mise à jour
6. Confirmation affichée

---

### UC2: Modérer un Commentaire
**Acteurs:** Admin  
**Flux:**
1. Ouvre détail ressource
2. Sélectionne commentaire non approuvé
3. Clique "✅ Approuver"
4. Commentaire devient visible aux users
5. Table mise à jour

---

### UC3: Rechercher une Ressource
**Acteurs:** Tous  
**Flux:**
1. Tape dans la barre recherche
2. TableView actualise en temps réel
3. Affiche ressources filtrées
4. Peut cliquer "Effacer" pour reset

---

## 🔗 Dépendances Entre Classes

```
Main.java
  ├─ ResourceListController (FXML open)
  │   ├─ ResourceService
  │   ├─ ResourceFormController
  │   └─ ResourceDetailController
  │       ├─ CommentaireService
  │       └─ CommentaireFormController
  │
Main.java (openResourcesWindow)
  └─ Load resource_list.fxml
      └─ ResourceListController (voir ci-dessus)
```

---

## 📝 État de Modération

```
States:
┌─────────────────────┐
│   User Posts       │ ← approved = false
│  Commentaire       │
└──────────┬──────────┘
           │
    Admin Reviews
           │
      ┌────┴────┐
      │         │
   REJECT    APPROVE
      │         │
    DELETE  ┌───▼────┐
            │approved│
            │ = true │
            │        │
            │Visible │
            │Users   │
            └────────┘
```

---

**Version**: 1.0  
**Java**: 17  
**JavaFX**: 17  
**MySQL**: 8.0+  
**Date**: 13 Avril 2026
