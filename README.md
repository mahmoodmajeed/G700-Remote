# G700 Remote

G700 Remote is a Kotlin Android companion app for Jetour G700 head units running the open-source DisplayMirror app. It connects to DisplayMirror's remote-access protocol over Bluetooth LE or LAN/mDNS and provides a focused phone remote for lock/unlock, climate, openings, lighting, charging, and vehicle telemetry that DisplayMirror exposes.

This repository started from the v1.2 baseline and now tracks the functional v1.3 release. It is intended as the clean source baseline for future development, CI, Play Store preparation, and Codex-assisted changes.

## Status

- App version: `1.3`
- Android package: `com.mmy.g700remote`
- `versionCode`: `4`
- Minimum Android: API 30
- Target/compile SDK: API 36
- UI: Jetpack Compose Material 3
- Protocol: DisplayMirror remote protocol v4
- Transports: BLE and LAN/mDNS
- Languages: English and Arabic

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

- First-time setup with pairing-code entry and a link to DisplayMirror.
- BLE scanning, LAN/mDNS discovery, and user-selectable transport priority.
- Smart lock/unlock home action based on returned lock state.
- Compact vehicle telemetry tiles for battery SOC, fuel, AC, cabin/outside temperature, and coolant when returned.
- Quick AC and hazard toggles with clear on/off state.
- Climate control with left/right temperatures, 10-step fan bars, mode toggles, parking AC, and seat ventilation.
- Optional regional controls for steering heat, seat heating, and PM2.5 filter. These are hidden by default.
- Window, sunroof, and sunshade controls.
- Charging controls including target SOC, parking charge, race charge start/stop/status, and returned charging telemetry.
- Lighting controls for hazards and Daytime Running Lights.
- Side mirror fold/unfold controls when DisplayMirror accepts the command.
- Share-to-car navigation: share Google Maps, `geo:` links, Google navigation links, coordinates, or place text to G700 Remote and the app forwards it to DisplayMirror.
- Settings for language, theme, pairing, connectivity, security gate, regional features, and diagnostics.
- Settings app update checker using GitHub Releases, with manual check and twice-daily background checks.
- Local biometric/PIN gate for sensitive actions when enabled.
- Redacted protocol log export for troubleshooting.

## Known Limits

- The app does not infer ignition or power state from AC, connection loss, or other indirect signals.
- The "car left running after walking away" background notification is intentionally not implemented because the inspected DisplayMirror remote protocol does not expose a reliable remote ignition/power mode.
- Rear/ceiling screen controls are intentionally absent because the inspected DisplayMirror remote protocol does not expose those commands.
- Hazards, DRL, and mirror commands can be sent, but DisplayMirror does not currently return confirmed live status for those fields in the general remote status payload.
- Android does not allow silent APK installation for normal apps. The updater downloads the signed APK and opens the Android package installer after the user grants install-from-this-source permission.
- Values are displayed only when returned by DisplayMirror. Missing fields remain unknown instead of being faked.
- Lock-state semantics may need real vehicle calibration if DisplayMirror or the head unit changes the returned mapping.

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
