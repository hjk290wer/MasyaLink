# MasyaLink server state

This project is currently connected to a Supabase backend configured through SQL patches.

## Required SQL file

Use only the current consolidated SQL patch:

```text
server/sql/SERVER_PATCH_CURRENT.sql
```

Old SQL patch files were intentionally removed to avoid applying outdated migrations.

## Important server notes

- Do not commit or share any Supabase `service_role` key.
- The client app only uses a public/anon key.
- Table data is disposable during testing, but RPC signatures and policies must remain compatible with the current APK.

## Current app model

- No username/PIN login in the UI.
- Two fixed profiles: `A` and `B`.
- Room code is not shown in the app.
- Profile names can be changed in Settings.
- Chat can be cleared from Settings.
- Individual messages can be replied to and deleted for everyone.
- Typing, delivered, and read states are supported.
