# Release And Play Store Notes

These notes prepare the v1.2 baseline for future automated builds and Play Store work.

## Local Verification

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testReleaseUnitTest
.\gradlew.bat assembleRelease
```

Generated artifacts are under `app/build/outputs/` and should not be committed.

## Versioning

Current baseline:

- `versionName = "1.2"`
- `versionCode = 3`

For future releases, increase `versionCode` for every Play Store upload. Keep `versionName` user-readable and match release notes.

## Signing

Signing keys are not stored in this repository. Keep keystores and passwords outside Git and outside generated artifacts.

For local manual signing, use Android Studio or `apksigner` with a keystore outside this repository. For CI later, prefer encrypted GitHub Actions secrets or Play App Signing with a dedicated upload key.

Never commit:

- keystore files
- signing passwords
- `signing.properties`
- generated signed APKs/AABs

## Release Size

Release builds enable:

- `isMinifyEnabled = true`
- `isShrinkResources = true`

Keep those enabled unless a specific compatibility issue is found.

## Play Store Preparation Checklist

- Choose and add a project license if the repository will be open source.
- Prepare a privacy policy covering Bluetooth, LAN discovery, pairing code storage, diagnostics, and optional protocol-log export.
- Review Android permissions and make sure every permission has a user-facing reason.
- Replace debug/test wording in screenshots and store copy.
- Validate English and Arabic UI on small and large devices.
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
