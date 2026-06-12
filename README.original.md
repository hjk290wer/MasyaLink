# MasyaLink - Private 1-to-1 Android Messenger

MasyaLink is a native Android private 1-to-1 text messaging client built with **Kotlin** and **Jetpack Compose** using the **MVVM** architecture pattern. It connects directly to a prepared Supabase room-based messaging backend using a raw HTTP/REST protocol implemented via the **Ktor HTTP client** and **Kotlinx Serialization**.

---

## 🚀 Key Features

### Implemented
- **Anonymous Session Initialization**: Securely signs up/in anonymously directly through raw REST calls to the Supabase auth backend.
- **Dynamic Peer Pairing**: Connects to specific private chat sessions using customized Room Codes (defaulting to `pink-masya-tema-mili`).
- **Real-Time Polling Message Thread**: Queries for messages in the workspace room every 2 seconds without duplicates, automatically auto-scrolling to the latest incoming chat bubbles.
- **Micro-Presence Sync Heartbeats**: Dispatches persistent background heartbeat pings every 15 seconds while chatting and syncs peer online statuses in the top bar.
- **Modular Design & Separation of Concerns**: Pure domain usecases, reusable UI components, and complete color, typography, spacing, and shape tokens.

### Future Scope (Not Implemented Yet)
- Voice messages
- Attachment files sharing
- Circular video notes
- Push notifications / FCM support
- Branded romantic graphic design presets

---

## 📂 Project Architecture (MVVM)

```
app/src/main/java/com/shiroyama/messenger/
├── MainActivity.kt                  # Main launcher activity
├── MessengerApplication.kt          # Application base class
├── core/
│   ├── config/AppConfig.kt          # Backend keys, URLs, and Room defaults
│   ├── network/
│   │   ├── HttpClientProvider.kt    # Ktor HTTP client builder configuration
│   │   └── SupabaseRestClient.kt    # Low-level REST/RPC API queries
│   ├── storage/
│   │   └── LocalSessionStorage.kt  # Synchronous session preference storage
│   └── util/
│       └── DateTimeUtils.kt         # ISO time formatters and presence checkers
├── data/
│   ├── dto/                         # Request/Response serializable schemas
│   │   ├── AuthResponseDto.kt
│   │   ├── DeviceDto.kt
│   │   ├── MessageDto.kt
│   │   └── RegisterDeviceDto.kt
│   └── repository/
│       └── MessengerRepositoryImpl.kt # Repository implementation
├── domain/
│   ├── model/                       # Domain entities
│   │   ├── Device.kt
│   │   ├── LocalSession.kt
│   │   └── Message.kt
│   ├── repository/
│   │   └── MessengerRepository.kt   # Repository interfaces
│   └── usecase/                     # Decoupled business logic units
│       ├── LoadDevicesUseCase.kt
│       ├── LoadMessagesUseCase.kt
│       ├── PairDeviceUseCase.kt
│       ├── SendTextMessageUseCase.kt
│       └── TouchPresenceUseCase.kt
└── ui/
    ├── components/                 # Reusable Compose widgets
    │   ├── ChatInputBar.kt
    │   ├── MessageBubble.kt
    │   └── StatusDot.kt
    ├── navigation/
    │   └── AppNavigation.kt        # Jetpack Navigation graphs
    ├── screens/                    # MVVM view presenters
    │   ├── chat/ChatScreen.kt & ChatViewModel.kt
    │   ├── pairing/PairingScreen.kt & PairingViewModel.kt
    │   ├── settings/SettingsScreen.kt
    │   └── splash/SplashScreen.kt
    └── theme/                      # Visual custom tokens
        ├── AppTheme.kt
        ├── ColorTokens.kt
        ├── TypographyTokens.kt
        ├── SpacingTokens.kt
        └── ShapeTokens.kt
```

---

## 🛠️ How to open the project in Android Studio

1. Open **Android Studio** (Hedgehog 2023.1.1 or newer is highly recommended).
2. Select **File > Open** (or **Open an existing Android Studio project** from the wizard).
3. Select the folder where this project is downloaded (ensure the selected root directory contains `settings.gradle.kts`, `build.gradle.kts` and the `app/` subfolder).
4. Wait for Android Studio to sync the Gradle configuration, download required SDK platforms, and index the workspace.

---

## 📦 How to build the APK

### Via Gradle Wrapper (Command Line)
In the project directory terminal, type correct execution parameters:

*On Linux / macOS:*
```bash
./gradlew assembleDebug
```

*On Windows:*
```cmd
gradlew.bat assembleDebug
```

Once compilation completes, the debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Via Android Studio UI
1. Select the build configuration dropdown next to the Run button on the main toolbar and select `:app`.
2. Go to the top bar menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Once completed, a pop-up confirmation at the bottom-right corner of Android Studio will appear with a clickable link pointing to standard build outputs. Click on **Locate** to retrieve the compiled APK.

## Accounts patch

This build uses username + PIN as a recoverable chat account:

- Login screen asks for Room Code, Username and PIN.
- Same Username + PIN restores the same account after app reinstall / data clear.
- Messages are linked to `sender_account_id` when backend supports it.
- Settings contains:
  - Log out of account: clears local session and calls `logout_device` if available. Account and messages remain.
  - Delete account and my messages: calls `delete_account_and_messages`, then clears local session.
- If the account/device is deleted remotely, ChatScreen clears local session and returns to pairing screen.

Required backend RPC functions:

- `login_or_create_account(p_room_code, p_username, p_pin, p_device_name, p_fcm_token)`
- `delete_account_and_messages(p_account_id, p_pin)`
- `logout_device(p_device_id)`
- `send_message(...)`
- `touch_device_presence(p_device_id)`

## v1.2 Reply/Delete patch

Before building this patch, execute `SERVER_PATCH_REPLY_DELETE_V1_2.sql` in Supabase SQL Editor.

Added:
- tap a message to reply to it;
- reply preview above the input field;
- reply preview inside message bubbles;
- long-press a message to delete it for everyone;
- `send_message_v2` RPC for reply metadata;
- `delete_message_for_everyone` RPC.

Current behavior:
- deleting removes the message row from the chat for both accounts;
- replies keep a text/username snapshot, so a reply can still show context even if the original message is later deleted.
