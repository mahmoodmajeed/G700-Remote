# G700 Remote — v3 Upgrade (DisplayMirror 3.2 compatibility)

This document is the engineering spec for upgrading G700 Remote from the DisplayMirror
protocol-v4 / BLE+LAN baseline (app v1.6.8) to full **DisplayMirror 3.2** compatibility:
cloud accounts, cloud-relay remote control, QR pairing, car cameras, sentinel alerts,
scenes, audio, and cabin cooling.

Source of truth: reverse-engineering of `DisplayMirror-v3.2.0.apk` (== v3.1.0 functionally;
v3.2 only adds a headunit-side `MANAGE_EXTERNAL_STORAGE` auto-grant guard). Detailed RE
notes live in `decompiled-v3.1/analysis/01..04`.

## 1. Cloud backend (PocketBase)

- REST base:  `https://car-api.wowbooking.one`
- WS relay:   `wss://car.wowbooking.one`
- Shared `fleetKey`: `7VH_r1OpXzkO_jSQkJdaxdymEgidZoshiWNagNMOihk` (sent on custom car endpoints).

Auth (the phone uses these two):
- Login:  `POST /api/collections/users/auth-with-password`  body `{identity,password}` → `{token, record}` (PocketBase JWT)
- (Register is done on the headunit; phone is login-only per product decision.)
- All authenticated calls: header `Authorization: Bearer <jwt>`.

Account ↔ car binding & sync (phone side):
- Bind the scanned car to the account by redeeming the QR `pair` token against the cloud.
- Settings sync: `POST /api/pull-settings` `{carId}` → `{data}` (pull on first login),
  `POST /api/push-settings` `{carId,data}` (debounced push). Car-scoped `display_prefs`.

Token handling: store JWT in encrypted prefs; no refresh endpoint — re-auth on 401.

## 2. QR pairing payload (shown on headunit)

Single UTF-8 JSON object:

```json
{ "v":1,
  "relay":"wss://car.wowbooking.one",
  "api":"https://car-api.wowbooking.one",
  "car":"car-<androidId>",
  "pair":"<one-time bind token>",
  "code":"<4-8 digit pairing code>" }
```

Phone flow: **login → scan QR → redeem `pair` (bind car to account) → save car
{carId, relay, api, code} → connect relay → inner `hello` with `code`.**
The 6-digit `code` is still the secret the headunit checks in `hello`; QR just carries it.

## 3. Transport (now three)

`hello` handshake is identical across all transports:
`{"cmd":"hello","protocolVersion":4,"pairingCode":"<code>"}` →
`{"type":"helloResult","success":true,"protocolVersion":4}`.

- BLE GATT (unchanged UUIDs), LAN/mDNS TCP 9274 (unchanged) — local.
- **NEW Cloud relay (WebSocket).** Phone connects to `wss://car.wowbooking.one` (phone leg),
  authenticated with the JWT, targeting `carId`. Frames are JSON envelopes
  `{"t":"open|msg|close","sid":<session>,"d":<one protocol line>}`. `d` carries the same
  newline-JSON protocol used over BLE/LAN. Implemented with OkHttp `WebSocket`.

Connection policy: prefer local (BLE/LAN) when reachable; fall back to Cloud for remote.
Live camera view is gated to local transports (bandwidth); snapshot works on cloud.

## 4. Protocol command surface (protocol still v4, many new commands)

Existing (kept): hello, ping, status, lock, unlock, window, sunroof, sunshade, hazards, drl,
mirror, soc, parking_charge, race_charge, climate(*), get_location, navigate.

NEW commands to add:
- Climate: `climate hvac_on` / `hvac_off`, `front_heat_on` / `front_heat_off` (front_heat already modelled).
- `cameras` → `{type:cameraList, cameras:["0","1",...]}`
- `snapshot {camera,id}` → async `{type:snapshot, ok, w, h, data:<b64 jpeg>, id}` (cloud caps 1280px/Q75)
- `live_view {action:start|stop, camera}` → `{type:liveView,state}` then pushed
  `{type:liveFrame, seq, w, h, data:<b64 jpeg>}` (~12fps/480px/Q55; LAN only)
- `sentinel {action:start|stop}` → `{type:sentinel,...}` then pushed
  `{type:sentinelAlert, event:3|4|5, eventName, time, thumb:<b64 jpeg>}`
- `scene {scene:bigbed|cinema|rescue|pet|carwash|lightshow|resting|romance}`
- `audio` (read) + `audio_set {loudness,surround,eq_mode,balance,fade}`
- `cabin_cooling {action:enable|disable|set_config|set_schedule|trigger_now|get_config|status}`
- (`telemetry`, `cabin`, `obd` exist on the headunit; OBD excluded from this release.)

New status fields already present: coolantTemp, fuelPercent, chargeMode enums, race/parking
charge fields (all already modelled). No new mandatory status fields for the phone.

Handshake errors to handle: `update_required`, `pairing_code_required`, `bad_pairing_code`,
`locked_out` (5 fails → 60s backoff, doubling to 1h). `camera_busy`, `transport_unsupported`
(camera over BLE), `unknown_cmd`.

## 5. Information architecture (phone app)

Onboarding (new): Welcome → **Login** → **Scan QR** (bind) → permissions → Home.
Manual/local fallback retained: enter code + pick BLE/LAN device; demo mode retained.

Bottom nav (5 tabs), merging thin sections to make room for Cameras:
1. **Home** — lock/unlock hero, telemetry, quick actions, location, account+transport state.
2. **Climate** — existing climate + new hvac_on/off, front heat, **Audio** + **Cabin cooling** sections.
3. **Controls** — merged **Openings (windows/sunroof/sunshade)** + **Lighting (hazards/DRL)** + **Mirror** + **Scenes**.
4. **Camera** — camera picker, snapshot, live view (local), **Sentinel** arm + alerts.
5. **Charge** — existing charging (SOC, parking charge, race charge, telemetry).

Settings (header gear) gains an **Account** section (email, signed-in car(s), sign out,
cloud on/off, re-scan QR) plus existing connectivity/pairing/theme/security/diagnostics.
Links (navigation history) stays in the header as today.

## 6. Build order (phases)

1. Deps (OkHttp, ML Kit barcode + CameraX) + protocol extensions (commands/responses).
2. Cloud REST client (login, bind, settings sync) + account models + secure storage.
3. CloudRelayTransport (WebSocket) + Composite transport integration + connection policy.
4. Onboarding/Login/QR-scan UI + entry-flow gating.
5. Feature UIs: Camera(+Sentinel), Controls merge, Climate additions, Scenes.
6. Settings Account section, analytics, docs; version bump; build/test; sign; commit + tag; push.

## 6b. Verified cloud contract (live-tested against the running backend)

Tested with a real account + a real car QR. Confirmed:

- **Login** `POST /api/collections/users/auth-with-password {identity,password}` → `{token, record}`.
  `record.verified` can be `false` (admin approval) while login still returns a token.
- **QR redemption / car binding** — the phone calls **`POST /api/adopt-car`** with the QR
  **`pair` token in place of `carToken`**: `{carId, carToken: <pair>, fleetKey}` + `Authorization: Bearer <jwt>`
  → `200 {"claimed":true,"pairingCode":"<code>","relayUrl":"wss://…"}`. This binds the car to the
  account. (There is **no** `claim-car`/`redeem-pairing` endpoint — those 404.)
- **Settings sync** `POST /api/pull-settings {carId}` + Bearer → `200 {"data":…}` once the car is
  adopted; before adoption it returns `403 {"error":"not your car"}`. `push-settings` mirrors it.
- **Owned cars** `GET /api/collections/cars/records` + Bearer → the account's cars
  (`{car_id, name, online, owner, last_seen}`).
- **Relay phone leg** `wss://car.wowbooking.one/ws/phone` exists (returns 401, not 404) but its
  WebSocket auth is enforced by an opaque edge worker. JWT (Bearer / X-Auth-Token / query),
  the `pair` token, and cookies all returned 401, and the car was offline so end-to-end could not
  be exercised. `POST /api/relay-auth` exists but is an internal validator (ignores client auth).

### Assumptions / open items
- **Relay phone-leg auth is unverified.** We send `X-Car-Id` + `X-Auth-Token`(jwt) + Bearer + fleetKey
  as a best guess. Cloud *remote* control needs one calibration pass with an **online** car. All of
  it is centralized in `cloud/CloudConfig.kt` + `cloud/CloudRelayClient.kt`. This never blocks local
  control.
- **Cloud requires account approval** (`verified`/activation). Until approved, REST cloud ops return
  403; we surface a clear "account not activated yet" message and fall back to local control.
- **Local control is the reliable path:** QR provides the pairing `code`; the app then auto-discovers
  the car over BLE (service-UUID scan) or LAN (mDNS) and completes the `hello` handshake with that
  code — no account or cloud approval required.

## 7. Notes / limits

- Sentinel/camera-push alerts arrive only while connected (PocketBase backend does not push
  FCM to this app); true app-closed push is not available via this backend.
- Live dashcam clips remain LAN-only Hikvision (not remoteable); only sentinel events + thumb.
- Not an OEM-certified key; same safety posture (local biometric/PIN gate on sensitive actions).
</content>
