# AtomFan Remote

**AtomFan Remote** is an advanced, multi-transport Android application designed to control Atomberg smart fans and other IR/BLE appliances. It supports three distinct transport modes:
1. **Phone IR** (Direct infrared transmission using built-in IR blaster hardware with NEC and Raw protocol encoding).
2. **Fan Direct BLE** (GATT control and real-time state notification decoding for Atomberg BLE fans).
3. **ESP32 Bridge** (ESP32 Bluetooth-to-IR bridge device forwarding commands to legacy infrared appliances).

---

## Features

- **Multi-Transport Remote Control**: Seamlessly switch between Phone IR blaster, Fan BLE direct connection, and ESP32 Bridge.
- **Visual Status & Feedback**: Real-time fan state tracking (Power, Speed 1..5, LED, Sleep, Timer) and verification indicators.
- **In-App Code Editor**: Select any remote button (Power, Speed 1..5, Boost, Timer, LED, Sleep), view/edit protocol (NEC or Raw), address, command, frequency, and custom timings, test transmission instantly, and persist changes via DataStore.
- **Code Finder & NEC Sweep**: Automated NEC address and command sweep (Mode A & C) with real-time BLE notification response detection (Mode B feedback within 400ms) to log and highlight successful hits ("HIT! Fan responded at Address X, Command Y").
- **ESP32 Learning Mode**: Scan, connect, and learn IR codes from physical remote controls via ESP32 bridge hardware.
- **Material Design 3 (M3)**: Dynamic color themes, edge-to-edge support, and responsive layouts.

---

## Build Instructions

### Prerequisites
- **Android Studio**: Iguana / Jellyfish or newer (Ladybug / Meerkat compatible).
- **JDK**: JDK 17 or higher.
- **Android SDK**: API 34+ (Target SDK 34, Min SDK 26).

### Building via Command Line
Open a terminal at the project root (`D:/project/AtomFan2`) and run:

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## Runtime Permissions

AtomFan Remote requires certain runtime permissions depending on the features and transport modes used:

1. **IR Transmission (`TRANSMIT_IR`)**:
   - Required for Phone IR blaster mode. Granted automatically on devices supporting consumer IR.
2. **Bluetooth Permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`)**:
   - Required for Fan Direct BLE and ESP32 Bridge modes.
   - On Android 12+ (API 31+), the app prompts for runtime Bluetooth permissions when scanning or connecting.
   - On Android 11 and below, `ACCESS_FINE_LOCATION` may be required for BLE scanning.

---

## IR Codes Configuration

Default IR codes are bundled in `app/src/main/res/raw/ir_codes.json`. You can inspect or edit this JSON file directly to update default infrared parameters for each button.

Alternatively, you can use the **in-app Code Editor** screen:
1. Tap the **Edit (Pencil)** icon in the top app bar.
2. Select any remote button (e.g., Power, Speed 1, Boost).
3. Modify protocol (`NEC` or `Raw`), address (decimal or hex), command (decimal or hex), frequency (e.g., `38000 Hz`), or raw timing list.
4. Tap **"Test Code"** to transmit instantly using the active transport.
5. Tap **"Save Changes"** to persist your configuration to DataStore.

---

## BLE Troubleshooting & GATT Status Explanation

When connecting to BLE devices (Atomberg Fan or ESP32 Bridge), you may encounter GATT status codes in the connection logs (`BLE Dashboard`). Here is a quick reference guide:

- **GATT 0 (`GATT_SUCCESS`)**: Operation completed successfully.
- **GATT 8 (`GATT_CONN_TIMEOUT`)**: Connection timed out. Ensure the fan or ESP32 bridge is powered on, within range, and not already paired/connected to another phone.
- **GATT 19 (`GATT_CONN_TERMINATE_PEER_USER`)**: The remote device disconnected the connection.
- **GATT 133 (`GATT_ERROR`)**: A general GATT client error (common on Android during rapid connect/disconnect cycles). 
  - *Troubleshooting*: Toggle Bluetooth off and on, restart app, or ensure location services are enabled for BLE scanning.
- **Experimental BLE Write**: If standard BLE commands fail, enable "Experimental BLE Write" in settings to send direct 2-byte HEX payloads (`%02X%02X` for address and command) over GATT characteristics.
