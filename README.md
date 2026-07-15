# 🚗 CarDebug - OBD2 Android Diagnostic App

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)
![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

> **Android application for real-time vehicle diagnostics via OBD2 & ELM327**

---

## 📱 Overview

**CarDebug** connects to your vehicle's ECU via **ELM327** over **Bluetooth** to provide real-time diagnostics, sensor monitoring, and fault code analysis.

---

## ✨ Features

- 🔌 Bluetooth connection to ELM327
- 📊 Real-time sensor data (Speed, RPM, Temp, Voltage, etc.)
- ⚠️ DTC reading & analysis with descriptions
- 📈 Live data charts for trend monitoring
- 💾 History logging with Room Database
- 🎨 Modern Material Design UI

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary language |
| **Android SDK** | Native development |
| **Room DB** | Local data persistence |
| **Coroutines** | Async processing |
| **Bluetooth API** | ELM327 connection |
| **OBD2 Protocol** | ECU communication |
| **MPAndroidChart** | Data charts |
| **Material Design** | Modern UI |

---

## 📁 Project Structure

| Directory | Description |
|-----------|-------------|
| `data/` | Room Database, DAOs, and Repository implementation |
| `domain/` | Business models and UseCases |
| `presentation/` | Activities, Fragments, and ViewModels |
| `bluetooth/` | Bluetooth connection and ELM327 management |
| `obd2/` | OBD2 protocol parsing and PID handling |
| `res/` | UI resources (layouts, drawables, strings, themes) |

👨‍💻 Developer
Aref Mostafavi

🎯 Senior Software Engineer | Full-Stack & Android Developer

📍 Tehran, Iran

💼 6+ years of experience in software development
