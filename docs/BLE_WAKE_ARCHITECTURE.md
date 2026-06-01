# BLE Proximity Wake Architecture

G700 Remote can optionally register an Android-managed BLE scan that wakes the app when the paired DisplayMirror BLE peripheral advertises nearby. The feature is on by default for new installs and is designed to avoid a constant app-owned background service or periodic high-power scans.

## Design

- `BleWakeManager` registers `BluetoothLeScanner.startScan(filters, settings, pendingIntent)`.
- The scan filter uses DisplayMirror's BLE service UUID and, when available, the paired BLE MAC address.
- `BleWakeReceiver` receives scan PendingIntent callbacks, checks errors, validates the scan result, debounces duplicate wakes, and delegates immediately.
- `BleWakeCoordinator` starts the existing repository connection path and attempts to start `ConnectedCarNotificationService`.
- `ConnectedCarNotificationService` remains the single persistent connected-control notification and uses foreground service type `connectedDevice`.
- `BleWakeBootReceiver` re-registers the scan after boot or app replacement if the user enabled the feature.
- `G700CompanionDeviceService` and `BleCompanionManager` provide the preferred Companion Device path for Android background foreground-service exemptions.

## Android Behavior

- Android can wake the app after normal process death when the scan PendingIntent matches.
- Android will not wake an app that the user force-stopped. Android treats force-stop as an explicit user choice.
- OEM battery savers can still delay or suppress wake behavior.
- The peripheral must be advertising for the scan wake to happen.
- If Android 12+ blocks starting the foreground service from the background and no Companion Device exemption is available, the app queues a short WorkManager reconnect attempt and posts a tap-to-activate notification instead of crashing.
- BLE MAC addresses can be private/random on some peripherals. DisplayMirror currently advertises the stable service UUID, so service UUID matching is the primary identifier and MAC is used only when it is reliable.

## Manual Test Checklist

- Fresh install, pair DisplayMirror, confirm "Wake when nearby" is enabled.
- Grant Bluetooth permissions and notification permission.
- Deny Bluetooth permissions and confirm the app does not crash.
- Enable Companion Device setup from Settings and confirm Android association.
- Put app in foreground, confirm normal controls still work.
- Move app to background, then approach the car while DisplayMirror is advertising.
- Swipe app away or let process die, then approach the car while DisplayMirror is advertising.
- Test with Bluetooth off and confirm only a recoverable notification/state appears.
- Reboot phone and confirm wake scan re-registers after unlock/boot completed.
- Update app and confirm wake scan re-registers after package replacement.
- Android 12+: verify background foreground-service restriction path shows the tap-to-activate fallback if no companion exemption is active.
- Android 13+: deny notification permission and confirm foreground service flow does not crash.
- Android 14+: confirm connectedDevice foreground service type validation passes.
- Force-stop the app and confirm wake does not happen until opened again.
