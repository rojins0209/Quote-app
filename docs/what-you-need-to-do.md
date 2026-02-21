# What You Need To Do (Owner Setup & Run Guide)

This document lists exactly what you need to do on your side to run the app end-to-end.

---

## 1) Install required tools

### Android side
- **Android Studio** (latest stable)
- **Android SDK 34** (or matching SDK configured by Android Studio)
- A physical Android device OR emulator

### Firebase / backend side
- **Node.js 20+**
- **npm**
- **Firebase CLI**
  - Install: `npm install -g firebase-tools`
- **Telegram account** (to create and operate the bot)

---

## 2) Create Firebase project

1. Go to Firebase Console.
2. Create a new project (or reuse existing).
3. Enable **Cloud Firestore**.
4. Create an **Android app** inside Firebase project:
   - package name should match `com.quoteapp` (or update app config/code if you choose another).
5. Download `google-services.json`.
6. Place it in:
   - `android-app/app/google-services.json`

---

## 3) Deploy Firestore rules/indexes + Functions

From repo root:

1. Login Firebase CLI:
   - `firebase login`
2. Initialize project selection:
   - `firebase use --add`
3. Install function dependencies:
   - `npm --prefix firebase-functions install`
4. Build functions:
   - `npm --prefix firebase-functions run build`
5. Set required env vars for functions:
   - `OWNER_TELEGRAM_CHAT_ID`
   - `TELEGRAM_BOT_TOKEN`

> You can set runtime env vars using Firebase Functions v2 env config (or your preferred secure secret manager approach).

6. Deploy:
   - `firebase deploy`

This deploys:
- Cloud Functions (`firebase-functions/src/index.ts`)
- Firestore rules (`firestore.rules`)
- Firestore indexes (`firestore.indexes.json`)

---

## 4) Create Telegram bot and connect webhook

1. Open Telegram and message **@BotFather**.
2. Create bot with `/newbot` and copy bot token.
3. Get your owner chat ID (your own account ID) and set it in Firebase env as `OWNER_TELEGRAM_CHAT_ID`.
4. After deployment, get `telegramWebhook` HTTPS URL from Firebase output.
5. Set webhook:
   - `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=<YOUR_FUNCTION_URL>`

---

## 5) Add initial quotes from Telegram

Use owner chat with your bot commands:

- `/add YYYY-MM-DD | quote text | accent(optional) | author(optional)`
- `/add --force YYYY-MM-DD | quote text | accent(optional) | author(optional)`
- `/update YYYY-MM-DD | quote text | accent(optional) | author(optional)`
- `/preview YYYY-MM-DD`
- `/list [YYYY-MM]`
- `/stats`
- `/delete YYYY-MM-DD`
- `/help`

---

## 6) Run Android app

1. Open `android-app` folder in Android Studio.
2. Sync Gradle.
3. Run on emulator/device.
4. First launch flow:
   - Complete onboarding
   - Pick reminder time (or continue without reminders)
   - Grant notifications permission on Android 13+

---

## 7) Validate core features

After app launch, check:

1. **Quote fetch**
   - Today’s quote displays if scheduled.
2. **Date switching**
   - Previous/next/today works.
3. **Widget**
   - Add app widget to home screen.
   - Open app to refresh and verify widget quote updates.
4. **Reminder notifications**
   - In Settings, enable reminders and pick a time.
   - Verify local notification appears.
5. **Telegram admin**
   - `/add`, `/preview`, `/list`, `/stats` all respond correctly.

---

## 8) Common issues and fixes

### App builds but no quotes appear
- Check Firestore has documents in `quotes` collection with document IDs like `YYYY-MM-DD`.
- Check device date/timezone.

### Telegram commands do nothing
- Verify webhook URL is set correctly.
- Verify `TELEGRAM_BOT_TOKEN` and `OWNER_TELEGRAM_CHAT_ID` are configured.
- Confirm you are sending commands from the owner account.

### Notifications not showing
- Check notification permission (Android 13+).
- Ensure reminders are enabled in app settings.
- Ensure device battery optimizations are not aggressively restricting alarms.

### Widget not updating
- Open app once to load and cache a quote.
- Re-add widget if launcher cached old state.

---

## 9) Production readiness checklist

- [ ] Firebase project configured
- [ ] `google-services.json` placed in correct path
- [ ] Firestore rules deployed
- [ ] Functions deployed
- [ ] Telegram webhook configured
- [ ] At least 7 future quotes scheduled
- [ ] Tested on one real device
- [ ] Notification permission + reminder flow validated

