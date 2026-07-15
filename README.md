# 🚗 CarDebug - OBD2 Android Diagnostic App

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **A native Android application for real-time vehicle diagnostics using OBD2 protocol**

---

## 📱 Overview

**CarDebug** is a professional Android application that connects to your vehicle's ECU (Engine Control Unit) via an **ELM327** device over **Bluetooth**. It provides real-time diagnostics, sensor monitoring, and fault code analysis.

---

## ✨ Key Features

- 🔌 **Bluetooth connection** to ELM327 OBD2 adapter
- 📊 **Real-time sensor data** (speed, RPM, coolant temp, battery voltage, etc.)
- ⚠️ **DTC reading & analysis** with detailed descriptions
- 📈 **Live data charts** for trend monitoring
- 💾 **History logging** with Room database
- 🚦 **Support for multiple OBD2 protocols** (ISO 9141-2, KWP2000, CAN, etc.)
- 🎨 **Modern Material Design UI**

---

## 🛠️ Tech Stack

- **Kotlin** - Primary programming language
- **Android SDK** - Native Android development
- **Room Database** - Local data persistence
- **Coroutines & Flow** - Asynchronous processing
- **Bluetooth API** - ELM327 connection
- **OBD2 Protocol** - ECU communication
- **MPAndroidChart** - Professional charts
- **Material Design** - Modern UI

---

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Iguana or later
- Android SDK API 24+
- ELM327 OBD2 adapter (for physical testing)

### Steps
```bash
# 1. Clone the repository
git clone https://github.com/aref8985/CarDebug-Android.git

# 2. Open in Android Studio
cd CarDebug-Android

# 3. Build and run on device or emulator

CarDebug-Android/
├── app/
│   ├── src/main/java/com/example/cardebug/
│   │   ├── data/           # Data layer (Room, Repository)
│   │   ├── domain/         # Domain layer (UseCases, Entities)
│   │   ├── presentation/   # Presentation layer (ViewModel, UI)
│   │   ├── bluetooth/      # Bluetooth connection management
│   │   └── obd2/           # OBD2 protocol processing
│   └── src/main/res/       # UI resources
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
