# Mail and Zoom Setup (Windows)

This project reads configuration from:

- Environment variables (recommended)
- Java system properties (`-D...`)

The code already defaults to MailHog in dev (`localhost:1025`).

## 1) Start MailHog

Run MailHog so SMTP is available on port `1025` and UI on `8025`.

- SMTP: `localhost:1025`
- UI: `http://localhost:8025`

## 2) Configure SMTP (PowerShell)

In the same PowerShell where you launch the app:

```powershell
$env:MAIL_SMTP_HOST = "localhost"
$env:MAIL_SMTP_PORT = "1025"
$env:MAIL_SMTP_AUTH = "false"
$env:MAIL_SMTP_STARTTLS = "false"
$env:MAIL_FROM = "noreply@mindcare.local"
```

Optional (for production SMTP):

```powershell
$env:MAIL_SMTP_USERNAME = "your-username"
$env:MAIL_SMTP_PASSWORD = "your-password"
```

## 3) Configure Zoom API (PowerShell)

Set these for Zoom Server-to-Server OAuth:

```powershell
$env:ZOOM_ACCOUNT_ID = "your_zoom_account_id"
$env:ZOOM_CLIENT_ID = "your_zoom_client_id"
$env:ZOOM_CLIENT_SECRET = "your_zoom_client_secret"
$env:ZOOM_USER_ID = "me"
$env:ZOOM_TIMEZONE = "Africa/Tunis"
```

Security note:

- Do not commit real Zoom secrets in project files.
- If a real secret was committed, rotate it immediately in Zoom Marketplace.

Notes:

- `ZOOM_USER_ID` can stay `me` for many accounts.
- If Zoom vars are missing, acceptance mails are still sent; online appointments just won't include a Zoom link.

## 4) Run the app

Use:

```powershell
.\run.bat
```

`run.bat` already tries several Maven locations automatically.

## 5) Manual test checklist

1. Student creates appointment (`location = in office`):
   - Expected: psychologue receives "pending approval" email in MailHog.

2. Psychologue accepts `in office` appointment:
   - Expected: student + psychologue receive "accepted" email (no Zoom link).

3. Student creates appointment (`location = online`), psychologue accepts:
   - Expected: student + psychologue receive "accepted" email with Zoom URL.

4. Psychologue refuses appointment:
   - Expected: student receives refusal email.

5. Psychologue cancels appointment:
   - Expected: student receives cancellation email.

6. Student cancels accepted appointment:
   - Expected: psychologue receives cancellation email.

## 6) Java property alternative

Instead of env vars, you can pass Java properties:

- Mail: `mail.smtp.host`, `mail.smtp.port`, `mail.smtp.username`, `mail.smtp.password`, `mail.smtp.auth`, `mail.smtp.starttls.enable`, `mail.from`
- Zoom: `zoom.accountId`, `zoom.clientId`, `zoom.clientSecret`, `zoom.userId`, `zoom.timezone`
