# Google Calendar OAuth Integration Guide

## Overview
This implementation adds Google Calendar OAuth 2.0 integration to MindCare, allowing students to have appointments automatically added to their personal Google Calendars.

## Components Created

### 1. **GoogleOAuthService** (`services/GoogleOAuthService.java`)
Handles OAuth token management:
- Generate authorization URLs
- Exchange authorization codes for tokens
- Refresh expired tokens
- Validate access tokens

### 2. **GoogleCalendarService** (`services/GoogleCalendarService.java`)
Creates, updates, and deletes calendar events:
- Create appointment events
- Update appointments
- Delete appointments
- Add reminders (24 hours via email, 1 hour via popup)

### 3. **Updated Models**
- `User.java` - Added OAuth token fields
- `Appointment.java` - Added googleCalendarEventId for tracking

## Setup Instructions

### Step 1: Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project: "MindCare"
3. Enable these APIs:
   - Google Calendar API
   - Google+ API

### Step 2: Create OAuth 2.0 Credentials
1. Go to **Credentials** → **Create Credentials** → **OAuth Client ID**
2. Application type: **Web Application**
3. Authorized redirect URIs: `http://localhost:8888/oauth/callback`
4. Download JSON credentials

### Step 3: Configure Environment Variables
Add to your system environment or `.env` file:
```properties
GOOGLE_CLIENT_ID=your_client_id_here
GOOGLE_CLIENT_SECRET=your_client_secret_here
```

Or in `application.properties`:
```properties
google.client.id=your_client_id_here
google.client.secret=your_client_secret_here
```

### Step 4: Add Maven Dependencies
Already added to `pom.xml`:
- `google-api-client`
- `google-oauth-client-jetty`
- `google-api-services-calendar`
- `gson`

## Usage Examples

### Connect Student's Google Calendar

```java
// 1. Generate authorization URL
GoogleOAuthService oauthService = new GoogleOAuthService();
String authUrl = oauthService.getAuthorizationUrl();

// 2. Direct student to URL (open in browser)
Desktop.getDesktop().browse(new URI(authUrl));

// 3. Student logs in and grants permission
// 4. Capture authorization code from callback
String authCode = captureAuthCodeFromCallback();

// 5. Exchange for tokens
GoogleOAuthService.GoogleOAuthToken token = oauthService.exchangeCodeForTokens(authCode);

// 6. Store in database
user.setGoogleRefreshToken(token.getRefreshToken());
user.setGoogleAccessToken(token.getAccessToken());
user.setGoogleTokenExpiry(token.getExpiry());
userDAO.updateUser(user);
```

### Create Calendar Event When Appointment is Accepted

```java
public void onAppointmentAccepted(Appointment appointment, User student) {
    if (!student.isGoogleCalendarConnected()) {
        return; // Student hasn't connected Google Calendar
    }

    try {
        GoogleOAuthService oauthService = new GoogleOAuthService();
        GoogleCalendarService calendarService = new GoogleCalendarService();

        // Get valid access token (refreshes if needed)
        String accessToken = oauthService.getValidAccessToken(
            student.getGoogleAccessToken(),
            student.getGoogleRefreshToken(),
            student.getGoogleTokenExpiry()
        );

        // Create calendar event
        String eventId = calendarService.createAppointmentEvent(
            accessToken,
            appointment,
            student.getEmail()
        );

        // Store event ID for future updates/deletions
        appointment.setGoogleCalendarEventId(eventId);
        appointmentDAO.updateAppointment(appointment);

        // Update student tokens in case they were refreshed
        student.setGoogleAccessToken(accessToken);
        userDAO.updateUser(student);

    } catch (IOException e) {
        logger.error("Failed to create calendar event", e);
        // Continue anyway - calendar sync is optional
    }
}
```

### Update Calendar Event When Appointment Changes

```java
public void onAppointmentRescheduled(Appointment appointment, User student) {
    if (!student.isGoogleCalendarConnected() || appointment.getGoogleCalendarEventId() == null) {
        return;
    }

    try {
        GoogleOAuthService oauthService = new GoogleOAuthService();
        GoogleCalendarService calendarService = new GoogleCalendarService();

        String accessToken = oauthService.getValidAccessToken(
            student.getGoogleAccessToken(),
            student.getGoogleRefreshToken(),
            student.getGoogleTokenExpiry()
        );

        calendarService.updateAppointmentEvent(
            accessToken,
            appointment.getGoogleCalendarEventId(),
            appointment,
            student.getEmail()
        );

        student.setGoogleAccessToken(accessToken);
        userDAO.updateUser(student);

    } catch (IOException e) {
        logger.error("Failed to update calendar event", e);
    }
}
```

### Delete Calendar Event When Appointment is Cancelled

```java
public void onAppointmentCancelled(Appointment appointment, User student) {
    if (!student.isGoogleCalendarConnected() || appointment.getGoogleCalendarEventId() == null) {
        return;
    }

    try {
        GoogleOAuthService oauthService = new GoogleOAuthService();
        GoogleCalendarService calendarService = new GoogleCalendarService();

        String accessToken = oauthService.getValidAccessToken(
            student.getGoogleAccessToken(),
            student.getGoogleRefreshToken(),
            student.getGoogleTokenExpiry()
        );

        calendarService.deleteAppointmentEvent(
            accessToken,
            appointment.getGoogleCalendarEventId()
        );

        appointment.setGoogleCalendarEventId(null);
        appointmentDAO.updateAppointment(appointment);

        student.setGoogleAccessToken(accessToken);
        userDAO.updateUser(student);

    } catch (IOException e) {
        logger.error("Failed to delete calendar event", e);
    }
}
```

## Integration Points

### 1. **AppointmentNotificationService**
Update to sync with Google Calendar:
```java
public void notifyAppointmentAccepted(Appointment appointment) {
    sendAsync(() -> sendAcceptanceEmail(appointment));
    
    // NEW: Also sync to Google Calendar
    User student = userDAO.findById(appointment.getStudentId());
    syncToGoogleCalendar(appointment, student);
}
```

### 2. **AppointmentService**
Update appointment status changes to sync calendar:
```java
public void updateAppointmentStatus(Appointment appointment, String newStatus) {
    appointment.setStatus(newStatus);
    appointmentDAO.updateAppointment(appointment);
    
    // Sync to Google Calendar if status changed to accepted/cancelled
    User student = userDAO.findById(appointment.getStudentId());
    if ("accepted".equals(newStatus)) {
        onAppointmentAccepted(appointment, student);
    } else if ("cancelled".equals(newStatus)) {
        onAppointmentCancelled(appointment, student);
    }
}
```

## Database Migration (Optional)

If using SQL, add columns to track OAuth tokens and calendar event IDs:

```sql
ALTER TABLE users ADD COLUMN google_refresh_token VARCHAR(512);
ALTER TABLE users ADD COLUMN google_access_token VARCHAR(512);
ALTER TABLE users ADD COLUMN google_token_expiry BIGINT;

ALTER TABLE appointments ADD COLUMN google_calendar_event_id VARCHAR(255);
```

## Features

✅ OAuth 2.0 authorization flow  
✅ Automatic token refresh  
✅ Create appointments in student's calendar  
✅ Update appointments in calendar  
✅ Delete appointments from calendar  
✅ Automatic reminders (24h email + 1h popup)  
✅ Fallback if Google Calendar unavailable  

## Error Handling

All services handle common errors:
- Missing credentials → `IllegalStateException`
- Network errors → `IOException`
- Token expiry → Automatic refresh
- Invalid tokens → Exception thrown to UI

## Security Notes

1. **Never** log tokens
2. Store tokens in **secure database** (encrypted)
3. Use HTTPS in production
4. Validate redirect URIs in Google Cloud Console
5. Store client secret securely (env vars, not in code)

## Testing

```java
@Test
public void testOAuthFlow() {
    GoogleOAuthService service = new GoogleOAuthService();
    String authUrl = service.getAuthorizationUrl();
    assertNotNull(authUrl);
    assertTrue(authUrl.contains("client_id"));
}

@Test
public void testCalendarEventCreation() {
    GoogleCalendarService calService = new GoogleCalendarService();
    Appointment apt = new Appointment();
    apt.setDateTime(LocalDateTime.now().plusDays(1));
    apt.setPsyName("Dr. Smith");
    
    // Would need valid token for actual test
    // String eventId = calService.createAppointmentEvent(token, apt, "student@email.com");
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Credentials not configured" | Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` env vars |
| "Invalid redirect URI" | Ensure callback URL matches Google Cloud configuration |
| "Token refresh failed" | Check if refresh token is valid and not expired |
| "Calendar API not enabled" | Enable Google Calendar API in Google Cloud Console |
| "403 Forbidden" | Student may have revoked access - ask to reconnect |

## Next Steps

1. Implement UI for "Connect Google Calendar" button
2. Add OAuth callback endpoint in your controller
3. Integrate with existing `AppointmentNotificationService`
4. Test with real Google account
5. Deploy to production with proper redirect URI
