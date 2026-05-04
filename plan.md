# Mail and Zoom Implementation Plan

## Goal
Add appointment notifications to the JavaFX app so that:

1. When a student creates an appointment, the psychologue receives an email.
2. When the psychologue accepts or refuses an appointment, the student receives an email.
3. When the psychologue cancels an accepted appointment, the student receives an email.
4. When an appointment is accepted and its existing location value is `online`, both the student and the psychologue receive the acceptance email with the Zoom meeting link.

## Current Code Paths

The current appointment flow is centered in `src/main/java/com/mindcare/services/AppointmentService.java`, with UI actions in:

- `src/main/java/com/mindcare/legacy/client/ContractsLegacyContent.java`
- `src/main/java/com/mindcare/legacy/psychologue/GestionRendezVousLegacyContent.java`
- `src/main/java/com/mindcare/legacy/admin/GestionReservationsLegacyContent.java`

Right now, appointment creation and status changes are handled in the service layer, but there is no mailer service and no Zoom integration.

## Assumptions

- Dev email delivery can continue to use MailHog.
- Production email delivery will use a real SMTP provider later.
- Zoom meetings will be created through the Zoom API inside the mailer flow when the appointment is accepted.
- The existing `location` field already distinguishes `online` from `in office`, so no table changes are needed.

## Phase 1: Mail Infrastructure

1. Add a reusable mail service in the Java codebase.
2. Add SMTP configuration for host, port, sender address, username, and password.
3. Keep MailHog as the local development SMTP target.
4. Make mail sending asynchronous so the JavaFX UI does not freeze.

### Outcome

- One service can send all appointment-related emails.
- Local development keeps working with MailHog.

## Phase 2: Notification Events

1. Send a notification when a student creates a new appointment.
2. Send a notification when a psychologue accepts an appointment.
3. Send a notification when a psychologue refuses an appointment.
4. Send a notification when a psychologue cancels an accepted appointment.
5. Send a notification when a student cancels a permitted appointment, if that action exists in the app flow.

### Recommended trigger points

- Appointment creation: inside `AppointmentService.create(...)` or the controller that submits the form.
- Accept/refuse/cancel actions: inside the methods that update status in `GestionRendezVousLegacyContent` and the corresponding service method.

### Outcome

- Every status change has one clear notification path.
- The mail body can be tailored by event type and recipient role.

## Phase 3: Appointment Data Model

1. Reuse the existing `location` field to detect whether the appointment is online.
2. Do not add new columns to the tables.
3. Keep Zoom data transient for the mail send operation unless later requirements explicitly need persistence.

### Outcome

- The app can distinguish online meetings from physical appointments using the current schema.
- No database migration is required for this part.

## Phase 4: Zoom API Integration

1. Add a Zoom integration service used by the mailer.
2. Authenticate with Zoom using a supported OAuth flow.
3. When an appointment becomes accepted, check the existing `location` value.
4. If the location is `online`, create the Zoom meeting and inject the join URL into the acceptance email.
5. If the location is `in office`, send the normal acceptance email without a Zoom link.

### Important note

Zoom credentials should not be hardcoded in the UI layer. The safer approach is to isolate Zoom calls in the mailer/service layer and keep secrets in environment variables or external config.

### Outcome

- Online appointments get a real meeting link only after approval.
- Both parties receive the same join URL inside the acceptance email.

## Phase 5: Email Templates

1. Create one template per notification type.
2. Keep the subject lines short and explicit.
3. Include appointment date, time, status, and relevant names.
4. Include the Zoom link only in the acceptance email for online appointments.

### Suggested templates

- New appointment pending approval
- Appointment accepted
- Appointment refused
- Appointment cancelled by psychologue
- Appointment cancelled by student, if applicable
- Online appointment accepted with Zoom link

### Outcome

- Messages stay consistent and easy to understand.
- Email content can be localized later if needed.

## Phase 6: Validation

1. Test appointment creation with MailHog.
2. Test psychologue accept/refuse flows.
3. Test student cancellation flows.
4. Test online appointment acceptance with Zoom link generation.
5. Confirm that emails contain the correct recipient, subject, and link.

### Outcome

- The flow is verified end to end before production SMTP or Zoom credentials are finalized.

## Open Questions Before Coding

1. Do you want the Zoom meeting created exactly when the acceptance email is sent, or in a separate mailer step just before sending?
2. Should emails be sent directly from the JavaFX app, or do you want a backend service to handle mail and Zoom securely?
3. Which SMTP provider will you use outside MailHog?

## Proposed Implementation Order

1. Add the mail service and SMTP config.
2. Wire notifications into appointment status changes.
3. Reuse the existing `location` value to detect online appointments.
4. Integrate Zoom meeting creation inside the acceptance mail flow.
5. Test with MailHog and a Zoom sandbox or test account.