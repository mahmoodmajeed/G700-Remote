# Architecture

G700 Remote is intentionally small: Compose UI, one repository, protocol models/codecs, and two transport implementations. The goal is to keep the app easy to reason about while supporting both BLE and LAN connectivity.

## Runtime Flow

```text
SettingsStore
  -> G700RemoteViewModel
  -> RemoteRepository
  -> CompositeDisplayMirrorTransport
  -> BLE or LAN transport
  -> DisplayMirror head unit
```

1. The setup screen stores a pairing code and selected device/transport settings.
2. The repository asks the composite transport to connect using the configured priority.
3. The active transport sends a protocol v4 `hello` command with the pairing code.
4. After DisplayMirror accepts the handshake, normal commands are allowed.
5. Transport responses are applied directly to `VehicleTelemetry` and UI state.
6. Missing fields stay nullable so the UI can show "Unknown" instead of inventing values.

## Main Modules

- `protocol`: command models, response models, JSON serialization/parsing, and newline frame handling.
- `ble`: BLE scanner/client, GATT state machine, notification handling, command queue, and fake transport used by tests/previews.
- `network`: LAN TCP client plus Android NSD/mDNS discovery for the DisplayMirror `CarKey` service.
- `data`: repository, secure settings store, composite transport priority/fallback logic, telemetry, and redacted protocol logging.
- `security`: local biometric/PIN gate.
- `update`: GitHub release checks, background update worker, APK download, and Android installer handoff.
- `ui`: Compose screens, controls, translations, and theme definitions.

## State Model

The repository keeps a single `RemoteUiState` stream. Command responses update telemetry as soon as they return, then refresh commands may request a newer vehicle snapshot. This avoids waiting for a later poll when DisplayMirror already returned useful state.

Telemetry fields are nullable by design. A null means DisplayMirror did not return the value during this session, not that the vehicle value is zero or off.

While the app is foregrounded and connected, the repository refreshes the main DisplayMirror status surfaces every 3 seconds. Commands still apply returned values immediately when DisplayMirror sends a response.

## Transport Strategy

`CompositeDisplayMirrorTransport` selects BLE or LAN according to settings:

- BLE first
- LAN first
- BLE only
- LAN only

BLE remains the preferred remote-key transport for close range. LAN/mDNS is useful when the phone and head unit share a network and can be used as a fallback or primary path.

## UI Strategy

The UI should prioritize confirmed vehicle state and reduce ambiguity:

- Home: lock/unlock action, compact telemetry, quick toggles.
- Climate: dense but readable controls with clear selected states.
- Openings/lights: grouped action boxes instead of raw command lists.
- Settings: transport status, protocol configuration, regional features, theme, and diagnostics.

Avoid debug-style copy in normal screens. Diagnostics and protocol logs should stay behind the advanced section.
