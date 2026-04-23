# 📅 API Calendrier - Structure Complète

## 🎯 Organisation des APIs

L'API Calendrier est organisée en **deux parties** : FRONT OFFICE (Utilisateur) et BACK OFFICE (Admin).

---

## 🔹 PART 1: FRONT OFFICE (USER) - Consultation et Interaction

### 1️⃣ Voir le calendrier complet
```
GET /api/calendar?mode=month&date=2026-04-23
```
**Réponse:**
```json
{
  "type": "calendar",
  "mode": "month",
  "date": "2026-04-23",
  "rangeStart": "2026-04-01",
  "rangeEnd": "2026-04-30",
  "events": [...]
}
```

### 2️⃣ Voir les événements d'un jour
```
GET /api/calendar/day?date=2026-04-23
```
**Réponse:**
```json
{
  "type": "day_view",
  "date": "2026-04-23",
  "events": [...]
}
```

### 3️⃣ Planning personnel
```
GET /api/calendar/user/{userId}?mode=month&date=2026-04-23
```
**Réponse:**
```json
{
  "type": "user_planning",
  "userId": 1,
  "mode": "month",
  "date": "2026-04-23",
  "events": [...]
}
```

### 4️⃣ Voir disponibilités (Free/Busy)
```
GET /api/calendar/free-busy?userId=1&date=2026-04-23
```
**Réponse:**
```json
{
  "type": "free_busy",
  "userId": 1,
  "date": "2026-04-23",
  "availableSlots": [
    {
      "start": "09:00",
      "end": "10:00",
      "available": "true"
    },
    {
      "start": "14:00",
      "end": "15:00",
      "available": "true"
    }
  ],
  "busyTimes": [...]
}
```

### 5️⃣ Gérer les rappels
```
GET /api/calendar/reminders?userId=1
```
**Réponse:**
```json
{
  "type": "reminders",
  "userId": 1,
  "reminders": [
    {
      "eventId": 5,
      "minutesBefore": 30,
      "enabled": true
    }
  ]
}
```

---

## 🔹 PART 2: BACK OFFICE (ADMIN) - Gestion et Optimisation

### 1️⃣ Proposer des créneaux libres
```
POST /api/calendar/admin/suggest-slot
```
**Body:**
```json
{
  "title": "Réunion Team",
  "durationMinutes": 60,
  "preferredDate": "2026-04-25"
}
```
**Réponse:**
```json
{
  "type": "suggested_slots",
  "query": "Réunion Team",
  "slots": [
    {
      "date": "2026-04-25",
      "time": "09:00",
      "duration": "60"
    },
    {
      "date": "2026-04-26",
      "time": "10:00",
      "duration": "60"
    }
  ]
}
```

### 2️⃣ Vérifier les conflits
```
POST /api/calendar/admin/conflicts
```
**Body:**
```json
{
  "titre": "Conférence",
  "dateEvent": "2026-04-24T14:00:00",
  "lieu": "Salle A",
  "capacite": 50,
  "durationMinutes": 120
}
```
**Réponse:**
```json
{
  "type": "conflict_check",
  "hasConflicts": false,
  "count": 0,
  "conflicts": []
}
```

### 3️⃣ Envoyer tous les rappels
```
POST /api/calendar/admin/reminders/send
```
**Réponse:**
```json
{
  "type": "reminders_sent",
  "message": "Reminders sent successfully"
}
```

---

## 📊 Tableau Récapitulatif

| Endpoint | Méthode | Rôle | Fonction |
|----------|---------|------|----------|
| `/api/calendar` | GET | PUBLIC | Voir calendrier complet |
| `/api/calendar/day` | GET | USER | Voir jour spécifique |
| `/api/calendar/user/{id}` | GET | USER | Planning personnel |
| `/api/calendar/free-busy` | GET | USER | Disponibilités |
| `/api/calendar/reminders` | GET | USER | Gérer rappels |
| `/api/calendar/admin/suggest-slot` | POST | ADMIN | Proposer créneaux |
| `/api/calendar/admin/conflicts` | POST | ADMIN | Vérifier conflits |
| `/api/calendar/admin/reminders/send` | POST | ADMIN | Envoyer rappels |

---

## 🗂️ Structure du Code

```
org.example
├── api/
│   └── ApiServer.java
│       ├── CalendarHandler
│       │   ├── handleGetCalendar()
│       │   │   ├── /api/calendar → Vue complète
│       │   │   ├── /api/calendar/day → Jour spécifique
│       │   │   ├── /api/calendar/user/{id} → Planning perso
│       │   │   ├── /api/calendar/free-busy → Disponibilités
│       │   │   └── /api/calendar/reminders → Rappels
│       │   ├── handlePostCalendar()
│       │   │   ├── /api/calendar/admin/suggest-slot → Proposer créneaux
│       │   │   ├── /api/calendar/admin/conflicts → Vérifier conflits
│       │   │   └── /api/calendar/admin/reminders/send → Envoyer rappels
│       │   └── Helpers
│       │       ├── calculateAvailableSlots()
│       │       ├── toEventMap()
│       │       └── payloadToEvent()
│
├── event/
│   └── ReminderService.java
│       ├── getUserReminders()
│       └── sendAllReminders()
```

---

## 💡 Utilisation

### Exemple 1: Récupérer le calendrier du mois actuel
```bash
curl "http://localhost:8080/api/calendar?mode=month"
```

### Exemple 2: Récupérer le planning d'un utilisateur
```bash
curl "http://localhost:8080/api/calendar/user/1?mode=month"
```

### Exemple 3: Voir les créneaux libres
```bash
curl "http://localhost:8080/api/calendar/free-busy?userId=1&date=2026-04-23"
```

### Exemple 4: Proposer des créneaux (ADMIN)
```bash
curl -X POST "http://localhost:8080/api/calendar/admin/suggest-slot" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Réunion",
    "durationMinutes": 60
  }'
```

### Exemple 5: Envoyer les rappels (ADMIN)
```bash
curl -X POST "http://localhost:8080/api/calendar/admin/reminders/send"
```

---

## ✅ Fonctionnalités

### FRONT OFFICE
- ✔️ Consulter le calendrier global
- ✔️ Voir les événements d'une journée
- ✔️ Afficher le planning personnel
- ✔️ Visualiser les créneaux libres
- ✔️ Gérer les rappels personnels

### BACK OFFICE  
- ✔️ Proposer des créneaux optimaux
- ✔️ Détecter les conflits d'événements
- ✔️ Envoyer les rappels aux utilisateurs
- ✔️ Vue globale du calendrier
- ✔️ Organiser et contrôler les événements
