# 🚀 QUICK START - Démarrage Rapide

## ⏱️ 5 Minutes pour Commencer

### 1️⃣ Préparation (1 min)

```bash
# Assurez-vous que MySQL est lancé
mysql -u root -p

# Dans MySQL:
CREATE DATABASE mindcare;
USE mindcare;
SOURCE schema.sql;
EXIT;
```

### 2️⃣ Lancer l'App (2 min)

```bash
cd c:\Users\pc\Downloads\java\projetjava
mvn clean compile javafx:run
```

**Attendez** que la fenêtre s'ouvre (peut prendre 10-20s)

### 3️⃣ Se Connecter (1 min)

```
Username: admin
Password: admin123
```

Cliquez **"Se connecter"**

### 4️⃣ Accéder au Module Ressources (1 min)

Dans l'application principale:
- Cherchez le bouton **"📚 Ressources"** dans le haut
- Cliquez dessus
- **Une nouvelle fenêtre s'ouvre!**

Vous êtes prêt! 🎉

---

## 🎯 Premiers Pas

### Pour Créer une Ressource:
1. Cliquez **"➕ Nouveau"**
2. Remplissez:
   - **Titre**: ex: "Introduction à JavaFX"
   - **Description**: ex: "Un guide complet pour débuter avec JavaFX"
   - **Type**: choisissez "article" ou "video"
3. Cliquez **"💾 Enregistrer"**
4. ✅ Ressource créée!

### Pour Lire les Ressources:
1. Le tableau affiche automatiquement toutes les ressources
2. Pour voir les détails: cliquez **"👁️ Consulter"**
3. Vous voyez tous les commentaires approuvés

### Pour Modifier:
1. Sélectionnez une ressource dans le tableau
2. Cliquez **"✏️ Modifier"**
3. Changez les champs
4. Cliquez **"💾 Enregistrer"**

### Pour Supprimer:
1. Sélectionnez une ressource
2. Cliquez **"❌ Supprimer"**
3. Confirmez

### Pour Chercher:
1. Tapez dans la barre **"Rechercher:"**
2. Le tableau se met à jour en temps réel
3. Cherche dans titre ET description

---

## 💬 Gérer les Commentaires

### Voir les Commentaires:
1. Cliquez **"👁️ Consulter"** sur une ressource
2. Tableau affiche les commentaires approuvés
3. Colones: **Auteur** | **Note (étoiles)** | **Contenu**

### Ajouter un Commentaire:
1. Dans la page détail, cliquez **"➕ Ajouter"**
2. Remplissez:
   - **Nom**: votre nom
   - **Email**: email valide
   - **Commentaire**: 5-2000 caractères
   - **Note**: sélectionnez 1-5 étoiles
3. Cliquez **"📤 Publier"**
4. 📢 Message: *"Commentaire en attente d'approbation"*

### Approuver Commentaire (Admin):
1. Dans le détail ressource
2. Sélectionnez le commentaire
3. Cliquez **"✅ Approuver"**
4. ✅ Commentaire maintenant visible!

### Supprimer Commentaire:
1. Sélectionnez le commentaire
2. Cliquez **"❌ Supprimer"**
3. Confirmez

---

## 🔴 Dépannage Rapide

| Problème | Solution |
|----------|----------|
| "Connection refused" | Lancez MySQL: `mysql -u root -p` |
| "Unknown database" | Créez la BD: `CREATE DATABASE mindcare;` |
| Application ne s'ouvre pas | Attendez 20 secondes (parois lent) |
| Erreur compil | Lancez: `mvn clean compile` |
| Pas de données | Exécutez schema.sql |

---

## 📚 Documentation Complète

Pour plus d'informations:
- **[GUIDE_UTILISATION.md](GUIDE_UTILISATION.md)** - Guide détaillé
- **[DOCUMENTATION_TECHNIQUE.md](DOCUMENTATION_TECHNIQUE.md)** - Pour développeurs
- **[INDEX.md](INDEX.md)** - Localisation fichiers

---

## ✅ C'est Tout!

Vous êtes maintenant prêt à gérer vos **Ressources** et **Commentaires** avec JavaFX! 

**Questions?** Consultez la documentation complète.

**Bonne utilisation!** 🎉

---

*Créé le: 13 Avril 2026*
