# G700 Remote

G700 Remote is a Kotlin Android companion app for Jetour G700 head units running the open-source DisplayMirror app. It connects to DisplayMirror's remote-access protocol over Bluetooth LE or LAN/mDNS and provides a focused phone remote for lock/unlock, climate, openings, lighting, charging, and vehicle telemetry that DisplayMirror exposes.

This repository started from the v1.2 baseline and now tracks the v1.6.6 release. It is intended as the clean source baseline for future development, CI, Play Store preparation, and Codex-assisted changes.

## Status

- App version: `1.6.6`
- Android package: `com.mmy.g700remote`
- `versionCode`: `28`
- Minimum Android: API 30
- Target/compile SDK: API 36
- UI: Jetpack Compose Material 3 with an expressive spring-motion surface system
- Protocol: DisplayMirror remote protocol v4
- Transports: BLE and LAN/mDNS
- Languages: English and Arabic
- Firebase: Analytics, Crashlytics, Performance Monitoring, App Check with Play Integrity, and Cloud Messaging are wired through the app module configuration.

This is not an OEM-certified digital key. It is a compatibility client for the DisplayMirror head-unit protocol and should only be used with vehicles and head units you own or are authorized to control.

## DisplayMirror Compatibility

The head unit should have DisplayMirror installed and configured with Remote Access enabled. DisplayMirror is credited to Baghdady92:

[https://github.com/Baghdady92/DisplayMirror](https://github.com/Baghdady92/DisplayMirror)

The v1.3 app was updated against the developer-provided DisplayMirror source repository:

- [https://github.com/Baghdady92/DisplayMirror-Using-Internal-method](https://github.com/Baghdady92/DisplayMirror-Using-Internal-method)

The inspected source was at DisplayMirror `2.74.0` / protocol v4 and confirmed:

- protocol v4 pairing through the `hello` command
- BLE service/characteristics used by DisplayMirror remote access
- LAN discovery through `_carkey._tcp.` and TCP port `9274`
- telemetry for `fuelPercent`, `coolantTemp`, and `race_charge`
- navigation sharing through the `navigate` command
- car-location readback through `get_location`
- mirror fold/unfold through the `mirror` command
- no reliable remote ignition/power-mode status
- no remote rear/ceiling screen command

For implementation details, see [docs/DISPLAYMIRROR_COMPATIBILITY.md](docs/DISPLAYMIRROR_COMPATIBILITY.md).

## Features

- First-time setup with pairing-code entry, a link to DisplayMirror, and a demo mode for review/testing without a paired car.
- Material 3 Expressive-inspired UI with responsive spring press motion, larger tactile surfaces, and a Jetour-branded header.
- v1.6.6 is a silent bug-fix build on top of v1.6.3 with the same user-facing release notes.
- v1.6.3 keeps the Home redesign and interactive vehicle location map, adds cleaner map gestures, current-phone-location context, source chips, smoother map transitions, GT icon optical alignment, Firebase-triggered update checks, and safer local-only Google configuration.
- v1.6.2 adds dark-mode map styling, a cleaner expanded map with open/navigate/copy actions, better address cleanup, pull-to-refresh refinements, and safer local-only Google configuration.
- v1.6.1 preserves the v1.6.0 release-note content and fixes a startup crash caused by creating a Google Maps bitmap marker before Maps initialized.
- v1.6.0 adds the redesigned Home screen, Google Maps vehicle location, expanded map details, direct navigation directions, pull-to-refresh on app pages, and clearer fresh-status wording.
- v1.4.12 refines the connected header status, moves pairing reset to the end of Settings with history choices, defaults BLE proximity wake on for new installs, and makes the connected notification harder to dismiss accidentally.
- v1.4.11 keeps the v1.4.9 feature set and adds minor launcher icon scale and header transport-line polish.
- v1.4.10 keeps the v1.4.9 feature set and adds minor launcher icon safe-zone and header spacing polish.
- v1.4.9 refines the header into a three-line status block, moves refresh/connect into the tappable header area, adds selectable launcher icon themes, defaults new installs to Himalaya Slate, adds optional BLE proximity wake, and animates the bottom tab highlight as a sliding pill.
- v1.4.8 adds smoother lock/unlock progress, persisted last-known vehicle status, last-refresh display, connected quick-action notification, cleaner shared-link titles, and in-app release notes.
- v1.4.7 sends only resolved map coordinates or clean place text to DisplayMirror, prevents unsupported remote HVAC-off behavior, and keeps Arabic temperature values left-to-right.
- v1.4.6 improves Lighting readability with vertical actions and makes Android back return to Home before exiting.
- v1.4.5 shared-link history resend now uses the stored navigation command directly, while tapping the saved link still opens the original location on the phone.
- v1.4.4 shared-link loading feedback, quieter connection notifications, and a smart header action that connects when offline and refreshes when connected.
- Dark/light appearance setting, defaulting to dark mode on first install, plus professional G700-inspired color themes and launcher icon variants.
- BLE scanning, LAN/mDNS discovery, and user-selectable transport priority.
- Smart lock/unlock home action based on returned lock state.
- Compact vehicle telemetry tiles for battery SOC, fuel, AC, cabin/outside temperature, and coolant when returned.
- Quick air-conditioner and hazard toggles with clear on/off state.
- Climate control with left/right temperatures, 10-step fan bars, mode toggles, parking AC, seat ventilation, and a compact A/C compressor control beside cabin air on/off.
- Optional regional controls for steering heat, seat heating, and PM2.5 filter. These are hidden by default.
- Window, sunroof, and sunshade controls.
- Charging controls including target SOC, parking charge, race charge start/stop/status, and returned charging telemetry.
- Lighting controls for hazards and Daytime Running Lights.
- Side mirror fold/unfold controls when DisplayMirror accepts the command.
- Share-to-car navigation: share Google Maps, `geo:` links, Google navigation links, coordinates, or place text to G700 Remote and the app resolves coordinates or forwards a clean destination to DisplayMirror.
- Shared-link history with readable place names when available, original-link open back into Maps/browser, resend, delete confirmation, and clear-all confirmation.
- Last-known status is saved on the phone after refreshes, so offline screens can still show the latest returned vehicle data while controls remain disabled until connected.
- Last-known vehicle location is saved when DisplayMirror returns it. The Home location card uses Android geocoding when available, embeds a Google Map when configured locally, and opens the phone's Maps app for location/directions.
- Optional location source setting can use DisplayMirror-reported location by default or the phone's last known location while connected over BLE when location permission is granted.
- Optional connected notification, enabled by default, keeps a low-priority ongoing status while connected and provides quick lock/unlock and hazards actions.
- Optional BLE proximity wake, on by default for new installs, registers an Android-managed BLE PendingIntent scan so the app can wake when the paired DisplayMirror BLE device is nearby without keeping a permanent background scanning service. v1.5.0 also performs a bounded sync attempt from the wake worker so status/location can refresh when Android permits background work.
- In-app "What's new" dialog appears after updates and can be reopened from Settings in English or Arabic.
- Settings for merged connectivity/pairing, language, theme, security gate, regional features, and diagnostics.
- Settings app update checker using GitHub Releases, with manual check, twice-daily background checks, and a 7-day freshness gate so outdated builds stop controlling the car until the update check succeeds.
- Local biometric/PIN gate for sensitive actions when enabled.
- Redacted protocol log export for troubleshooting.

## Known Limits

- The app does not infer ignition or power state from AC, connection loss, or other indirect signals.
- The "car left running after walking away" background notification is intentionally not implemented because the inspected DisplayMirror remote protocol does not expose a reliable remote ignition/power mode.
- Rear/ceiling screen controls are intentionally absent because the inspected DisplayMirror remote protocol does not expose those commands.
- Hazards, DRL, and mirror commands can be sent, but DisplayMirror does not currently return confirmed live status for those fields in the general remote status payload.
- Remote DisplayMirror protocol v4 exposes compressor `ac_on`/`ac_off`, not a full HVAC power command. G700 Remote can start cabin airflow with a moderate fan speed, but remote HVAC-off is blocked with a user message because the reviewed DisplayMirror source does not expose that action.
- Auto climate mode is not exposed by the reviewed DisplayMirror remote protocol. Auto defrost is shown as a returned/toggleable DisplayMirror mode, but it should still be validated on the vehicle because it depends on head-unit behavior.
- Android does not allow silent APK installation for normal apps. The updater downloads the signed APK and opens the Android package installer after the user grants install-from-this-source permission.
- BLE proximity wake depends on DisplayMirror advertising, Android background policy, and OEM battery behavior. It will not run after the user force-stops the app, and Android 12+ may require Companion Device association before the connected foreground notification can start automatically from the background.
- Values are displayed only when returned by DisplayMirror. Missing fields remain unknown instead of being faked.
- Embedded Google Maps requires a local `MAPS_API_KEY` configured outside Git. Without it, map loading is not expected to work.
- Phone-derived location requires Android location permission and uses only the last known phone location while connected over BLE.

## Firebase

Firebase is configured locally with `app/google-services.json` for package `com.mmy.g700remote`. The real file is ignored by Git; use `app/google-services.example.json` only as a structure reference.

Enabled SDK paths:

- Analytics events for screen views, command taps, key settings changes, sessions, connection states, navigation-share outcomes, and map-open actions. Sensitive values such as pairing code, BLE MAC address, raw shared links, and exact coordinates are not logged as analytics parameters.
- Crashlytics custom keys for connection state, transport, language, theme, demo mode, and paired state, plus non-fatal reporting for background wake and notification failures.
- Performance Monitoring through the Firebase Performance SDK/plugin.
- App Check using the Play Integrity provider.
- Cloud Messaging through `G700FirebaseMessagingService` with app-branded notifications.

Release signing SHA-256 for Firebase Android app / App Check setup:

`36:AE:81:B9:60:D1:46:E6:A4:44:5E:3E:04:15:BF:F9:5B:0F:6A:18:EC:04:44:78:59:C8:1B:81:C3:DA:72:4F`
- Lock-state semantics may need real vehicle calibration if DisplayMirror or the head unit changes the returned mapping.

## Local Google Configuration

Do not commit local Google/Firebase keys or generated service files. Local builds need:

- `local.properties` containing the Android SDK path and `MAPS_API_KEY=...`
- `app/google-services.json` downloaded from Firebase Console

`local.defaults.properties` and `app/google-services.example.json` are committed only so a fresh checkout explains the expected shape without exposing live keys.

## Project Layout

```text
app/src/main/java/com/mmy/g700remote/
  ble/          BLE transport, scanning, GATT client, command queue, fake transport
  data/         repository, settings storage, transport selection, app state
  network/      LAN TCP client and Android NSD/mDNS discovery
  protocol/     command models, response models, JSON codec, frame assembler
  security/     biometric/PIN gate
  update/       GitHub release checking, update notifications, APK installer handoff
  ui/           Compose app screens and theme system
```

Additional project context lives in:

- [AGENTS.md](AGENTS.md) for future Codex/AI work.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the internal app structure.
- [docs/DISPLAYMIRROR_COMPATIBILITY.md](docs/DISPLAYMIRROR_COMPATIBILITY.md) for protocol compatibility notes.
- [docs/BLE_WAKE_ARCHITECTURE.md](docs/BLE_WAKE_ARCHITECTURE.md) for the battery-conscious BLE proximity wake design and manual test checklist.
- [docs/RELEASE_AND_PLAY_STORE_NOTES.md](docs/RELEASE_AND_PLAY_STORE_NOTES.md) for release, signing, and store preparation.

## Build

Use Android Studio/IntelliJ IDEA or the Gradle wrapper. On this Windows workstation, the reliable command-line setup uses Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testReleaseUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

Release builds have R8 and resource shrinking enabled:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleRelease
```

Release signing keys and passwords are intentionally not committed. Keep keystores outside the repository and use local signing configuration or Android Studio's signing workflow.

## Repository Hygiene

Do commit:

- Kotlin/Compose source
- resources and launcher assets
- Gradle wrapper and build scripts
- tests
- project documentation

Do not commit:

- signing keys or passwords
- `local.properties`
- generated APK/AAB files
- Gradle build output
- decompiled APK folders or raw DisplayMirror APKs
- protocol logs containing live vehicle data

## Credits

- DisplayMirror app: [Baghdady92/DisplayMirror](https://github.com/Baghdady92/DisplayMirror)
- G700 Remote app: developed by Mahmood Majeed in Bahrain

## License

No open-source license has been selected yet. Until a license is added, all rights are reserved by the repository owner.
