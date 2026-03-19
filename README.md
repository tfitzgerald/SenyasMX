# SenyasMX
Bi-directional Sign language application for Anfroid phone
# 🤟 Señas MX

**Señas MX** is a free, fully offline Android app that translates between **Mexican Sign Language (LSM)** and **Spanish text** — entirely on your device, with no internet connection required.

---

## Table of Contents

- [How the App Works](#how-the-app-works)
- [Features](#features)
- [Project Structure](#project-structure)
- [Setting Up the Build](#setting-up-the-build)
- [Installing on Your Phone](#installing-on-your-phone)
- [Using the App](#using-the-app)
- [Testing](#testing)
- [Adding Sign Language Videos](#adding-sign-language-videos)
- [Future Feature Upgrades](#future-feature-upgrades)
- [Technical Architecture](#technical-architecture)

---

## How the App Works

Señas MX has two translation modes:

### Mode A — Señas → Texto (Sign to Text)
The phone camera watches you sign. A machine learning classifier analyses your hand and body movements in real time and outputs the matching Spanish phrase. The result is shown on screen and spoken aloud using the phone's built-in text-to-speech engine.

```
Camera (30 fps)
    ↓  sample every 2nd frame
Luminance check (low-light warning)
    ↓
Landmark extraction — 75 body/hand points per frame
    ↓
Sliding window buffer — last 30 frames (~2 seconds)
    ↓
TFLite classifier → 106 probability scores
    ↓
EMA smoother + stability check
    ↓
Spanish text output + spoken audio
```

The classifier runs a **stub model** by default — it cycles through sample phrases so every screen works immediately without a trained model. When a real trained model is available, you drop one file into the project and rebuild.

### Mode B — Texto → Señas (Text to Sign)
You type a Spanish phrase or browse by category. The app plays the matching sign language video clip stored locally on the device. A fuzzy search engine finds the best match even with typos or accented letters.

```
Spanish text input  →  Offline fuzzy search  →  Local MP4 video
Category browser    ↗                           Adjustable speed (0.5× / 1× / 1.5×)
```

---

## Features

| Feature | Detail |
|---------|--------|
| ✈️ 100% offline | Works in Airplane Mode — no internet ever required |
| 🔒 Private | Video never leaves the device, no analytics, no tracking |
| 📷 Live recognition | Camera-based sign detection with confidence meter |
| 🎬 Video playback | Local MP4 clips with speed control and replay |
| 🔍 Fuzzy search | Finds phrases even with typos or missing accents |
| 🗂️ 12 categories | Greetings, Emergency, Medical, Food, Transport, and more |
| 📋 105 phrases | Starter vocabulary covering everyday communication |
| 🔊 Offline TTS | Speaks recognised phrases aloud in Spanish (Mexico) |
| 📜 History | Keeps a log of recognised phrases with timestamps |
| 🌗 Dark mode | Follows system light/dark preference |

---

## Project Structure

```
SenyasMX/
├── .github/
│   └── workflows/
│       └── build.yml               ← GitHub Actions build recipe
├── app/
│   ├── build.gradle.kts            ← App dependencies
│   └── src/main/
│       ├── AndroidManifest.xml     ← Permissions and activity declaration
│       ├── assets/
│       │   ├── phrases.json        ← 105 phrases across 12 categories
│       │   └── videos/             ← MP4 sign videos (add yours here)
│       ├── java/com/lsm/translator/
│       │   ├── MainActivity.kt     ← App entry point and theme
│       │   ├── LSMApplication.kt   ← Startup initialisation
│       │   ├── model/
│       │   │   └── PhraseModels.kt ← All data classes
│       │   ├── service/
│       │   │   ├── LSMClassifier.kt      ← Stub + TFLite classifier, smoother
│       │   │   └── PhraseRepository.kt   ← JSON loader + fuzzy search
│       │   ├── viewmodel/
│       │   │   ├── RecognitionViewModel.kt
│       │   │   └── PlaybackViewModel.kt
│       │   └── ui/
│       │       ├── MainScreen.kt         ← Bottom navigation
│       │       ├── RecognitionScreen.kt  ← Camera + recognition UI
│       │       ├── PlaybackScreen.kt     ← Video playback UI
│       │       └── HistoryScreen.kt      ← Recognition history
│       └── res/values/
│           ├── strings.xml         ← Spanish UI strings
│           └── themes.xml          ← App theme
├── build.gradle.kts                ← Root build file
├── gradle.properties               ← AndroidX and memory settings
└── settings.gradle.kts             ← Project name and module setup
```

---

## Setting Up the Build

You do not need to install anything on your computer. The app is compiled entirely on GitHub's servers using **GitHub Actions**.

### Step 1 — Create a GitHub account
Go to [github.com](https://github.com) and sign up for a free account if you don't have one.

### Step 2 — Create a new repository
- Click **+** → **New repository**
- Name it `SenyasMX`
- Set visibility to **Public**
- Check **Add a README file**
- Click **Create repository**

### Step 3 — Upload the project files
- Download the project zip file provided
- Extract it on your computer — you will get a folder containing `app/`, `.github/`, `build.gradle.kts`, etc.
- On GitHub, click **Add file → Upload files**
- Open the extracted folder, select **everything inside it** (Ctrl+A / Cmd+A), and drag it all into the GitHub upload area
- Click **Commit changes**

> ⚠️ **Important:** Drag the *contents* of the folder, not the folder itself. The `.github/` folder must appear at the top level of your repository.

### Step 4 — Watch the build
- Click the **Actions** tab on your repository
- You will see a workflow called **Build APK** running with a yellow spinning circle
- Wait approximately 5 minutes for all steps to complete
- When you see a green ✓ checkmark, the build succeeded

### Step 5 — Download the APK
- Click the completed workflow run
- Scroll to the bottom of the page
- Under **Artifacts**, click **SenyasMX-debug** to download a zip
- Extract the zip to get **app-debug.apk**

### Rebuilding after changes
Every time you edit any file on GitHub and commit the change, a new build starts automatically. You don't need to do anything extra — just wait for the green checkmark and download the new APK.

---

## Installing on Your Phone

### Enable installation from unknown sources
Because this APK is not from the Google Play Store, you need to allow it once:

**Android 8 and newer:**
1. Go to **Settings → Apps**
2. Tap the app you will use to open the APK (usually **Files** or **My Files**)
3. Tap **Install unknown apps** → toggle **Allow from this source** to ON

**Samsung (One UI):**
Settings → Biometrics and security → Install unknown apps

**Android 7 and older:**
Settings → Security → Unknown sources → toggle ON

### Install the APK
1. Transfer **app-debug.apk** to your phone (email, Google Drive, USB cable, or any cloud storage)
2. Open your phone's **Files** or **My Files** app
3. Navigate to **Downloads** and tap **app-debug.apk**
4. Tap **Install**
5. Tap **Open** when installation finishes

### Minimum requirements
- Android 8.0 (API 26) or newer
- A working front-facing camera
- 50 MB free storage (more if you add videos)

---

## Using the App

### Tab 1 — Señas → Texto (Sign Recognition)

1. Open the app and tap **Allow** when it asks for camera permission
2. You will see your camera feed with a white rectangle in the centre — position your hands inside it
3. Tap **Iniciar reconocimiento** (Start recognition)
4. Perform a sign — the app shows the recognised phrase and confidence level
5. Tap the **speaker icon** to hear the phrase spoken aloud
6. Tap **Detener** (Stop) to pause recognition

**Confidence colours:**
- 🟢 Green bar — high confidence (85%+)
- 🔵 Cyan bar — medium confidence (65–85%)
- 🟠 Orange bar — low confidence (below 65%)

If the app shows **"No estoy seguro / Intenta de nuevo"** it means the sign was not recognised clearly enough. Try again with better lighting and clearer hand placement.

> **Note:** The app currently uses a demonstration classifier that cycles through sample phrases. Recognition of actual LSM signs requires adding a trained model — see [Future Feature Upgrades](#future-feature-upgrades).

### Tab 2 — Texto → Señas (Phrase Playback)

1. Type a Spanish phrase in the search bar — matching phrases appear instantly
2. Or tap a **category chip** (Saludos, Emergencia, Comida, etc.) to browse by topic
3. Tap any phrase in the list
4. A video player appears at the top — tap **Repetir** to watch again
5. Use the **0.5× / 1× / 1.5×** buttons to change playback speed

> **Note:** Videos show a placeholder icon until MP4 clips are added. See [Adding Sign Language Videos](#adding-sign-language-videos).

### Tab 3 — Historial (History)

- Shows every phrase recognised in Tab 1 during the current session
- Each entry shows the phrase text, time, and a colour-coded confidence badge
- Tap the **speaker icon** on any entry to hear it again
- Tap **Limpiar todo** to clear the history
- History is cleared when you close the app

---

## Testing

### Verifying the build works

After installing the APK:

| Test | Expected result |
|------|----------------|
| Open app | Loads immediately, no spinner or error |
| Enable Airplane Mode, reopen app | App still loads fully — confirms offline operation |
| Tab 1 — tap Start | Camera activates, sample phrases cycle every few seconds |
| Tab 1 — tap speaker icon | Phrase is spoken aloud in Spanish |
| Tab 2 — type "Hola" in search | "Hola" appears at the top of results |
| Tab 2 — tap "Emergencia" chip | List filters to emergency phrases only |
| Tab 2 — tap any phrase | Video player appears (placeholder icon if no video files) |
| Tab 3 — after recognising phrases | History list shows entries with timestamps |
| Tab 3 — tap Limpiar todo | History list clears |
| Deny camera permission | Error message shown, app does not crash |

### Low-light test
Cover the camera partially — the orange **"Poca luz"** warning banner should appear at the top of Tab 1.

---

## Adding Sign Language Videos

The app is fully ready to play sign language videos — you just need to supply the MP4 files.

### Video specifications
| Property | Requirement |
|----------|------------|
| Format | MP4 (H.264 video) |
| Orientation | Portrait (vertical) |
| Resolution | 480×854 or 720×1280 |
| Duration | 1–4 seconds per phrase |
| Max file size | 5 MB per clip |
| Audio | Optional (app works without it) |

### File naming
Each video file name must match the `video_file` field in `phrases.json`. For example:

```
ph_001_hola.mp4
ph_016_gracias.mp4
ph_028_ayuda.mp4
```

The complete list of required filenames is in `app/src/main/assets/phrases.json` — look for the `"video_file"` field in each phrase entry.

### Uploading videos to GitHub
1. In your repository, navigate to `app/src/main/assets/videos/`
2. Click **Add file → Upload files**
3. Drag your MP4 files into the upload area
4. Click **Commit changes**
5. GitHub Actions will automatically build a new APK with the videos included
6. Download and reinstall the new APK

> For more than 100 video clips, consider using [Git LFS](https://git-lfs.github.com/) (Large File Storage) to keep your repository size manageable. It is free to set up.

---

## Future Feature Upgrades

### Near-term (next 3–6 months)

**1. Real LSM recognition model**
The most important upgrade. Currently the app uses a demonstration classifier. Training a real model requires:
- Video recordings of a native LSM signer performing each of the 105 phrases
- Running the training script (`docs/TRAINING_PIPELINE.md`) to produce a `.tflite` model file
- Dropping the model file into `app/src/main/assets/` and uncommenting ~10 lines of code

**2. MediaPipe landmark extraction**
Replace the stub landmark generator with Google's MediaPipe library, which detects 33 body pose points and 21 hand points per frame in real time. The interface for this is already built — it just needs to be activated by adding the MediaPipe dependency and uncommenting the relevant code block in `RecognitionScreen.kt`.

**3. Expanded phrase vocabulary**
Scale from 105 to 500+ phrases by adding entries to `phrases.json`. No code changes are needed — the search, categories, and classifier interface all scale automatically.

**4. User-recorded video clips**
Allow users to record their own sign video demonstrations directly within the app, replacing the need to source external MP4 files.

### Medium-term (6–12 months)

**5. Practice mode**
A built-in learning feature where the user selects a phrase, watches the reference video, then attempts to perform the sign themselves. The classifier scores their attempt and provides feedback. The foundation for this (classifier + camera pipeline) is already in place.

**6. Confidence calibration settings screen**
A settings panel allowing users to adjust the recognition sensitivity:
- Confidence threshold (how certain the model must be before showing a result)
- Smoothing speed (how quickly the result updates)
- Emission cooldown (minimum time between recognised phrases)

**7. Phrase bookmarks and custom lists**
Let users mark favourite phrases and create custom phrase collections for specific situations (e.g. "At the doctor", "At the restaurant").

**8. Multiple signer profiles**
The recognition model can be fine-tuned per signer. A profile system would let multiple users each have their own calibrated model variant stored on the device.

### Long-term (12+ months)

**9. Continuous sentence recognition**
Rather than recognising one phrase at a time, build a sequence model that strings together multiple signs into a full sentence, with grammar-aware post-processing for Spanish output.

**10. Bidirectional text-to-avatar**
Instead of pre-recorded videos, generate real-time 3D avatar animations of a signing figure from text input, eliminating the need for a video library entirely.

**11. iOS version**
The architecture and all data files (phrases.json, model file, videos) are shared with the iOS codebase already built. Publishing to the App Store requires an Apple Developer account ($99/year).

**12. Offline Spanish keyboard**
An Android accessibility service that lets users type using LSM signs anywhere on the phone — in WhatsApp, SMS, email, and any other app — without opening Señas MX.

**13. Community phrase contributions**
A moderated system for the LSM community to submit new phrases, review video recordings, and vote on additions to the official phrase pack — expanding coverage to regional LSM variants across Mexico.

---

## Technical Architecture

The app is built with **Kotlin** and **Jetpack Compose** (Android's modern UI toolkit). It follows the MVVM (Model-View-ViewModel) pattern.

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material Design 3 |
| State management | Kotlin StateFlow + Coroutines |
| Camera | CameraX (Google) |
| ML inference | TFLite stub (real model: TensorFlow Lite) |
| JSON parsing | Gson |
| Text-to-speech | Android TextToSpeech (es-MX) |
| Build system | Gradle 8.10 + Android Gradle Plugin 8.7 |
| CI/CD | GitHub Actions |

### How the ML pipeline works
```
30 frames × 75 landmarks × 3 coordinates = 6,750 numbers per inference
                    ↓
        TFLite model: [1, 30, 75, 3] → [1, 106]
                    ↓
    106 probability scores (0 = unknown, 1-105 = phrases)
                    ↓
    EMA smoothing (α=0.3) + 4-window stability check
                    ↓
        Emit phrase when confidence ≥ 65%
```

### Swapping in a real trained model
1. Train using the pipeline in `docs/TRAINING_PIPELINE.md`
2. Copy `lsm_phrase_classifier.tflite` to `app/src/main/assets/`
3. In `app/build.gradle.kts` uncomment the two TFLite dependency lines
4. In `service/LSMClassifier.kt` uncomment the interpreter blocks (marked with comments)
5. Commit and push — GitHub Actions builds the updated APK automatically

---

## Licence

MIT — free to use, modify, and distribute.

---

*Built with ❤️ for the LSM community.*
