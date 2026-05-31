# DisplayMirror Compatibility

This document records the compatibility assumptions used for the v1.4.1 release. It is a source summary, not a replacement for re-checking future DisplayMirror APK releases.

## Head Unit Requirement

Install DisplayMirror on the Jetour G700 head unit and enable Remote Access in DisplayMirror. The phone app enters the generated pairing code during first-time setup.

DisplayMirror project:

[https://github.com/Baghdady92/DisplayMirror](https://github.com/Baghdady92/DisplayMirror)

Developer-provided source reviewed for v1.3:

[https://github.com/Baghdady92/DisplayMirror-Using-Internal-method](https://github.com/Baghdady92/DisplayMirror-Using-Internal-method)

## Protocol

- Protocol version: `4`
- Framing: UTF-8 JSON objects separated by newline
- Handshake command: `hello`
- Pairing field: `pairingCode`

Example handshake shape:

```json
{"cmd":"hello","protocolVersion":4,"pairingCode":"123456"}
```

The app does not send normal control commands until the transport is connected, notifications/streams are ready, and the handshake is accepted.

## BLE

Known DisplayMirror remote-access identifiers:

- Service: `b1c2d3e4-f5a6-7890-abcd-ef1234567890`
- Command characteristic: `b1c2d3e4-f5a6-7890-abcd-ef1234567891`
- Response characteristic: `b1c2d3e4-f5a6-7890-abcd-ef1234567892`
- CCCD: `00002902-0000-1000-8000-00805f9b34fb`

BLE notifications may split responses into chunks, so the app buffers until newline before parsing.

## LAN / mDNS

- Service name: `CarKey`
- Service type: `_carkey._tcp.`
- TCP port: `9274`

LAN uses the same newline-framed JSON command/response model as BLE.

## Commands Used By The App

- `status`
- `lock`
- `unlock`
- `window`
- `sunroof`
- `sunshade`
- `hazards`
- `drl`
- `soc`
- `parking_charge`
- `race_charge`
- `climate`
- `mirror`
- `get_location`
- `navigate`

Sensitive commands remain behind local auth or explicit confirmation.

## Telemetry Included

The app parses and displays these fields when returned:

- `fuelPercent`
- `coolantTemp`
- `raceChargeActive`
- `raceChargeTarget`
- `raceChargeEtaMin`

Missing fields are nullable and do not overwrite previously known values.

DisplayMirror has internal vehicle fields beyond this list, but the app only exposes values that the remote protocol sends to clients.

## v1.3 Additions

- `navigate`: accepts coordinates, Google Maps links, `geo:` links, Google navigation links, or place queries shared from Android.
- `get_location`: reads the current car location when DisplayMirror returns latitude and longitude.
- `mirror`: sends `fold` and `unfold` actions.
- Foreground refresh sends `status`, `climate status`, `parking_charge status`, and `race_charge status` every 3 seconds while connected.

## v1.4.1 Notes

- Google Maps shares may arrive as long URLs, short `maps.app.goo.gl` URLs, `/@lat,lon` paths, or embedded `!3d...!4d...` data. The phone app resolves redirects and extracts coordinates before sending `navigate`, so the head unit receives a clean location instead of a raw Google URL.
- Shared destinations are stored locally for resend/delete history. The history is phone-local and not synced.
- DisplayMirror protocol v4 exposes `climate` `ac_on` and `ac_off`, but the reviewed source maps that to the A/C compressor state. The phone app therefore treats cabin air on/off as fan speed control (`0` for off, a low fan speed for on) and keeps compressor control separate.
- No remote Auto climate command was found in the reviewed DisplayMirror protocol. Auto mode is not exposed by the phone app.
- Auto defrost is hidden because the returned state was not reliable enough for end-user control.

## Intentionally Excluded

The inspected DisplayMirror source did not provide a reliable remote surface for:

- rear/ceiling screen control
- ignition or power mode
- odometer
- EV range
- gear
- steering angle
- live hazards, DRL, or mirror status in the general status payload
- full HVAC power command separate from fan speed
- Auto climate mode command
- park keep-alive / head-unit awake controls

Do not expose UI for these fields unless a future DisplayMirror release clearly sends them over the remote protocol.

## Lock State

DisplayMirror lock-state semantics can depend on how the head unit maps `doorLockState`. The app should remain calibration-friendly and avoid hard-coded assumptions beyond what has been validated on the real vehicle.
