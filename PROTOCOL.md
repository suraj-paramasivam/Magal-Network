# SOS Decentralized Protocol (SOS-DP) v1.0

SOS-DP is a peer-to-peer emergency alert protocol used by **SOS Network**. It requires no central server for alert delivery.

## Design goals

- **Decentralized propagation**: Alerts hop across nearby devices (mesh relay).
- **Privacy**: Raw government ID documents never appear in protocol messages.
- **Verifiable identity**: Admins issue signed attestations; devices validate signatures locally.

## Wire format

```
[ magic: 4 bytes "SOS1" ][ version: 1 byte ][ JSON envelope UTF-8 ]
```

### Envelope fields

| Field | Description |
|-------|-------------|
| `id` | Unique message ID (deduplication) |
| `type` | Message type enum |
| `sender` | Sender peer UUID |
| `ts` | Unix epoch milliseconds |
| `hop` | Relay hop count (max 8) |
| `ttl` | Suggested validity window (seconds) |
| `sig` | Optional ECDSA signature (future) |
| `payload` | Type-specific JSON string |

## Message types

| Type | Purpose |
|------|---------|
| `HELLO` | Peer discovery handshake |
| `SOS_ALERT` | Emergency alert with location + optional text/voice |
| `SOS_ACK` | Responder acknowledgement |
| `RELAY` | Opaque relay wrapper (reserved) |
| `IDENTITY_ANNOUNCE` | Signed identity + verification level |
| `ADMIN_ATTESTATION` | Admin-signed verification record |
| `VERIFICATION_REQUEST` | Broadcast verification queue entry (hash only) |

## SOS alert payload

```json
{
  "alertId": "uuid",
  "location": { "latitude": 0.0, "longitude": 0.0, "accuracyMeters": 10.0 },
  "messageText": "optional",
  "voiceNoteMime": "audio/mp4",
  "voiceNoteBase64": "...",
  "severity": "CRITICAL",
  "senderDisplayName": "Alex",
  "senderVerificationLevel": "PHYSICALLY_VERIFIED"
}
```

## Identity & verification

1. User generates an **EC P-256** key pair on device.
2. Government ID is stored **encrypted at rest**; only a **SHA-256 hash** is announced on the mesh.
3. Trusted **admin public keys** are pinned on each device (bootstrap).
4. Admins issue `ADMIN_ATTESTATION` messages:
   - `GOVERNMENT_ID_VERIFIED` — document reviewed (typically on user's phone in person).
   - `PHYSICAL_PRESENCE_VERIFIED` — admin met the user in person.

Verification levels (ordered): `UNVERIFIED` → `ID_SUBMITTED` → `ID_VERIFIED` → `PHYSICALLY_VERIFIED`.

## Transport

Implementation uses **Google Nearby Connections** (`P2P_CLUSTER`) over Bluetooth / Wi-Fi. This is transport-layer only; SOS-DP payloads are transport-agnostic.

## Relay rules

- Each device deduplicates by `messageId`.
- Messages with `hop >= 8` are not relayed further.
- Best-effort fan-out to all connected endpoints.

## Security considerations

- Voice notes and large payloads increase mesh airtime; production deployments should cap size and prefer text.
- Admin key bootstrap is critical — use QR/in-person distribution, not public channels.
- This reference app is **not** certified for life-safety use without legal, security, and infrastructure review.
