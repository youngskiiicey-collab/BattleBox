# Battle Box Fight Club — Dudhnoi, Assam

Native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, **Google Credential Manager**, and **Firebase (Auth, Cloud Firestore in `asia-south1`, App Check, Firebase AI Logic)**.

*App created by DocCoder.exe*

---

## Application Identity
- **App Name:** Battle Box
- **Application ID:** `com.aistudio.battlebox.kmrxqv`
- **Firebase Project ID:** `gen-lang-client-0270493025`
- **Firestore Database ID:** `ai-studio-android-battlebo-f9f1c6c4-69cd-46ca-9bcc-c54ee93a8e1b` (`asia-south1`)
- **Club Location:** Dudhnoi, Assam (`https://maps.app.goo.gl/4ji9NFtZwpXZSiTN9`)
- **Timings (IST):** `05:30–10:00` & `16:30–21:30`

---

## 1. Export & Push to GitHub

1. In Google AI Studio, open the **Settings / Export** menu and choose **Push to GitHub** or **Download ZIP**.
2. If using the downloaded ZIP locally:
   ```bash
   git init
   git add .
   git commit -m "Initial Battle Box Android project"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
3. Pushing to `main` automatically triggers `.github/workflows/android-build.yml`, which builds `app-debug.apk` and uploads it under **GitHub Actions &rarr; Artifacts (`battle-box-debug-apk`)**.

---

## 2. Build & Install APK Locally on Android

### Option A: AI Studio One-Click APK
- Open AI Studio **Settings &rarr; Generate APK / AAB** to download the ready-to-install `.apk` directly to your computer or Android phone.

### Option B: Local Build via Android Studio / CLI
1. Ensure JDK 17 and Android SDK (API 36) are installed.
2. Decode the bundled debug keystore (if not already present):
   ```bash
   base64 -d debug.keystore.base64 > debug.keystore
   ```
   > Note: Keeping this debug keystore preserves the SHA-1 fingerprint registered in `app/google-services.json` for Google Sign-In.
3. Build the APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. Install on a connected Android device (with USB Debugging enabled):
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Or copy `app/build/outputs/apk/debug/app-debug.apk` onto your Android device and tap to install.

### Signed Release APK
Set keystore environment variables before running `./gradlew :app:assembleRelease`:
- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

---

## 3. Deploying on Vercel (Companion Distribution Portal)

Vercel hosts static web applications (`HTML/CSS/JS`) rather than running native Android `.apk` files in the browser. A Battle Box distribution & club portal is included at `public/index.html` with `vercel.json` pre-configured:

```bash
npm i -g vercel
vercel --prod
```

Or connect your GitHub repository directly in the Vercel dashboard — Vercel reads `vercel.json` and publishes `public/index.html` automatically.

---

## 4. Firebase Backend & Firestore Rules (`asia-south1`)

- Security rules: `firestore.rules`
- Database schema: `firebase-blueprint.json`
- Cloud Functions source: `backend/functions/`

Deploy rules & functions via Firebase CLI:
```bash
firebase deploy --only firestore:rules,functions --project gen-lang-client-0270493025
```
