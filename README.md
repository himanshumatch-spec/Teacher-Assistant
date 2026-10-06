# Teacher Assistant — Android APK Project

Native Android wrapper for the Guru Vasishta Vidhyan Teacher Assistant app.

## Current stable build

Build #63 is the verified debug build containing the WhatsApp package-visibility fix.

## Debug build

Requires JDK 17+ and Android SDK plus Gradle 8.13:

```bash
gradle assembleDebug
```

Output:
`app/build/outputs/apk/debug/app-debug.apk`

## Signed release APK

The project now has a dedicated manual GitHub Actions workflow:

`.github/workflows/release.yml`

It creates a properly signed `app-release.apk` using the project's release keystore.

### Required GitHub Actions secrets

Repository → Settings → Secrets and variables → Actions → New repository secret:

- `TA_RELEASE_KEYSTORE_B64`
- `TA_RELEASE_STORE_PASSWORD`
- `TA_RELEASE_KEY_ALIAS`
- `TA_RELEASE_KEY_PASSWORD`

The release workflow is manual so a missing signing secret cannot break normal debug builds.

### Important signing rule

Keep the same release keystore permanently. Future APK updates must use the same signing key or Android will not allow an update over the installed release app.

The release application ID is:
`com.guruvasishta.teacherassistant`

The debug build uses a separate `.debug` application ID suffix, so debug and release can coexist during testing.

## Release process

1. Add the four GitHub Actions secrets.
2. Open GitHub → Actions → **Teacher Assistant Signed Release APK**.
3. Click **Run workflow**.
4. Wait for the release workflow to finish successfully.
5. Download the `Teacher-Assistant-release` artifact.
6. Distribute the resulting `app-release.apk`.

The signed release is suitable for direct distribution to teachers. Android/Play Protect may still perform a security scan for APKs installed outside Google Play.

## App features

- Attendance
- Student register
- Attendance history
- Professional bilingual notice generator
- Notice history
- Save notice as PNG
- WhatsApp sharing
- School logo/settings
- Offline/local-first operation
