# MasyaLink

Private 1-to-1 Android messenger prototype.

## Current version

`v1.3-fixed-profiles`

The app now uses two fixed profiles instead of registration:

- Profile A
- Profile B

There is no room-code field, no username/PIN login, and no account deletion screen. A user chooses one fixed profile on first launch. The selected profile can be renamed later in Settings.

## Features

- Android native app, Kotlin + Jetpack Compose.
- Supabase backend through raw REST/RPC calls using Ktor.
- Two fixed profiles.
- Text messages.
- Replies to messages.
- Delete one message for everyone.
- Clear entire chat.
- Typing indicator.
- Delivered/read receipts.
- Profile display-name editing.
- Logout from this device.
- Automatic anonymous-session recovery after Supabase token expiration.

## Server setup

Before testing this patch, run:

```text
server/sql/SERVER_PATCH_FIXED_PROFILES_V1_3.sql
```

in Supabase SQL Editor.

Earlier patches must already exist on the server:

- typing/read receipts patch
- reply/delete patch

## Build APK locally

```bash
./gradlew assembleDebug
```

APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

Pushing to `main` runs the Android debug APK workflow and uploads the APK as an artifact.
