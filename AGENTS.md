# Codex / AI Context

This repository is the clean v1.2 baseline for G700 Remote, an Android phone companion app for a Jetour G700 head unit running DisplayMirror.

## Start Here

- Main app path: `app/src/main/java/com/mmy/g700remote`
- Package: `com.mmy.g700remote`
- Baseline version: `versionName = "1.2"`, `versionCode = 3`
- Build system: Gradle wrapper, Android Gradle Plugin 8.5.2, Kotlin 2.0.21, Compose
- Local command-line Java: prefer `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` on this workstation

## Product Intent

The app should feel like a polished, minimalist car remote. Keep home focused on lock/unlock and high-signal vehicle status. Do not add features that DisplayMirror does not expose remotely.

## Compatibility Rules

- Keep protocol assumptions grounded in DisplayMirror artifacts or fresh reverse-engineering.
- DisplayMirror v2.65 compatibility uses protocol v3 pairing and exposes `fuelPercent`, `coolantTemp`, and `race_charge`.
- Do not restore rear/ceiling screen UI unless a future DisplayMirror remote protocol actually exposes it.
- Do not implement background "car left running" alerts unless a future protocol exposes reliable ignition/power mode.
- Show unknown status as unknown. Do not infer live vehicle status from local clicks, connection loss, or unrelated telemetry.
- For commands without returned status, UI feedback may show a sent/accepted action but must not claim confirmed vehicle state.

## Security And Safety

- Never commit keystores, signing passwords, generated signed APKs, or local protocol logs.
- Sensitive actions should remain behind local biometric/PIN gate or confirmation.
- This app is not an OEM-certified digital key and should not be described as one.

## Checks Before Publishing Changes

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testReleaseUnitTest
.\gradlew.bat assembleRelease
```

If OneDrive locks build files, stop Gradle first and delete only build output inside this repository:

```powershell
.\gradlew.bat --stop
Remove-Item -LiteralPath .\app\build -Recurse -Force
```

Only do that after verifying the resolved path is under this repo.
