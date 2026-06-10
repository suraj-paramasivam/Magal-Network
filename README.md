# SOS Network — Decentralized Android SOS App

Android app for community emergency alerts without a central alert server. When someone presses **SOS**, nearby peers receive their location plus an optional text or voice note over a decentralized mesh (SOS-DP).

## Features

- **One-tap SOS** with GPS location, optional message, optional voice note
- **Decentralized mesh** via Google Nearby Connections (Bluetooth / Wi-Fi Direct)
- **Hop relay** so alerts can propagate beyond direct radio range (up to 8 hops)
- **Government ID verification** — documents encrypted on device; only a hash is broadcast
- **Admin verification** — verify ID and **in-person physical presence** with signed attestations
- **Full-screen incoming alert** UI with open-in-maps

## Project structure

```
sos-android/
├── app/          # Android UI, mesh service, verification
├── protocol/     # SOS-DP protocol, crypto, attestation validation
├── PROTOCOL.md   # Protocol specification
└── README.md
```

## Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Physical devices recommended (Nearby Connections is limited on emulators)
- Android 8.0+ (API 26)

## Build & run

1. Open `sos-android` in Android Studio.
2. Let Gradle sync and download dependencies.
3. Run on two or more physical phones with Bluetooth and Location enabled.
4. Grant all requested permissions on both devices.

```bash
cd sos-android
./gradlew :app:assembleDebug
```

## User flow

1. **Register** with a display name.
2. Optionally **upload government ID** (photo/PDF) — stored encrypted locally.
3. Paste your **admin's public key** if you have one (shown on the admin device).
4. Keep the app running so the **mesh foreground service** stays active.
5. Press **SOS** in an emergency.

## Admin flow

1. On the admin phone: open **Admin verification** from the main screen (always visible).
2. Tap **Enable admin on this device** (dev/bootstrap — production should use secure provisioning).
3. Copy the **admin public key** and share it with users (registration screen).
4. When users submit ID, requests appear in the admin list (via mesh `VERIFICATION_REQUEST`).
5. **Verify ID** — inspect the user's physical document on their phone, then confirm.
6. **Verify in person** — after meeting the user face-to-face, confirm physical presence.

Attestations are broadcast on the mesh; the user's app updates their verification badge automatically.

## Testing with two phones

| Step | Device A (admin) | Device B (user) |
|------|------------------|-----------------|
| 1 | Enable admin, copy public key | Register, paste admin key, submit ID |
| 2 | Verify ID + physical in admin UI | — |
| 3 | — | Press SOS |
| 4 | Receive full-screen alert | — |

## Legal & safety disclaimer

This is a **reference implementation** for development and research. Real-world emergency systems require regulatory compliance, professional security review, integration with emergency services (e.g. 112/911), and reliability testing. Do not rely on this app alone for life-threatening situations.

## Future enhancements

- Third-party KYC provider integration (Onfido, etc.)
- Encrypted P2P ID document transfer for remote admin review
- BLE mesh fallback without Google Play services
- iOS client with compatible SOS-DP

See [PROTOCOL.md](PROTOCOL.md) for wire format details.
