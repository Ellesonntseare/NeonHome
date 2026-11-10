# Neon Home

A personal Android launcher inspired by the dark, cyan futuristic concept. Android 8.0 or newer. Written in Java using Android's built-in UI toolkit; no third-party app dependencies.

## Install

1. Copy NeonHome.apk to your Android phone and open it. If asked, allow installation from the app you used to open the file.
2. Open Neon Home. Tap Settings → Make Neon your home app, then select Neon Home.
3. To return to your previous launcher, use Android Settings → Apps → Default apps → Home app. Names may vary by phone.

## Features

- Clock and date following the phone's locale and time format; tap the clock for alarms.
- Procedural night sky and mountain background, cyan borders, and cyan/violet/mint accent choices.
- Searchable drawer of installed launchable apps. Hold an app to pin/unpin or open system app info. Up to 12 favourites, with an initial selection of eight apps.
- Phone, Messages, Camera and Browser shortcuts.
- Editable focus note; three persistent checklist items. Hold a task to edit its text. These do not reset automatically each day.
- Open the drawer with the All apps button, or swipe up when the home screen reaches its bottom.
- Secret folder: Settings → Secret folder, or hold the NEON title. Unlock using the phone's existing PIN, pattern or password, then choose apps to hide. Hidden packages are removed from favourites and excluded from the regular drawer and search. The folder locks when you leave Neon. Set a screen lock first if necessary.

## Secret folder and notifications

The secret folder hides shortcuts inside Neon; it does not isolate or encrypt other apps, and those apps can still be opened through Android settings, other launchers, links or recent apps. It is not Android Private Space or Xiaomi Second Space.

To stop a hidden app's notifications, unlock the secret folder, hold the app, select **Turn off notifications…**, and switch off its notifications in the system screen. Repeat for each app. Neon cannot change another app's notification permission automatically and does not claim that an app is muted. Removing an app from the folder does not restore its notifications; re-enable them in Android settings when wanted. Existing notifications may need to be cleared separately.

Xiaomi's standard REDMI 15 FAQ states that Second Space is unsupported: https://www.mi.com/uk/support/faq/details/KA-592870/

Preferences stay on the device. No network, contacts, location or storage permissions are requested.

## Current scope

This is a first functional version. Installed apps retain their original icons inside themed frames. The background is drawn by the app; it is not a system wallpaper picker. Third-party widgets, notification badges, folders, drag-and-drop arrangement, weather, work profiles and icon packs are not implemented. Some system gestures depend on the phone manufacturer. Uninstalling clears local preferences.

## Build

Open this folder in Android Studio with JDK 17 and Android SDK 35. Allow Gradle sync, then use Build → Build APK(s). The project uses Android Gradle Plugin 8.7.3, compatible with Gradle 8.9. No Gradle wrapper binary is bundled.

Alternatively, use the included PowerShell build script with a JDK 17 installation, Android SDK platform 35 android.jar, and SDK Build Tools 35.0.0:

```powershell
./build-local.ps1 -JdkHome 'C:/tools/jdk17' -AndroidJar 'C:/Android/sdk/platforms/android-35/android.jar' -BuildTools 'C:/Android/sdk/build-tools/35.0.0'
```

The local script compiles resources and Java, creates DEX, aligns the package and signs a debug APK. Keep its build-local/debug.keystore to sign compatible future updates. This personal testing build is not a Play Store release.

Platform reference: https://developer.android.com/reference/android/content/Intent#CATEGORY_HOME
