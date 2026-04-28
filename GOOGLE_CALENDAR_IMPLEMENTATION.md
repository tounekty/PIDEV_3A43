# Google Calendar Integration - Implementation Guide

## ✅ What's Been Done

1. **GoogleOAuthService** - OAuth token management
2. **GoogleCalendarService** - Calendar event creation/update/deletion
3. **GoogleCalendarOAuthHandler** - UI-friendly OAuth flow handler
4. **GoogleCalendarConnectButton** - Ready-to-use UI component
5. **AppointmentNotificationService** - Integrated with calendar sync
6. **Updated Models** - User and Appointment with OAuth fields
7. **.env Configuration** - Google credentials already set up

## 🚀 Integration Steps

### Step 1: Add GoogleCalendarConnectButton to Client Profile

Open [ClientProfileViewController.java](src/main/java/com/mindcare/controller/client/ClientProfileViewController.java):

```java
package com.mindcare.controller.client;

import com.mindcare.components.GoogleCalendarConnectButton;
import com.mindcare.dao.UserDAO;
import com.mindcare.services.GoogleCalendarOAuthHandler;
import com.mindcare.legacy.client.ClientProfileLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class ClientProfileViewController {

    @FXML
    private VBox profileContent; // Add this to your FXML
    
    private int currentUserId; // Set from SessionManager or auth context

    @FXML
    private void initialize() {
        // Load legacy content
        profileContent.getChildren().setAll(new ClientProfileLegacyContent().build());
        
        // Add Google Calendar button
        setupGoogleCalendarIntegration();
    }
    
    private void setupGoogleCalendarIntegration() {
        try {
            UserDAO userDAO = new UserDAO(); // Your DAO instance
            GoogleCalendarOAuthHandler oauthHandler = new GoogleCalendarOAuthHandler(userDAO);
            
            GoogleCalendarConnectButton calendarButton = new GoogleCalendarConnectButton(
                oauthHandler,
                currentUserId
            );
            
            // Add to your profile layout
            profileContent.getChildren().add(calendarButton);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Google Calendar button: " + e.getMessage());
        }
    }
}
```

### Step 2: Database Migration (Optional but Recommended)

Add columns to track OAuth tokens and calendar events:

```sql
-- Add to users table
ALTER TABLE users ADD COLUMN google_refresh_token VARCHAR(512);
ALTER TABLE users ADD COLUMN google_access_token VARCHAR(512);
ALTER TABLE users ADD COLUMN google_token_expiry BIGINT;

-- Add to appointments table
ALTER TABLE appointments ADD COLUMN google_calendar_event_id VARCHAR(255);

-- Create indexes for faster lookups
CREATE INDEX idx_user_google_refresh ON users(google_refresh_token);
CREATE INDEX idx_appointment_google_event ON appointments(google_calendar_event_id);
```

### Step 3: Update UserDAO to Handle OAuth Fields

Add methods to `UserDAO.java`:

```java
public User findById(int userId) {
    // ... existing code ...
    // Make sure these fields are loaded from database:
    user.setGoogleRefreshToken(resultSet.getString("google_refresh_token"));
    user.setGoogleAccessToken(resultSet.getString("google_access_token"));
    user.setGoogleTokenExpiry(resultSet.getLong("google_token_expiry"));
    return user;
}

public void updateUser(User user) {
    // ... existing update code, add:
    String updateSql = "UPDATE users SET ... , google_refresh_token = ?, google_access_token = ?, google_token_expiry = ? WHERE id = ?";
    // Set the OAuth fields in your prepared statement
}
```

### Step 4: Update AppointmentDAO to Save Calendar Event ID

Add method to `AppointmentDAO.java`:

```java
public void saveAppointment(Appointment appointment) {
    // Include google_calendar_event_id in INSERT/UPDATE
    String sql = "INSERT INTO appointments (..., google_calendar_event_id) VALUES (..., ?)";
    // Or update existing appointments
}

public Appointment findById(int appointmentId) {
    // Load google_calendar_event_id from database
    appointment.setGoogleCalendarEventId(resultSet.getString("google_calendar_event_id"));
    return appointment;
}
```

### Step 5: Test the Integration

1. **Run the application**:
   ```bash
   mvn clean javafx:run
   ```

2. **Navigate to Client Profile**

3. **Click "Connect Google Calendar"**

4. **Follow OAuth flow**:
   - Browser opens with Google login
   - Sign in with Google account
   - Grant MindCare permission
   - Copy authorization code
   - Paste into MindCare dialog

5. **Create a test appointment**:
   - Psychologist creates appointment
   - Student accepts
   - Check Google Calendar - appointment should appear!

## 🔄 How It Works in Production

### Appointment Accepted
```
1. Psychologist creates appointment
2. Student accepts appointment
3. AppointmentNotificationService.notifyAppointmentAccepted() called
4. Email sent to both parties
5. syncToGoogleCalendar("create") executes
6. Event created in student's Google Calendar with:
   - Title: "Appointment with [Psychologist Name]"
   - Time: From appointment dateTime
   - Description: Appointment details
   - Attendees: Student email
   - Reminders: 24h (email) + 1h (popup)
```

### Appointment Rescheduled
```
1. Psychologist changes appointment time
2. AppointmentNotificationService.notifyAppointmentTimeChangedByPsychologist() called
3. Email sent
4. syncToGoogleCalendar("update") executes
5. Google Calendar event updated with new time
```

### Appointment Cancelled
```
1. Appointment cancelled
2. AppointmentNotificationService.notifyAppointmentCancelledByPsychologist() called
3. Email sent
4. syncToGoogleCalendar("delete") executes
5. Event deleted from Google Calendar
```

## 🛡️ Error Handling

The system gracefully handles Google Calendar unavailability:

```
✓ If Google Calendar not connected → Silently skip (user still gets email)
✓ If tokens expired → Auto-refresh happens
✓ If network error → Logged as warning, appointment continues normally
✓ If API error → Logged, but email notification still sent
```

## 📋 Production Checklist

- [ ] Database migration scripts tested
- [ ] UserDAO updated to handle OAuth fields
- [ ] AppointmentDAO updated to save calendar event ID
- [ ] GoogleCalendarConnectButton integrated into profile view
- [ ] Tested full OAuth flow with real Google account
- [ ] Tested appointment creation → calendar sync
- [ ] Tested appointment update → calendar sync
- [ ] Tested appointment cancellation → calendar sync
- [ ] Error handling verified (network down, etc.)
- [ ] Tokens properly secured in database
- [ ] `.env` file NOT committed to repository
- [ ] Credentials added to production environment

## 🔐 Security Notes

1. **Never commit `.env` file** - Already in `.gitignore`
2. **Store tokens encrypted** in production database
3. **Use HTTPS** for OAuth callback in production
4. **Validate redirect URIs** in Google Cloud Console
5. **Rotate client secret** regularly
6. **Limit token scope** to Calendar API only
7. **Handle token refresh** automatically (already implemented)

## 📊 Files Created/Modified

```
Created:
  - GoogleOAuthService.java
  - GoogleCalendarService.java
  - GoogleCalendarOAuthHandler.java
  - GoogleCalendarConnectButton.java

Modified:
  - pom.xml (added Google API dependencies)
  - User.java (added OAuth fields)
  - Appointment.java (added calendar event ID)
  - AppointmentNotificationService.java (integrated calendar sync)

Configuration:
  - .env (Google credentials + other configs)
```

## 🆘 Troubleshooting

| Issue | Solution |
|-------|----------|
| "Google Calendar not available" | Check `.env` has correct credentials |
| "Token exchange failed" | Verify redirect URI is `http://localhost:8888/oauth/callback` |
| "Event not appearing in calendar" | Check student email is correct, tokens are valid |
| "Token refresh error" | Refresh token may have expired, ask user to reconnect |
| "ClassNotFoundException for Google API" | Run `mvn clean install` to download dependencies |

## 📞 Support

- Google Calendar API Docs: https://developers.google.com/calendar
- OAuth 2.0 Flow: https://developers.google.com/identity/protocols/oauth2
- MindCare Integration: Check GOOGLE_OAUTH_SETUP.md for detailed setup

---

**Status**: Ready for integration ✅
