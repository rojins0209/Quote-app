# Quote App (Kotlin + Firebase + Telegram Scheduling)

A minimal, modern motivation/quote app focused on a standard and professional UI/UX.

## What is implemented

- **Android app (Jetpack Compose)** with a polished daily quote experience:
  - standard Material 3 card layout
  - day switcher (previous/next/today)
  - animated state transitions
  - loading, empty, success, and error states
  - **monospace typography across the app**
- **Welcome + onboarding screen** for first launch with reminder time selection.
- **In-app settings screen** for reminder toggle and reminder time updates.
- **No signup/auth in app** — app reads quotes from Firestore.
- **Home screen widget** shows the last loaded quote and opens the app when tapped.
- **Daily notification reminder** schedules a local notification at onboarding/settings-selected time with cached quote preview text.
- **Firebase Cloud Function Telegram webhook** with owner-only scheduling and safer command parsing.
- **Firestore rules** with public read-only access for `quotes/*` and write denied to client SDKs.

## Android architecture

- `MainActivity` hosts onboarding flow + settings + quote route and notification setup.
- `QuoteHomeWidgetProvider` renders a standard app widget with cached quote text.
- `QuoteViewModel` owns selected date and async UI state.
- `NotificationHelper` stores/schedules reminder time and enable state.
- UI is split into route/screen/components:
  - `ui/screen/DailyQuoteScreen.kt`
  - `ui/screen/OnboardingScreen.kt`
  - `ui/screen/SettingsScreen.kt`
  - `ui/components/QuoteComponents.kt`
  - `ui/state/QuoteUiState.kt`

## UI inspiration

- See `docs/ui-inspiration-analysis.md` for concise analysis and concrete design decisions.

## Telegram Commands (owner-only)

Set both env vars in Firebase Functions:
- `OWNER_TELEGRAM_CHAT_ID`
- `TELEGRAM_BOT_TOKEN`

Commands:
- `/add YYYY-MM-DD | quote text | accent(optional) | author(optional)`
- `/add --force YYYY-MM-DD | quote text | accent(optional) | author(optional)`
- `/update YYYY-MM-DD | quote text | accent(optional) | author(optional)`
- `/preview YYYY-MM-DD`
- `/list [YYYY-MM]`
- `/stats`
- `/delete YYYY-MM-DD`
- `/help`

Notes:
- `/add` rejects duplicates unless `--force` is provided.
- Quote text max length is `600` characters.

## Data model (Firestore)

Collection: `quotes`
Document ID: `YYYY-MM-DD`

Fields:
- `text: string`
- `accentLine: string`
- `author: string`
- `createdAt: server timestamp`
- `source: "telegram" | "telegram-update"`

## Owner run checklist

- Follow `docs/what-you-need-to-do.md` for complete owner-side setup and run instructions.

## Setup

### Android
1. Open `android-app` in Android Studio.
2. Add your `google-services.json` into `android-app/app/`.
3. Build and run.

### Firebase Functions + Firestore Rules
1. `cd firebase-functions`
2. `npm install`
3. `npm run build`
4. Deploy all Firebase resources (functions + rules + indexes):
   - `firebase deploy`
5. Set Telegram webhook URL to the deployed `telegramWebhook` endpoint.

## Next UX upgrades

- Favorite quotes and saved collection.
- Theme presets (calm/night/sepia).
