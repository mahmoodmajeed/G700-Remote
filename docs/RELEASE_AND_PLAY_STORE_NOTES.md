# Release And Play Store Notes

These notes prepare the v1.6.8 baseline for future automated builds and Play Store work.

## Local Verification

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testReleaseUnitTest
.\gradlew.bat assembleRelease
```

Generated artifacts are under `app/build/outputs/` and should not be committed.

## Versioning

Current baseline:

- `versionName = "1.6.8"`
- `versionCode = 30`

For future releases, increase `versionCode` for every Play Store upload. Keep `versionName` user-readable and match release notes.

## Signing

Signing keys are not stored in this repository. Keep keystores and passwords outside Git and outside generated artifacts.

For local manual signing, use Android Studio or `apksigner` with a keystore outside this repository. For CI later, prefer encrypted GitHub Actions secrets or Play App Signing with a dedicated upload key.

Never commit:

- keystore files
- signing passwords
- `signing.properties`
- generated signed APKs/AABs

Current release-signing certificate SHA-256 for Firebase Android app setup and Firebase App Check / Play Integrity registration:

`36:AE:81:B9:60:D1:46:E6:A4:44:5E:3E:04:15:BF:F9:5B:0F:6A:18:EC:04:44:78:59:C8:1B:81:C3:DA:72:4F`

## Firebase Release Setup

- `app/google-services.json` is required for Firebase SDK initialization, but the real file is local-only and ignored by Git. Use `app/google-services.example.json` as a shape reference.
- The Maps SDK key is injected through the Google Maps Secrets Gradle Plugin from `local.properties` as `MAPS_API_KEY`. Do not commit the real key.
- Firebase Analytics, Crashlytics, Performance Monitoring, App Check with Play Integrity, and Cloud Messaging are configured in the app module.
- In Firebase Console, add the SHA-256 fingerprint above to the Android app, then enable App Check with the Play Integrity provider.
- In Google Play Console / Play Integrity, ensure package `com.mmy.g700remote` and the release certificate fingerprint match the distributed APK signing lineage.
- Cloud Messaging notification messages can use the default channel `g700_remote_messages` and the app will show branded notifications.

## Release Size

Release builds enable:

- `isMinifyEnabled = true`
- `isShrinkResources = true`

Keep those enabled unless a specific compatibility issue is found.

## Play Store Preparation Checklist

- Choose and add a project license if the repository will be open source.
- Prepare a privacy policy covering Bluetooth, LAN discovery, pairing code storage, diagnostics, and optional protocol-log export.
- Review Android permissions and make sure every permission has a user-facing reason, including optional BLE proximity wake and Companion Device setup.
- Replace debug/test wording in screenshots and store copy.
- Validate English and Arabic UI on small and large devices.
- Validate demo mode from a clean install with no paired car. This is the safest Play Store review path because it does not send vehicle commands.
- Validate shared Google Maps URLs, including `maps.app.goo.gl` short links, against a real DisplayMirror head unit and confirm the car receives coordinates/place text rather than the raw short URL.
- Validate optional BLE proximity wake with the manual checklist in `docs/BLE_WAKE_ARCHITECTURE.md`.
- Test on a real phone and real DisplayMirror-equipped head unit before public release.
- Decide whether Play Store distribution is appropriate for a vehicle-control companion that depends on a third-party head-unit app.
- Prepare clear disclaimers: not OEM-certified, authorized use only, vehicle state depends on DisplayMirror responses.

## Future Automation

A good first CI workflow can run:

```powershell
.\gradlew.bat testReleaseUnitTest
.\gradlew.bat assembleRelease
```

For Play Store automation, prefer building Android App Bundles (`bundleRelease`) and use secure upload credentials through GitHub Actions secrets.

## GitHub Release Updater

The app checks `mahmoodmajeed/G700-Remote` GitHub Releases for the latest APK asset. For every public release that should be offered in-app:

- create a GitHub release with a semantic tag such as `v1.5.0`
- attach one signed `.apk` asset
- keep the APK `versionName` higher than installed builds
- keep the APK signed with the same signing lineage so Android can install it as an update

The app records successful update checks. If the installed build has not successfully checked GitHub releases for 7 days, control surfaces are locked until a check succeeds. If a newer release is found, the app requires updating before control surfaces are enabled again.

Normal Android apps cannot silently install updates. The app downloads the APK and opens the Android package installer after the user grants install-from-this-source permission.
