# 📚 Guide Complet - CRUD Ressources et Commentaires en JavaFX

## 🎯 Objectif
Ce document explique comment utiliser et configurer le système CRUD pour les **Ressources** (Articles/Vidéos) et les **Commentaires** intégré en JavaFX.

---

## 🔧 Configuration Initiale

### 1. Préparation de la Base de Données

Avant de lancer l'application, exécutez le script `schema.sql` fourni pour créer les tables:

```bash
mysql -u root -p mindcare < schema.sql
```

**Ou manuellement dans MySQL:**
```sql
-- Assurez-vous que la base de données existe
USE mindcare;

-- Exécutez le contenu de schema.sql (fourni dans le projet)
```

### 2. Configuration de la Connexion

Modifiez si nécessaire l'URL de la base de données dans [org/example/config/DatabaseConnection.java](src/main/java/org/example/config/DatabaseConnection.java):

```java
private static final String URL = "jdbc:mysql://localhost:3306/mindcare";
private static final String USER = "root";
private static final String PASSWORD = "";
```

### 3. Lancer l'Application

```bash
cd projetjava
mvn clean compile javafx:run
```

---

## 🚀 Utilisation du Système

### Accéder au Module Ressources

1. Lancez l'application
2. Connectez-vous (demo: `admin/admin123`)
3. Cliquez sur le bouton **"📚 Ressources"** dans le header
4. Une nouvelle fenêtre s'ouvre avec la gestion des ressources

---

## 📖 Fonctionnalités CRUD

### 1. **LIRE les Ressources** (Read)

#### List View
- Tableau affichant toutes les ressources
- Colonnes: **ID**, **Titre**, **Type**
- **Recherche en temps réel**: Tapez dans le champ pour filtrer par titre ou description

#### Détail d'une Ressource
- Cliquez sur **"👁️ Consulter"** pour voir le détail complet
- Affiche:
  - Titre, type, date de création
  - Description complète
  - URL vidéo (si applicable)
  - **Tous les commentaires approuvés**

---

### 2. **CRÉER une Ressource** (Create)

**Après avoir cliqué "➕ Nouveau":**

Remplissez le formulaire:
| Champ | Type | Validation | Requis |
|-------|------|-----------|--------|
| Titre | Texte | 3-255 caractères | ✅ Oui |
| Description | Texte long | 10-5000 caractères | ✅ Oui |
| Type | Combo | article \| video | ✅ Oui |
| Chemin fichier | Texte | - | ❌ Non |
| URL Vidéo | Texte URL | format valide | ❌ Non |
| URL Image | Texte URL | format valide | ❌ Non |

**Validations côté serveur:**
- Titre: obligatoire, 3-255 caractères
- Description: obligatoire, 10-5000 caractères
- Type: doit être "article" ou "video"
- URLs: format valide si fourni

**Cliquez "💾 Enregistrer"** → La ressource est créée et affichée dans la liste

---

### 3. **MODIFIER une Ressource** (Update)

1. Sélectionnez une ressource dans le tableau
2. Cliquez **"✏️ Modifier"**
3. Le formulaire s'ouvre pré-rempli
4. Modifiez les champs souhaités
5. Cliquez **"💾 Enregistrer"**

**Note**: Les validations s'appliquent comme pour la création

---

### 4. **SUPPRIMER une Ressource** (Delete)

1. Sélectionnez une ressource
2. Cliquez **"❌ Supprimer"**
3. Confirmez la suppression

**⚠️ Important**: 
- Tous les commentaires associés seront aussi supprimés (cascade)
- Cette action est irréversible

---

## 💬 Gestion des Commentaires

### Consulter les Commentaires

1. Ouvrez le détail d'une ressource (**"👁️ Consulter"**)
2. Tableau affichant les commentaires approuvés
3. Colonnes: **Auteur** | **⭐ Note** | **Contenu**

---

### Ajouter un Commentaire

1. Dans la page de détail, cliquez **"➕ Ajouter"**
2. Formulaire de commentaire:

| Champ | Type | Validation | Requis |
|-------|------|-----------|--------|
| Nom | Texte | 2-100 caractères | ✅ |
| Email | Email | Format email valide | ✅ |
| Commentaire | Texte | 5-2000 caractères | ✅ |
| Note | Slider | 1-5 étoiles | ✅ |

3. Cliquez **"📤 Publier"**
4. Message: *"Commentaire en attente d'approbation"*

**Note**: Les commentaires n'apparaissent que s'ils sont approuvés par un modérateur

---

### Approuver/Supprimer Commentaires (Admin)

Dans la page de détail d'une ressource:

- **"✅ Approuver"**: Rend le commentaire visible aux utilisateurs
- **"❌ Supprimer"**: Supprime le commentaire définitivement

---

## 🗂️ Structure du Projet

```
src/main/java/org/example/
├── model/
│   ├── Resource.java           # Modèle ressource
│   └── Commentaire.java        # Modèle commentaire
│
├── dao/
│   ├── ResourceDAO.java        # Accès données ressources
│   └── CommentaireDAO.java     # Accès données commentaires
│
├── service/
│   ├── ResourceService.java    # Logique métier ressources
│   └── CommentaireService.java # Logique métier commentaires
│
├── controller/
│   ├── ResourceListController.java       # Liste des ressources
│   ├── ResourceFormController.java       # Formulaire ressource
│   ├── ResourceDetailController.java     # Détail + commentaires
│   └── CommentaireFormController.java    # Formulaire commentaire
│
└── config/
    └── DatabaseConnection.java # Configuration BD

src/main/resources/org/example/fxml/
├── resource_list.fxml         # Interface liste
├── resource_form.fxml         # Interface formulaire
├── resource_detail.fxml       # Interface détail
└── commentaire_form.fxml      # Interface commentaire
```

---

## 📊 Architecture Technique

### Pattern Utilisé: **MVC + DAO**

```
View (FXML) 
    ↓
Controller (JavaFX)
    ↓
Service (Validation & Logique)
    ↓
DAO (Requêtes SQL)
    ↓
Database (MySQL)
```

**Séparation des responsabilités:**
- **View**: Interface graphique (FXML)
- **Controller**: Gestion des événements UI
- **Service**: Validation et logique métier
- **DAO**: Requêtes SQL et mapping
- **Model**: Entités de données

---

## 🔐 Validations

### Côté Client (UI)
- Vérification des champs vides
- Affichage des messages d'erreur

### Côté Serveur (Service)
- Validation complète des données
- Contrôle des longueurs de texte
- Validation format email
- Validation choix type ressource

---

## 📝 Champs de Base de Données

### Table: resource
```sql
CREATE TABLE resource (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(20) CHECK (type IN ('article', 'video')),
    file_path VARCHAR(255),
    video_url VARCHAR(500),
    image_url VARCHAR(500),
    created_at DATETIME DEFAULT NOW(),
    id_user INT NOT NULL
);
```

### Table: commentaire
```sql
CREATE TABLE commentaire (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_resource INT NOT NULL,
    id_user INT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    author_email VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    created_at DATETIME DEFAULT NOW(),
    approved BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_resource) REFERENCES resource(id) ON DELETE CASCADE
);
```

---

## 🐛 Dépannage

### "Erreur: connexion à la base de données impossible"
- Vérifiez que MySQL est lancé
- Vérifiez les identifiants (user/password)
- Vérifiez l'URL: `localhost:3306`
- Vérifiez que la base **"mindcare"** existe

### "Erreur: ResourceListController not found"
- Vérifiez que le fichier FXML existe
- Vérifiez le chemin: `src/main/resources/org/example/fxml/...`

### "Erreur: package not found"
- Recompiler: `mvn clean compile`
- Vérifier les imports dans les classes

---

## 📦 Dépendances Maven

```xml
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>17</version>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>
```

---

## 🚀 Prochaines Étapes (Optionnel)

1. **Pagination**: Ajouter pagination pour larges listes
2. **Recherche avancée**: Filtres par type, date, rating
3. **Export**: Exporter ressources en PDF/Excel
4. **Upload médias**: Permettre upload d'images/vidéos
5. **Modération OpenAI**: Analyser le contenu des commentaires
6. **Authentification**: Système user complet
7. **Statistiques**: Dashboard avec graphiques

---

## 📞 Support

Pour des questions sur l'implémentation:
- Consultez les commentaires dans le code source
- Vérifiez la structure des entités en base de données
- Testez chaque couche individuellement (Model → DAO → Service → UI)

---

**Version**: 1.0  
**Date**: 13 Avril 2026  
**Status**: ✅ Production Ready
