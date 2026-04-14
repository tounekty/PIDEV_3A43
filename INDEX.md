# 📑 INDEX - Localisation de Tous les Fichiers

## 🗂️ Où Trouver Chaque Fichier

### 📦 Modèles
```
src/main/java/org/example/model/
├── Resource.java                    (Entité Resource)
└── Commentaire.java                 (Entité Commentaire)
```

### 🗄️ Base de Données (DAOs)
```
src/main/java/org/example/dao/
├── ResourceDAO.java                 (CRUD pour Resource: Create, Read, Update, Delete, Search)
└── CommentaireDAO.java              (CRUD pour Commentaire: Create, Read, Update, Delete, Approve, Unapproved)
```

### ⚙️ Services (Logique Métier)
```
src/main/java/org/example/service/
├── ResourceService.java             (Validation Resource + appel DAO)
└── CommentaireService.java          (Validation Commentaire + appel DAO)
```

### 🎮 Controllers (Logique UI)
```
src/main/java/org/example/controller/
├── ResourceListController.java      (Liste ressources + search, CRUD buttons)
├── ResourceFormController.java      (Formulaire create/edit ressource)
├── ResourceDetailController.java    (Détail ressource + affichage commentaires)
└── CommentaireFormController.java   (Formulaire create commentaire)
```

### 🎨 Interfaces (FXML)
```
src/main/resources/org/example/fxml/
├── resource_list.fxml              (Interface: liste des ressources + boutons CRUD)
├── resource_form.fxml              (Interface: formulaire ressource)
├── resource_detail.fxml            (Interface: détail ressource + commentaires)
└── commentaire_form.fxml           (Interface: formulaire commentaire)
```

### 🗃️ Configuration Existante (Réutilisée)
```
src/main/java/org/example/config/
└── DatabaseConnection.java         (Connection pool MySQL)
```

### 📱 Intégration à l'Application Principale
```
src/main/java/org/example/
└── Main.java                       (Modifié: ajout bouton "📚 Ressources" + openResourcesWindow())
```

### 📋 Configuration du Projet
```
pom.xml                             (Modifié: ajout dépendance javafx-fxml)
```

### 💾 Base de Données
```
schema.sql                          (Script SQL: création tables + données exemples)
```

### 📚 Documentation
```
GUIDE_UTILISATION.md               (Guide complet pour l'utilisateur)
DOCUMENTATION_TECHNIQUE.md         (Documentation technique détaillée)
RESUME.md                          (Résumé de l'implémentation)
INDEX.md                           (Ce fichier)
```

---

## 📂 Structure Complète du Projet

```
projetjava/
├── pom.xml                         ← Configuration Maven (MODIFIÉ)
├── schema.sql                      ← Script BD
│
├── src/
│   └── main/
│       ├── java/
│       │   └── org/example/
│       │       ├── model/          ← Entités
│       │       │   ├── Resource.java
│       │       │   └── Commentaire.java
│       │       │
│       │       ├── dao/            ← Data Access Layer
│       │       │   ├── ResourceDAO.java
│       │       │   └── CommentaireDAO.java
│       │       │
│       │       ├── service/        ← Business Logic
│       │       │   ├── ResourceService.java
│       │       │   └── CommentaireService.java
│       │       │
│       │       ├── controller/     ← UI Controllers
│       │       │   ├── ResourceListController.java
│       │       │   ├── ResourceFormController.java
│       │       │   ├── ResourceDetailController.java
│       │       │   └── CommentaireFormController.java
│       │       │
│       │       ├── config/         ← Existant
│       │       │   ├── DatabaseConnection.java
│       │       │   └── ...
│       │       │
│       │       ├── auth/           ← Existant
│       │       │   ├── AppUser.java
│       │       │   └── AuthService.java
│       │       │
│       │       ├── event/          ← Existant
│       │       │   ├── Event.java
│       │       │   └── EventService.java
│       │       │
│       │       ├── reservation/    ← Existant
│       │       │   ├── ReservationRecord.java
│       │       │   └── ReservationService.java
│       │       │
│       │       ├── Main.java       ← MODIFIÉ
│       │       └── TestJDBC.java   ← Existant
│       │
│       └── resources/
│           └── org/example/
│               ├── fxml/           ← Interfaces (NOUVEAU DOSSIER)
│               │   ├── resource_list.fxml
│               │   ├── resource_form.fxml
│               │   ├── resource_detail.fxml
│               │   └── commentaire_form.fxml
│               │
│               └── ... autres existants
│
├── target/                        ← Généré par Maven
│
└── Documentation/
    ├── GUIDE_UTILISATION.md      ← Guide utilisateur
    ├── DOCUMENTATION_TECHNIQUE.md ← Architecture
    ├── RESUME.md                 ← Résumé
    ├── INDEX.md                  ← Ce fichier
    └── schema.sql                ← Script BD
```

---

## 🔄 Dépendances Entre Fichiers

### Main.java dépend de:
- ResourceListController (FXML loading)
- DatabaseConnection (existant)

### ResourceListController dépend de:
- ResourceService
- ResourceFormController
- ResourceDetailController
- Resource (Model)

### ResourceFormController dépend de:
- ResourceService
- Resource (Model)

### ResourceDetailController dépend de:
- ResourceService
- CommentaireService
- CommentaireFormController
- Resource (Model)
- Commentaire (Model)

### CommentaireFormController dépend de:
- CommentaireService
- Commentaire (Model)

### Services dépendent de:
- DAOs
- Models

### DAOs dépendent de:
- DatabaseConnection
- Models

---

## 🎯 Points d'Entrée

### Pour Lancer l'Application:
1. Start MySQL
2. Run `mvn clean compile javafx:run`
3. Login (demo: admin/admin123)
4. Click "📚 Ressources" button in header

### Pour Modifier:

**Ajouter champ à Resource:**
1. Modifier Resource.java
2. Ajouter getter/setter
3. Modifier ResourceDAO.java (SELECT, INSERT, UPDATE)
4. Modifier ResourceFormController.java (ajouutr Input field)
5. Modifier resource_form.fxml (ajouter UI element)

**Ajouter validation:**
1. Modifier ResourceService.validateResource()
2. Ajouter test dans ResourceFormController

**Modifier la BD:**
1. Update schema.sql
2. Run script en SQL
3. Restart application

---

## 📊 Compteurs

| Type | Nombre | Total Lignes (estimé) |
|------|--------|----------------------|
| Models | 2 | 250 |
| DAOs | 2 | 400 |
| Services | 2 | 150 |
| Controllers | 4 | 600 |
| FXML | 4 | 250 |
| Config/Docs | 3 | 1500 |
| **TOTAL** | **17** | **3150+** |

---

## 🔍 Comment Trouver Quelque Chose

### Je veux modifier l'interface:
→ Allez dans: `src/main/resources/org/example/fxml/*.fxml`

### Je veux ajouter une validation:
→ Allez dans: `src/main/java/org/example/service/ResourceService.java`

### Je veux modifier les requêtes SQL:
→ Allez dans: `src/main/java/org/example/dao/ResourceDAO.java`

### Je veux ajouter un bouton:
→ Allez dans: `src/main/java/org/example/controller/ResourceListController.java`

### Je veux changer la base de données:
→ Allez dans: `src/main/java/org/example/config/DatabaseConnection.java`

---

## ✅ Checklist Avant Utilisation

- [ ] Tous les fichiers copiés au bon endroit
- [ ] Maven dépendances installées (`mvn compile`)
- [ ] MySQL lancé et accessibles
- [ ] Base de données `mindcare` créée
- [ ] schema.sql exécuté
- [ ] Application lancée sans erreurs

---

**Réalisé le**: 13 Avril 2026  
**Version**: 1.0  
**Status**: ✅ Complet
