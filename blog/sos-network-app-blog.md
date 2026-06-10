# SOS Network App: Decentralized Emergency Mesh for Android

## Overview

SOS Network is an innovative Android application that leverages **peer-to-peer (P2P) mesh networking** and **Google Firebase services** to create a resilient emergency alert system. This app operates independently of any central server, ensuring alerts reach recipients even when infrastructure is compromised.

![App Architecture Diagram](./architecture.png)

## Key Features

### 1. Decentralized P2P Mesh Network 🕸️

Powered by **Google Nearby Connections**, the SOS Network creates a self-healing mesh where each device acts as both sender and relay:

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│   Alice  │────▶│    Bob   │────▶│   Charlie │
│(SOS User)│     │(Relay)   │     │ Receives  │
└──────────┘     └──────────┘     └──────────┘
```

**How it works:**
- **Hop-by-hop propagation**: Alerts travel from device to device via Bluetooth Low Energy and Wi-Fi Direct
- **Cluster strategy (P2P_CLUSTER)**: Maintains stable connections while minimizing bandwidth usage
- **Decentralized routing**: No single point of failure; the network adapts dynamically

### 2. Firebase Cloud Messaging Integration ☁️

For cross-device notifications, we integrate **Google Firebase Cloud Messaging (FCM)**:

```
┌──────────┐    ☁️     ┌──────────┐
│   Phone1 │─────[FCM]→│  Phone2  │
│ (SOS)    │    Google │ (Alert)  │
└──────────┘    Cloud  └──────────┘
```

**Benefits:**
- Instant push notifications on any of your registered devices
- Background delivery even when app is closed
- Reliable cloud fallback for long-range communications

### 3. Crashlytics Integration 🛡️

Production-ready with **Firebase Crashlytics** for real-time crash reporting and analytics.

## Use Cases

### 🚨 Emergency Scenarios

| Scenario | How SOS Network Helps |
|----------|----------------------|
| **Natural Disaster** | Works when cellular/tower infrastructure is down; devices form local mesh network |
| **Mass Shooting** | Silent SOS alerts propagate silently to nearby phones without triggering panic |
| **Medical Emergency** | Location-based alerts with GPS coordinates sent hop-by-hop to responders |
| **Car Crash** | Automatic alert with device location relayed through available neighbors |

### 🏙️ Community Applications

- **Student Campuses**: Dorm room-to-dorm room emergency comms during building lockdowns
- **Event Security**: Large gatherings (concerts, festivals) where crowd density exceeds cell tower capacity
- **Off-Grid Locations**: Hiking groups, sailing expeditions, maritime vessels without cellular coverage
- **Disaster Response Teams**: Field operations teams needing offline mesh communication

## Technical Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    SOS Network App                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐    ┌──────────────────┐               │
│  │  SosMeshService  │◀──▶│  FcmService      │               │
│  │ (P2P Mesh Layer) │    │ (Cloud Layer)    │               │
│  └──────────────────┘    └──────────────────┘               │
│         ▲                     ▲                             │
│         │                     │                             │
│         ▼                     ▼                             │
│  ┌─────────────────────────────────────────────┐            │
│  │        Room Database + DataStore             │            │
│  │           (User Registry & History)         │            │
│  └─────────────────────────────────────────────┘            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **P2P Mesh** | Google Nearby Connections v24.0.0 | Device-to-device discovery & communication |
| **Cloud Push** | Firebase Messaging SDK v24.1.2 | Cross-device notifications |
| **Crash Reporting** | Firebase Crashlytics v19.3.0 | Production monitoring |
| **Location** | Play Services Location v21.3.0 | GPS tracking for SOS alerts |
| **Database** | Room + DataStore | Local persistence & preferences |
| **Protocol** | Custom SOS-DP Protocol V1 | Structured alert data format |

## Security Considerations

The app is designed with multiple layers of security:

- **No Central Server**: Alerts don't pass through any third-party server
- **Encrypted Payloads**: Message contents end-to-end encrypted in mesh routing
- **Identity Verification**: Registration-based trust model with optional admin verification
- **Location Privacy**: Optional location sharing; can disable per session

## Setup Guide

### Prerequisites

1. Firebase project created at [console.firebase.google.com](https://console.firebase.google.com)
2. Android Studio with Gradle 8.9+
3. Google Cloud SDK (optional, for advanced features)

### Configuration

```kotlin
// app/build.gradle.kts
dependencies {
    // Firebase Core & Crashlytics
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-crashlytics-ktx:19.3.0")
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")
    
    // Nearby Connections for P2P mesh
    implementation("com.google.android.gms:play-services-nearby:24.0.0")
}

// Add GoogleServices plugin to root build.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

### Firebase Console Setup

1. Create a new project or add existing Android app
2. Download `google-services.json` → place in `app/src/main/`
3. Enable Crashlytics in project settings
4. Register device package name for FCM

## Testing Checklist

- [ ] Mesh discovery works within Bluetooth range (≈50m)
- [ ] Wi-Fi Direct fallback activates when Bluetooth fails
- [ ] FCM notifications received on separate devices
- [ ] Alert propagation maintains hop-by-hop integrity
- [ ] Location accuracy meets emergency requirements (<10m typical)
- [ ] Battery consumption acceptable for prolonged use

## Open Source Contributions

The protocol definition and core mesh logic are available for review in the `protocol/` module. The architecture supports adding new features without compromising decentralization.

---

## License

Apache 2.0 - See LICENSE file for details.

## Contact

For questions or collaboration opportunities, see the [GitHub repository](./README.md).
