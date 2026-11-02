# Project Plan

Build a native Android app (Kotlin, Jetpack Compose, minSdk 26, targetSdk 34) called "AtomFan Remote" that controls an Atomberg ceiling fan with three selectable modes:
A) PHONE IR: Phone's built-in IR blaster sends IR codes (ConsumerIrManager, NEC encoder, frequency matching for 30, 33, 36, 38, 40, 56 kHz).
B) FAN BLE: Direct Bluetooth LE connection to fan (scan Atomberg_, BluetoothGatt, notify/read/write characteristic e29ee02c-af3d-11ec-b909-0242ac120002, 4-byte payload decoder, live dashboard, raw hex log view + export, experimental BLE control write, GATT status handling, bond handling, auto-reconnect).
C) ESP32 BRIDGE: BLE connection to ESP32 (Nordic UART service 6e400001-b5a3-f393-e0a9-e50e24dcca9e, chunked ASCII line protocol ending with \n, PING/PONG/SEND/LEARN commands, Learn screen with code capture and test/save).

Key requirements:
- Permissions: TRANSMIT_IR, BLUETOOTH_SCAN (neverForLocation), BLUETOOTH_CONNECT, legacy BLUETOOTH/ADMIN/LOCATION maxSdkVersion 30. Runtime permission flow.
- Shared Code Model: Remote buttons (Power, Speed 1-5, Boost, Timer, LED, Sleep). Raw/NEC json defaults in res/raw/ir_codes.json and DataStore storage. NEC encoder implementation (9000/4500 us header, 560 us mark, 560/1690 us spaces, LSB first address/~address/command/~command, stop mark). FanTransport interface with PhoneIrTransport, FanBleTransport, Esp32Transport implementations. Repeat count setting. DataStore selected mode persistence.
- Code Editor screen: edit address, command, frequency, raw timings per button + test button.
- Code Finder screen: scan NEC address/command range with configurable interval + auto-detect response if BLE connected within 400ms.
- Main Screen: Mode segmented control, remote button grid, transport status bar, verification state indicator (fan responded/no response), dark/light theme, large touch targets, haptic feedback.
- Architecture & Quality: MVVM with ViewModel and StateFlow, separate classes (IrController, FanBleManager, Esp32BleManager, CodeRepository), unit tests for NEC encoder & ESP32 parser, README file.

## Project Brief

# AtomFan Remote - Project Brief

## Features
- **Multi-Transport Remote Interface**: Unified control dashboard (Power, Speed 1–5, Boost, Timer, LED, Sleep) supporting three transport modes: Phone IR Blaster (`ConsumerIrManager`), Direct Fan BLE connection, and ESP32 BLE Bridge.
- **Live Status & Visual Feedback**: Real-time transport status bar, verification state indicator (fan response detection), haptic feedback, and theme-adaptive layout.
- **Code Editor & Data Store**: Dedicated editor for fine-tuning address, command, frequency, and raw timings per button with instant testing and persistent storage.
- **Code Finder Engine**: Automated scanner to step through NEC address/command spaces with response auto-detection when connected over BLE.

## High-Level Tech Stack
- **Language & Core Framework**: Kotlin, Jetpack Compose, Material Design 3
- **Architecture**: MVVM using `ViewModel` and Kotlin `StateFlow` / `Coroutines`
- **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven navigation) and Compose Material Adaptive
- **Hardware & Interop**: Android Bluetooth LE APIs (`BluetoothGatt`, GATT characteristics), `ConsumerIrManager` (custom NEC signal encoder)
- **Data Persistence**: Jetpack DataStore (storing remote button configurations and active transport mode)

## Implementation Steps

### Task_1_SetupDataModelsAndTransports: Set up core data models, default IR codes (ir_codes.json), DataStore repository for settings and IR codes, FanTransport interface, and transport implementations (PhoneIrTransport with NEC encoder, FanBleTransport with GATT/dashboard, Esp32Transport with Nordic UART ASCII parser). Include unit tests for NEC encoder and ESP32 line parser.
- **Status:** COMPLETED
- **Updates:** Data models, ir_codes.json, DataStore repository, FanTransport implementations (PhoneIrTransport, FanBleTransport, Esp32Transport), NEC encoder, ESP32 line parser, and unit tests implemented and verified via unit tests and assembleDebug.
- **Acceptance Criteria:**
  - DataStore and default ir_codes.json are implemented
  - FanTransport interface and PhoneIrTransport, FanBleTransport, Esp32Transport implementations created
  - Unit tests for NEC encoder and ESP32 line parser pass
  - build pass

### Task_2_PermissionsAndMainRemoteUI: Implement runtime permission request flow (TRANSMIT_IR, BLUETOOTH_SCAN, BLUETOOTH_CONNECT) and Jetpack Compose Main Remote screen featuring mode selector (PHONE IR, FAN BLE, ESP32 BRIDGE), remote control button grid (Power, Speed 1-5, Boost, Timer, LED, Sleep), transport status bar, fan response verification indicator, and haptic feedback.
- **Status:** COMPLETED
- **Updates:** Runtime permissions flow, MainViewModel, MainScreen remote UI with mode selector, button grid, transport status bar, verification indicator, haptic feedback, and unit tests implemented and verified via unit tests and assembleDebug.
- **Acceptance Criteria:**
  - Runtime permission flow implemented for IR and Bluetooth
  - Main remote screen displays mode selector, transport status bar, and remote button grid
  - Button actions invoke selected transport with haptic feedback
  - build pass

### Task_3_BleDashboardAndEsp32LearnScreen: Implement Fan BLE live dashboard screen (raw hex log view + export, experimental BLE control write, GATT status handling, bond handling, auto-reconnect) and ESP32 IR Learn screen (IR signal capture, display received code, signal test, save to DataStore).
- **Status:** COMPLETED
- **Updates:** Fan BLE Live Dashboard screen and ESP32 IR Learn screen implemented and verified successfully via build and unit tests.
- **Acceptance Criteria:**
  - Fan BLE dashboard displays live status, raw hex log, and export capability
  - ESP32 Learn screen captures, tests, and saves IR codes
  - build pass

### Task_4_CodeEditorAndCodeFinderScreen: Implement Code Editor screen for customizing address, command, frequency, and raw timings per button with instant testing, Code Finder engine for automated NEC address/command scanner with BLE response auto-detection, and project README.md document.
- **Status:** COMPLETED
- **Updates:** Code Editor screen, Code Finder screen with BLE response auto-detection, and comprehensive README.md implemented and verified successfully.
- **Acceptance Criteria:**
  - Code Editor allows customizing and testing button IR codes
  - Code Finder engine sweeps NEC space and detects fan response
  - README.md created with build and usage documentation
  - build pass

### Task_5_RunAndVerify: Run and verify application stability, ensure all tests pass, build succeeds, and instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** All unit tests passed successfully (20 unit tests including NEC encoder, ESP32 line parser, and ViewModels), debug APK built successfully, and codebase fully meets all functional requirements.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verified application stability, alignment with user requirements, and reported UI issues
- **Duration:** N/A

