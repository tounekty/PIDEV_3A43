# 🧠 MindCare - Plateforme de Gestion Médicale

---

## 📌 Description

MindCare est une application JavaFX desktop complète pour la gestion des rendez-vous médicaux, dossiers patients et ressources éducatives.

---

## Description metier

MindCare centralise les besoins d'un cabinet universitaire de psychologie:

- gestion des comptes et des roles (etudiant, psychologue, admin)
- prise de rendez-vous (cabinet / en ligne)
- suivi via dossier patient
- moderation IA des commentaires
- statistiques d'usage
- integration Zoom pour les rendez-vous en ligne

---

## 🚀 Technologies Utilisées

- ☕ Java 17
- 🎨 JavaFX
- 🗄️ MySQL
- 📦 Maven
- 🔐 BCrypt
- 📧 SMTP
- 🎥 Zoom API
- 🤖 Ollama AI

---

## 🎯 Fonctionnalités

### 👤 Client
- ✅ Prendre un rendez-vous
- ✅ Voir ses rendez-vous
- ✅ Gérer son dossier patient

### 🏥 Psychologue
- ✅ Accepter ou refuser des rendez-vous
- ✅ Consulter les dossiers patients
- ✅ Voir les statistiques

### 🔑 Administrateur
- ✅ Gestion des utilisateurs
- ✅ Dashboard global
- ✅ Gestion complète du système

---

## Installation et configuration

1. Cloner le depot

```bash
git clone https://github.com/tounekty/PIDEV_3A43.git
cd PIDEV_3A43
```

2. Installer les dependances

```bash
mvn clean install
```

3. Configurer l'environnement

- Mettre a jour les parametres dans `src/main/resources/application.properties`
- Renseigner les variables SMTP/Zoom si necessaire

4. Initialiser la base

```bash
mysql -u <user> -p <db_name> < schema.sql
```

5. Lancer l'application

```bash
run.bat
```

---

## Utilisation (scenarios)

- inscription / connexion utilisateur
- creation et validation d'un rendez-vous
- affichage d'un dossier patient
- generation d'insights IA
- creation auto d'un lien Zoom sur un rendez-vous en ligne
- visualisation des statistiques administrateur

---

## 📁 Structure du Projet

```plaintext
src/
├── controller/
├── dao/
├── entities/
├── service/
├── utils/
└── view/
```

---

## Equipe et contributions

- Tounekty Haythem
- roles: Chef de projet
- liens GitHub: https://github.com/tounekty
- --------------------------
- Ben Brahim Mohamed Aziz
- roles: Dev backend / base de donnees
- liens GitHub: https://github.com/aziz98798465
- --------------------------
- Ahmed Omri
- roles: Dev frontend JavaFX / UI
- liens GitHub: https://github.com/ahmedomridev
- --------------------------
- Abdellaoui Nader
- roles: Dev services / integration API
- liens GitHub: https://github.com/nader0abdellaoui
- --------------------------
- Nawress Hichri
- roles:QA / tests et validation
- liens GitHub: https://github.com/nawress01
- --------------------------
- Laarousi Sarah
- roles: Documentation / support
- liens GitHub: https://github.com/sarahlaroussi

---

## 📜 Licence

Projet académique ESPRIT — PIDEV 2026
