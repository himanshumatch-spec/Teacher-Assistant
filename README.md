# Teacher Assistant — Android APK Project

Native Android wrapper for the Guru Vasishta Vidhyan Teacher Assistant PWA.

## App URL
https://school-teacher-gfsg.hatchable.site

## Build locally
Requires JDK 17+ and Android SDK plus Gradle 8.13:

```bash
gradle assembleDebug
```

Debug APK:
`app/build/outputs/apk/debug/app-debug.apk`

Release:
`./gradlew assembleRelease`

For a distributable release APK, configure Android release signing. The included GitHub Actions workflow builds debug APKs automatically; signed release builds activate when the documented keystore secrets are configured.

## GitHub Actions
The workflow in `.github/workflows/android.yml` builds a debug APK on every push and on manual dispatch. A signed release build is enabled when these GitHub Actions secrets are provided:
- KEYSTORE_BASE64
- KEYSTORE_PASSWORD
- KEY_ALIAS
- KEY_PASSWORD

The Android wrapper keeps the existing standalone UI, attendance register, notices, PNG save, and settings intact.
