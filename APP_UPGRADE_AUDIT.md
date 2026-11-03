# AtomFan Application Upgrade & Architecture Audit Report

## 1. Executive Summary
**AtomFan** is an Android application (`com.example.atomfan`) designed to provide comprehensive remote control, telemetry monitoring, IR code editing, brute-force code finding, and hardware learning capabilities for **Atomberg Smart Fans** (e.g., Renesa, Studio, Gorilla series).

The app supports **three distinct transport modes**:
1. **Phone Built-in Consumer IR (`PHONE_IR`)**: Sends modulated 38kHz NEC IR signals directly via the Android phone's IR blaster.
2. **Atomberg Direct BLE (`FAN_BLE`)**: Connects directly to Atomberg fans over Bluetooth Low Energy to decode real-time 4-byte status telemetry notifications (power, speed, boost, LED, sleep, timer) and perform experimental GATT control writes.
3. **ESP32 IR Transceiver Bridge (`ESP32_BRIDGE`)**: Interfaces with an external ESP32 bridge over BLE Nordic UART Service (NUS) to send NEC/RAW IR commands and learn IR codes from original remotes.

---

## 2. Technical Stack & Build Configuration
- **Namespace / Package**: `com.example.atomfan`
- **Compile SDK**: `37`
- **Target SDK**: `34`
- **Min SDK**: `26` (Android 8.0+)
- **Languages & Frameworks**: Kotlin 2.0+, Jetpack Compose, Material 3, Kotlin Coroutines, StateFlow/SharedFlow.
- **Persistence & Serialization**: Jetpack DataStore Preferences (`atomfan_settings`) + `kotlinx.serialization.json`.
- **Hardware Integration**: Android `ConsumerIrManager` (IR Transmit API), Android `BluetoothLeScanner` & `BluetoothGatt` (BLE API 26-34+ compatible).
- **Test Infrastructure**: JUnit 4, Kotlinx Coroutines Test, DataStore Temporary DataStore Test Fixtures (22 passing unit tests).

---

## 3. Architecture & Subsystem Inspection

### 3.1 Data Models & State Representation (`model/Models.kt`)
- **`RemoteButton`**: Enum covering all 10 physical Atomberg remote controls: `POWER`, `SPEED_1`..`SPEED_5`, `BOOST`, `TIMER`, `LED`, `SLEEP`.
- **`TransportMode`**: Enum for hardware routes: `PHONE_IR`, `FAN_BLE`, `ESP32_BRIDGE`.
- **`IrCode`**: Data class holding protocol (`"NEC"` or `"RAW"`), 8-bit `address`, 8-bit `command`, `frequency` (Hz), list of `rawTimings`, and `comment`.
- **`FanState`**: Data class representing parsed fan telemetry: `powerOn`, `speed` (1..5 or 6 for boost), `led`, `boost`, `sleep`, `timer` (hours), `rawBytesHex`, `lastUpdatedTimestamp`.
- **`BleLogEntry`**: Telemetry log entry with timestamp formatting and GATT status error flags.

### 3.2 Repositories & Data Persistence (`data/CodeRepository.kt`)
- Implements asynchronous `Flow`-based reading and DataStore editing for:
  - Active `TransportMode` (`KEY_TRANSPORT_MODE`)
  - IR Repeat Count (`KEY_REPEAT_COUNT`, 1x or 2x)
  - Experimental BLE Write flag (`KEY_EXPERIMENTAL_BLE_WRITE`)
  - Custom IR Code Mappings (`KEY_CUSTOM_IR_CODES` serialized JSON string)
- Fallback loading from raw resource JSON (`res/raw/ir_codes.json`) if present.

### 3.3 Hardware IR Engine & NEC Protocol (`ir/NecEncoder.kt`, `transport/PhoneIrTransport.kt`)
- **`NecEncoder`**:
  - Generates exact 67-timing pulse/space arrays for 32-bit NEC protocol (Header: 9000µs mark / 4500µs space; 32 data bits composed of LSB-first 8-bit address, inverted address, 8-bit command, inverted command; bit 0 = 560µs/560µs, bit 1 = 560µs/1690µs; Stop mark: 560µs).
  - Finds nearest carrier frequency supported by device hardware (`getNearestSupportedFrequency`).
- **`PhoneIrTransport`**:
  - Wraps `ConsumerIrManager`. Checks `hasIrEmitter()` and handles multi-transmit repeat count (1x/2x) with inter-frame delays.

### 3.4 Direct Atomberg BLE Subsystem (`ble/FanBleManager.kt`, `transport/FanBleTransport.kt`)
- **Service & Characteristic UUID**: `e29ee02c-af3d-11ec-b909-0242ac120002`
- **CCCD Descriptor**: `00002902-0000-1000-8000-00805f9b34fb`
- **Telemetry Decoding (`decodeFanState`)**:
  - Decodes 4-byte notifications into `FanState`:
    - `byte0 & 0x07`: Speed (1..5, 6 for boost)
    - `byte0 & 0x10`: Power state (ON/OFF)
    - `byte0 & 0x20`: LED state
    - `byte0 & 0x40`: Boost mode
    - `byte0 & 0x80`: Sleep mode
    - `byte1`: `0xF8` when running, `0x00` when off
    - `byte2`: Timer countdown hours (0 = OFF)
- **Automatic Reconnection**:
  - Implements exponential backoff reconnects (1s up to 30s) upon accidental disconnection.
- **Experimental Control Writes**:
  - Converts IrCode address/command into hex bytes and attempts direct GATT characteristic write with bonding check (`createBond()`).

### 3.5 ESP32 Hardware Bridge Subsystem (`ble/Esp32BleManager.kt`, `ble/Esp32LineParser.kt`, `transport/Esp32Transport.kt`)
- **Nordic UART Service (NUS)**:
  - Service: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
  - RX: `6e400002-...` (Write)
  - TX: `6e400003-...` (Notify)
- **Stream Line Parser (`Esp32LineParser`)**:
  - Reassembles fragmented BLE notifications using an internal `StringBuilder` buffer.
  - Parses ASCII lines into `Esp32Response` objects: `Pong`, `Ok`, `Error`, `LearnedNec(address, command)`, `LearnedRaw(frequency, timings)`, `Unknown`.
- **Command Set**: `PING`, `SEND NEC <addr> <cmd>`, `SEND RAW <freq> <timings>`, `LEARN`, `CANCEL`.

### 3.6 Presentation & ViewModels (`viewmodel/`, `ui/`)
- **`MainViewModel`**:
  - Manages active transport mode selection, IR emitter availability check, dialog states (Timer/Sleep), and verification feedback loop.
  - **Verification Telemetry Loop**: When sending IR signals via Phone IR or ESP32, if Fan BLE is simultaneously connected, listens for an incoming BLE telemetry notification within 1000ms to confirm whether the fan physically responded.
- **`CodeEditorViewModel`**:
  - Provides full editing capabilities for NEC address/command, RAW timings, frequency, comments, live test transmission, and persistence.
- **`CodeFinderViewModel`**:
  - Executes automated NEC address/command range brute-force sweeps (50ms - 1000ms interval per code).
  - Integrates with `FanBleManager` telemetry stream: automatically flags a "HIT" when a BLE state change is detected within 400ms after an IR transmission.
- **UI Screens & Components**:
  - `MainScreen`: Hosts `AtombergRemoteWidget` (circular speed layout, power, boost, timer, LED, sleep), energy statistics card, transport mode selector.
  - `BleDashboardScreen`: Real-time GATT logger, RSSI scanner, 4-byte telemetry state viewer, log exporter.
  - `Esp32LearnScreen`: Interactive workflow for putting ESP32 in learning mode, capturing signals, testing, and saving to repository.
  - `CodeEditorScreen` & `CodeFinderScreen`: Parameter configuration, progress bar, hit list.

---

## 4. Feature Matrix: Fully Implemented vs. Partially Implemented vs. Missing

| Subsystem / Feature | Status | Details & Observations |
|---|---|---|
| **Physical Atomberg Remote UI** | **Fully Implemented** | Full M3 remote control layout with Power, 1-5 Speed ring, center Boost button, Timer, LED, Sleep controls, and haptic feedback. |
| **Phone Built-in IR Transmit** | **Fully Implemented** | Full 32-bit NEC encoder, carrier frequency matching, ConsumerIrManager integration, repeat count toggle. |
| **Atomberg Direct BLE Telemetry** | **Fully Implemented** | GATT service discovery, notification listener, CCCD write, 4-byte status decoder, live telemetry dashboard, auto-reconnect backoff. |
| **ESP32 Bridge Communication** | **Fully Implemented** | Nordic UART Service protocol, line stream reassembly buffer, SEND NEC/RAW, PING, LEARN, CANCEL. |
| **ESP32 IR Learning Workflow** | **Fully Implemented** | Interactive learning screen, 10s timer timeout, live parsing of `LEARNED NEC` / `LEARNED RAW`, code testing, repository saving. |
| **IR Code Editor** | **Fully Implemented** | Button selection, protocol toggle (NEC/RAW), address/command dec/hex formatting, frequency editing, comment field, test & save routines. |
| **Brute-Force Code Finder** | **Fully Implemented** | Configurable NEC address/command sweep, interval slider, start/pause/resume/stop controls, auto-response detection via Fan BLE telemetry (400ms window). |
| **BLDC Energy & Savings Calculator** | **Fully Implemented** | Real-time power calculation (0W to 45W), daily kWh estimation (8h baseline), yearly savings comparison vs. conventional induction fans. |
| **Timer & Sleep Schedule Dialogs** | **Fully Implemented** | Custom stepped timer dialog (30m - 8h) and sleep schedule picker (start/end times + day of week selector). |
| **Unit Test Coverage** | **Fully Implemented** | 22 comprehensive unit tests for `Esp32LineParser`, `NecEncoder`, `MainViewModel`, `CodeEditorViewModel`, and `CodeFinderViewModel`. All tests passing. |
| **Navigation System** | **Partially Implemented** | Currently uses custom destination enum state (`rememberSaveable`) in `MainActivity`. Needs migration to Jetpack Navigation 3 (`androidx.navigation3`). |
| **Adaptive Layouts** | **Partially Implemented** | Supports Edge-to-Edge display, but UI is currently single-column vertical scroll. Lacks multi-pane adaptive layouts (`ListDetailPaneScaffold`) for tablets/foldables. |
| **Theme System** | **Partially Implemented** | Standard M3 theme exists, but lacks dynamic color (`dynamicLightColorScheme`/`dynamicDarkColorScheme` for Android 12+) and expressive design tokens. |
| **Experimental Direct GATT Control** | **Partially Implemented** | Direct GATT write logic exists, but direct GATT control for Atomberg fans is marked experimental due to firmware model variances. |

---

## 5. Key Architecture Strengths & Upgrade Recommendations

### Strengths
1. **Clean Separation of Transports**: The `FanTransport` interface cleanly abstracts `PhoneIrTransport`, `FanBleTransport`, and `Esp32Transport`, making ViewModel code decoupled from hardware details.
2. **Robust Stream Parsing**: `Esp32LineParser` correctly handles BLE UART chunk fragmentation across notification boundaries.
3. **Smart Verification Loop**: Telemetry feedback loop in `MainViewModel` and `CodeFinderViewModel` provides real visual verification ("fan responded") by monitoring BLE notifications immediately after IR transmission.
4. **Solid Test Foundation**: Core unit tests cover protocol encoding, line parsing, ViewModel state transitions, and repository interactions.

### Recommendations for Future Phases
1. **Navigation Modernization**: Upgrade from custom enum state navigation to Jetpack Navigation 3 (`androidx.navigation3`).
2. **Adaptive Layout Strategy**: Incorporate Compose Material Adaptive (`ListDetailPaneScaffold`) for multi-pane tablet/foldable support.
3. **Dynamic Material 3 Theming**: Implement wallpaper-based dynamic color schemes (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) with expressive fallback palettes.
4. **Logging & Diagnostics**: Expand BLE log export capabilities and error handling for peripheral connection edge cases.

---

## 6. Verification Status
- **Build Status**: Successful (`./gradlew :app:assembleDebug`)
- **Unit Tests**: 22 Passed, 0 Failed (`./gradlew :app:testDebugUnitTest`)
- **Audit Completion**: Verified all source files, transport layers, models, ViewModels, UI screens, and test suites.
