<h1 align="center">Smart Island · Galaxy Z Fold8 Edition</h1>

<p align="center"><strong>English</strong> · <a href="README.zh-CN.md">简体中文</a></p>

<p align="center">
  <strong>Smart Island Fold8</strong> — a Galaxy Z Fold8 tuned edition of <a href="https://github.com/agupta07505/SmartIsland">Smart Island</a>, with liquid droplet motion and iOS-style interaction, fitted to the Fold8 cover screen.
</p>

<p align="center">
  <strong>Current release: v7.0.0-fold8.1</strong> · based on Smart Island v7.0.0 · GPL-3.0
</p>

<p align="center">
  <img src="docs/media/droplet-1x.gif" width="560" alt="Droplet motion at normal speed" /><br/>
  <sub>Normal speed</sub>
</p>
<p align="center">
  <img src="docs/media/droplet-3x.gif" width="560" alt="Droplet motion, 3× slow motion" /><br/>
  <sub>3× slow motion: swell → flow open → companion slides in · squeeze → pop → thud</sub>
</p>

## Droplet Motion

The whole motion is designed as "a drop of water pulled apart, then gathering back into one": opening feels viscous, closing has weight, and resting shapes hold surface tension.

- **Expand: swell, then flow open.** The island first swells by 6%, as if pressure built up inside the drop; then it spreads sideways — width first, height half a beat later, the top edge last — overshooting slightly before surface tension pulls it back. Content fades in from a blur only once the shape has mostly opened.
- **Collapse: edges flow back, then a "thud".** Content fades first, the outline gathers toward the camera, faster as it gets closer, with the outer edges trailing slightly. As it reforms, the drop sinks a little, spreads wide, squashes flat, rebounds once and settles — paired with a short, low haptic tap.
- **Two drops making way and squeezing out.** When the main notification expands, the secondary activity is drawn into the camera position 100ms later, travelling along a shallow arc with its leading side stretched, briefly connected to the main card by a thin neck that then snaps. When the main notification returns, it squashes the secondary first; the neck between them thins until it pops, and ~50ms later the main island lands with a thud.
- **Only the outline is liquid.** Icons and text stay sharp, never stretched, and two sets of content never overlap. Liquid merging only happens while shapes move close to each other; at rest the edges are crisp again.

Under the hood, every black outline is drawn on one layer; while moving, that layer gets a Gaussian blur plus an alpha threshold so nearby shapes merge naturally (metaballs). The choreography is driven by a single timeline (`IslandMotion.kt`). The GIFs above are rendered frame by frame from [`docs/index.html`](docs/index.html) using the same timeline — open it in a browser to play with it.

## What Is This

A fork of Animesh Gupta's open-source [Smart Island](https://github.com/agupta07505/SmartIsland), tuned for the **Samsung Galaxy Z Fold8 cover screen**. All original features are kept; the changes focus on the island's shape, motion and interaction.

### What's Tuned

**Liquid droplet feel**
- Expanding feels like a drop being pulled open: a gentle swell, then flowing out sideways with a small overshoot pulled back by "surface tension"; content emerges from a blur once the shape settles.
- Collapsing feels like water flowing back into one drop: edges retreat, accelerating, then land with a "thud" (slight sink, squash, one rebound, low haptic).
- Activities interact like liquid: the secondary activity is drawn into the camera slot when the main one expands, with a brief neck between them; when the main one returns it pops the secondary back out to the side.
- Only the black outlines merge and deform; icons and text always stay sharp.

**Fitted to the Fold8 cover screen**
- The island is anchored to the cover-screen camera cutout; content never enters the hole.
- The cover screen has near-square corners, so the expanded card uses a 20dp radius with 14dp side margins and sits below the status bar, clear of the clock and signal icons.
- The collapsed capsule follows Apple's HIG height of 36.67dp, with continuous-curvature ends; round icons sit concentric with the ends.

**iOS-style notification interaction**
- Pure black notification card: app name and time on top, title plus up to two lines of body text, dark translucent action buttons.
- Swipe left/right to clear the current notification, swipe up to tuck it back into the island (not deleted), swipe down or tap to open the app; new notifications stay expanded for 15 seconds.

**Steadier rendering**
- The drawing window is full-screen and never resized; touches are forwarded through a separate transparent window, eliminating flicker and jumps at the end of expand/collapse.
- Island animations always play at 1× speed, regardless of the system animation scale setting.

See [CHANGELOG.md](CHANGELOG.md) for the full list of changes.

### Installation Notes

- Application ID is `com.senbuilds.smartisland.fold8`, so it can be installed alongside the original Smart Island.
- On first launch, grant Notification Access and enable the Accessibility Service manually (same as the original).
- In-app "Check for updates" points to this repository's releases.

### Credits & License

- Original project: [Smart Island](https://github.com/agupta07505/SmartIsland) © Animesh Gupta, GPL-3.0.
- This edition was modified by Sen Hu ([@senbuilds](https://github.com/senbuilds)) in October 2026 and is likewise released under the [GNU GPL v3.0](LICENSE). The original author's copyright notices are kept in every source file; files added in this edition carry the edition's copyright header.
- Please report issues to this repository's [Issues](https://github.com/senbuilds/SmartIsland/issues), not to the original project.

---

<p align="center"><em>Original Smart Island README below</em></p>
<p align="center">
  A lightweight, privacy-first Android overlay that turns notifications, calls, media playback, battery states, timers, stopwatches, and system activities into a floating glanceable Dynamic Island.
</p>

<p align="center">
  <strong>Current release: v7.0.0</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8%2B-3DDC84?logo=android&logoColor=white" alt="Android 8+" />
  <img src="https://img.shields.io/badge/Kotlin-JVM%2017-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin JVM 17" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="GNU GPLv3" />
  <img src="https://img.shields.io/github/downloads/agupta07505/SmartIsland/total?color=2ea44f&logo=github" alt="Total Downloads" />
  <a href="https://www.virustotal.com/gui/file/c07da408e7fa3e3fdeb25ec6415af07f1d56eff099840221138e9364c6065102/details" target="_blank">
    <img src="https://img.shields.io/badge/VirusTotal-Scan-007ACC" alt="VirusTotal Scan" />
  </a>
</p>

<p align="center">
  <a href="#downloads--safety">Downloads</a> |
  <a href="#screenshots">Screenshots</a> |
  <a href="#features">Features</a> |
  <a href="#gesture-guide">Gesture Guide</a> |
  <a href="#getting-started">Getting Started</a> |
  <a href="#privacy-and-permissions">Privacy</a> |
  <a href="ROADMAP.md">Roadmap</a> |
  <a href="CHANGELOG.md">Changelog</a> |
  <a href="https://telegram.me/SmartIslandApp">Telegram Community</a>
</p>

---

## Overview

Smart Island is an open-source Android application that renders a fluid, dynamic floating pill near your device's camera notch or status bar. It intercepts system notifications, live activities, media playback, calls, charging events, hotspot connections, navigation instructions, timers, stopwatches, and battery alerts locally—expanding into rich interactive cards with zero cloud tracking.

The project is designed to be 100% transparent, hackable, and privacy-conscious: all notification processing and history storage are strictly on-device, preferences are stored in local DataStore, and network access is solely opt-in for in-app release checks.

---

## Downloads & Safety

* **Download APK**: Obtain the pre-compiled APK directly from the [GitHub Releases (v7.0.0)](https://github.com/agupta07505/SmartIsland/releases/latest) page.
* **Total Downloads**: ![Total Downloads](https://img.shields.io/github/downloads/agupta07505/SmartIsland/total?color=2ea44f&logo=github)
* **Telegram Channel**: Join our active community at [telegram.me/SmartIslandApp](https://telegram.me/SmartIslandApp) to suggest features, get support, and discuss updates.
* **Security Verification**: To ensure complete safety, inspect packages via [VirusTotal](https://www.virustotal.com/gui/file/c07da408e7fa3e3fdeb25ec6415af07f1d56eff099840221138e9364c6065102/details) or review automatic GitHub Actions CI builds.

---

## Screenshots

<p align="center">
  <img src="assets/screenshots/01_home_try_it_out.jpg" width="24%" alt="Home Screen & Try It Out" />
  <img src="assets/screenshots/02_layout_appearance_controls.jpg" width="24%" alt="Layout & Appearance Controls" />
  <img src="assets/screenshots/03_notifications_privacy_rules.jpg" width="24%" alt="Notifications & OEM Privacy Rules" />
  <img src="assets/screenshots/04_live_activities_app_alerts.jpg" width="24%" alt="Live Activities & App Alerts Manager" />
</p>

<p align="center">
  <img src="assets/screenshots/05_app_shortcuts_launcher.jpg" width="24%" alt="App Shortcuts Launcher" />
  <img src="assets/screenshots/06_notification_history_log.jpg" width="24%" alt="Virtualized Notification History Log" />
  <img src="assets/screenshots/07_appearance_colors_studio.jpg" width="24%" alt="Appearance & Colors Studio" />
  <img src="assets/screenshots/08_gesture_guide_playground.jpg" width="24%" alt="Gesture Guide & In-Pill Playground" />
</p>

<p align="center">
  <img src="assets/screenshots/09_permissions_setup_center.jpg" width="24%" alt="Permissions Setup Center" />
  <img src="assets/screenshots/10_backup_and_restore.jpg" width="24%" alt="Settings Backup & Restore" />
  <img src="assets/screenshots/11_app_updates_and_downloads.jpg" width="24%" alt="App Updates & Downloads Hub" />
  <img src="assets/screenshots/12_expanded_navigation_and_about.jpg" width="24%" alt="Expanded Navigation & Community" />
</p>

<p align="center">
  <img src="assets/screenshots/13_developer_options_diagnostics.jpg" width="24%" alt="Developer Options & System Diagnostics" />
</p>

---

## Features

| Feature Area | Capabilities & Details |
| --- | --- |
| **Deep Burnt-Orange (#D84315) Design System** | Harmonious curated color palette with rich burnt-orange accents, deep slate/black card surfaces, crisp typography, and unified tinted Material icons throughout all settings cards and switches. |
| **App Updates & Downloads Hub** | Dedicated section showcasing Total Downloads counter (15,648+), Current Version line (v7.0.0), 1-tap GitHub update checker, in-app changelogs viewer, Top Contributors gallery, and Recent Commits feed. |
| **In-Pill Swipe Gestures & Media Controls** | Swiping horizontally across the compact pill skips tracks (`skipToNext`/`skipToPrevious`) or cycles notifications without expanding the island. Features master toggle, swipe direction pickers, and individual disable controls. |
| **Selective App Alerts & Sound Manager** | Fast selective app configuration modal with search bar, per-app alert toggles, custom ringtone sound toggling, and island exclusion without heavy upfront package loading. |
| **Configurable Companion Circle Position** | Place the multi-tasking companion circle on either the **Left** or **Right** of the pill with anti-overlap geometry clamping for corner and punch-hole displays. |
| **Material 3 Expressive UI** | Modern MD3 color tokens, dynamic wallpaper color adaptation (Android 12+), rounded shapes, and fluid physics. |
| **Ultra-Fluid Spring Physics** | iOS-grade synchronized spring curves (`stiffness = 520f`, `dampingRatio = 0.72f`), snappy 190ms cross-fades, organic companion bubble pops, and tactile button bounce. |
| **Tap Anywhere to Open App** | Tapping anywhere on expanded notification and music cards outside interactive buttons launches the source app and collapses the island. |
| **Auto-Hide Pill Inactivity Timer** | Automatically hides the pill and companion bubble after a customizable inactivity timeout (1s to 60s with 1-tap presets), with tap-to-awaken gesture. |
| **Landscape Mode Control** | User toggle to keep Smart Island visible in landscape orientation with automated screen width repositioning, or auto-hide for distraction-free gaming and video. |
| **Bluetooth Battery & Earbuds Animation** | Dual-path battery level extraction via broadcast extra and reflection, with a smooth 3-second spring-animated transition between live battery gauge and earbuds icon. |
| **Notification Cooldown & Anti-Spam** | Intelligent burst detection (e.g. 3 alerts in 30s) that temporarily cools down noisy apps, holding rapid spam and surfacing the latest message after a customizable quiet period (1–30 min), with per-app whitelist exclusion. |
| **Island Opacity & Transparency** | Continuous background opacity slider (20% to 100%) with 4 quick 1-tap presets (Solid, Dark, Glass, Clear) and ambient drop shadow rendering. |
| **Inline Reply & IME Focus** | Direct text reply input right inside the expanded Island notification card with dynamic WindowManager focus switching, soft keyboard integration, and `RemoteInput` dispatch. |
| **Timer & Stopwatch Modes** | Intelligent clock notification parsing for Google Clock, Samsung Clock, MIUI/HyperOS, ColorOS, and Huawei Clock. Collapsed countdown/elapsed badges, expanded circular/linear timer progress, live millisecond stopwatch ticker, and interactive pause/resume/lap/reset controls. |
| **Virtualized Notification History** | Ultra-smooth `LazyColumn` virtualized SQLite history log with background `AppIconMemoryCache`, search bar, app filter chips, full message inspection dialog, and bulk **Delete by App** support. |
| **OEM Device Rules Engine** | Tailored background protection and autostart management for Xiaomi/HyperOS, Samsung OneUI, OnePlus/Oppo/Realme (ColorOS/OxygenOS), Huawei/Honor, Vivo/iQOO, and Asus to prevent aggressive background kills. |
| **Full-Color App Icons** | Extracted launcher icons with LRU caching for crisp, authentic app icons in both collapsed pill and expanded cards. |
| **Battery Modes (Low & Saver)** | Dynamic Battery island displaying Green for Charging, Pulsing Red for Low Battery (&le; 20%), and Warm Amber for Battery Saver ON with live battery percentage badge. |
| **Comprehensive Gesture Engine** | Dual-tier gesture system: In-Pill Swipes (Left/Right/Up/Down for media/notifications) and Expanded Card Gestures (Tap, Dismiss, 300ms Hold + Swipe Clear All, Floating Window, Notification Shade). |
| **Split Island Multi-State** | Secondary auxiliary bubble for concurrent background activities (e.g. Music + Hotspot, Call + Bluetooth, Timer + Music). |
| **Wavy Music Player** | Real-time audio waveform scrubber, ambient album artwork background glow, track metadata, and dynamic color customization. |
| **Message Sync Suppression** | Automatically filters background message polling notifications (e.g. Snapchat, WhatsApp sync) to prevent download mode hijacking. |
| **Live Activity Tracking** | Real-time delivery and rideshare tracking (Uber, Swiggy, Zomato, Blinkit, Rapido, Ola) with brand colors. |
| **Turn-by-Turn Navigation** | Live turn navigation indicators, maneuver direction arrows, remaining distance, and ETA. |
| **Download & Upload Progress** | Real-time transfer speed meters (MB/s), progress bars, and animated icons. |
| **Wi-Fi Hotspot Monitor** | Live tethering client counter, SSID badge, data usage, and quick turn-off action. |
| **Flashlight & Screen Recording** | Active torch toggle card and live screen recording timer overlay. |
| **App Shortcuts Launcher** | Quick-launch grid with up to 8 pinned apps or auto-filled recent applications, with complete user enable/disable control. |
| **Custom RGB Color Studio** | Fine-grained Red, Green, Blue slider color picker with live Hex preview for all 13 dynamic modes. |
| **Notch & Layout Presets** | Instant 1-tap calibration for Center Hole, Wide Island, Left Corner, Right Corner, and Compact Pill. |
| **Precision Sizing Controls** | Millimeter-accurate sliders for Width, Height, X Offset, Y Offset, Corner Radius, and Drop Shadows. |
| **Shizuku 1-Tap Auto Setup** | Automated permission grants for Restricted Settings, Usage Access, Overlay, and Battery Optimization. |
| **Backup & Restore System** | Export and restore complete island notch coordinates, dimensions, custom color themes, app shortcuts, and rules to/from JSON via Android Storage Access Framework with bounds validation. |
| **iPhone Notch Mode** | Top-edge docked notch styling, rounded bottom corners, and complete elimination of the secondary right companion circle for an authentic iPhone notch experience. |
| **Developer Mode & Log Recording** | 7-tap version unlock, in-memory diagnostic buffer, process logcat extraction, live status badges, and 1-tap `.txt` report export via Storage Access Framework. |
| **Lock Screen Privacy Guard** | Opt-in lock screen display with customizable sensitive content hiding (App Icon Only vs Full Preview). |

---

## 13 Dynamic Island Modes

Smart Island intelligently categorizes and presents live activities into 13 dedicated modes:

1. **Notification**: Standard incoming app alerts with full-color launcher icons, action buttons, and **Inline Reply**.
2. **Timer**: Live countdown timer with circular/linear progress indicator and pause/resume/stop controls.
3. **Stopwatch**: Live elapsed millisecond ticker with lap tracking and pause/reset controls.
4. **Music**: Media playback with interactive wavy seek bar, ambient album art glow, and repeat/like toggles.
5. **IncomingCall**: Active call timer, caller badge, and waveform animations.
6. **Battery**: Charging speed & time-until-full, low battery pulsing alerts (&le; 20%), and battery saver mode.
7. **LiveActivity**: Delivery and ride-sharing updates (Uber, Zomato, Swiggy, Blinkit, Rapido, Ola) with brand accents.
8. **Navigation**: Turn-by-turn routing indicators, maneuver direction arrows, remaining distance, and ETA.
9. **DownloadUpload**: Real-time download and upload progress bars with MB/s transfer speed meters.
10. **Hotspot**: Tethering status, connected device client counter, and 1-tap toggle.
11. **Bluetooth**: Connected device battery percentage, dual-path battery level extraction, and smooth alternating spring animation between battery gauge and earbuds icon.
12. **Flashlight**: Active torch status indicator with 1-tap shutoff.
13. **ScreenRecording**: Live recording elapsed duration timer.

---

## Gesture Guide

Smart Island provides dual-tier gesture control covering both the compact collapsed pill and the expanded card:

### Collapsed Pill Gestures
| Gesture on Pill | Default Action | Available Custom Actions |
|---|---|---|
| **Swipe Left** | **Previous Notification** | Next Notification, Previous Track (`skipToPrevious`), Next Track (`skipToNext`), Play/Pause, Dismiss Current, Open App, Floating Window, Notification Shade, **Disabled** |
| **Swipe Right** | **Next Notification** | Previous Notification, Next Track (`skipToNext`), Previous Track (`skipToPrevious`), Play/Pause, Dismiss Current, Open App, Floating Window, Notification Shade, **Disabled** |
| **Swipe Up** | **Dismiss Current** | Dismiss All Notifications, Open Notification Shade, **Disabled** |
| **Swipe Down** | **Expand Island** | Open Notification Shade, Open App, Floating Window, Dismiss Current, **Disabled** |
| **Single Tap** | **Expand Island** | Opens full interactive card (or awakens pill when idle-hidden) |

### Expanded Card Gestures
| Gesture on Card | Action | Description & Feedback |
|---|---|---|
| **Single Tap** | **Open Source App** | Tapping neutral card areas opens the underlying application; tapping outside collapses the card |
| **Quick Swipe Up** | **Dismiss Current** | Flick upward (&ge; 48dp) to dismiss active notification from stack |
| **Hold + Swipe Up** | **Clear ALL Notifications** | Press & hold for **300ms** until **haptic vibration pulse**, then swipe up to dismiss all pending notifications |
| **Swipe Down** | **Floating Window / Shade** | Drag downward (&ge; 48dp) to launch app in freeform floating window or open notification shade |
| **Swipe Left / Right** | **Switch Stack / Tracks** | Navigate smoothly between notifications, timers, and active media sessions |

---

## Architecture

```mermaid
flowchart TD
    subgraph System["Android OS & System Services"]
        A[Status Bar Notifications] --> B[SmartIslandNotificationListenerService]
        Sys[System Broadcasts: Power, Battery, BT, Torch] --> SER[SystemEventReceiver]
    end

    subgraph Parsers["Parsers & Rules Engine"]
        B -->|Filter & Suppress Sync| NF[NotificationFilter]
        B -->|Parse Timers & Stopwatches| TSP[TimerStopwatchParser]
        B -->|Parse Turn-by-Turn Navigation| NP[NavigationParser]
        B -->|Parse Tethering Clients| HU[HotspotUtil]
        OEM[OEM Vendor Detection] --> ODR[OemDeviceRules / Autostart]
    end

    subgraph Repositories["Data Repositories & State"]
        NF --> NR[SmartIslandNotificationRepository]
        TSP --> NR
        NP --> NR
        HU --> NR
        SER --> NR
        ODR --> NR
        B -->|Persist Alerts| NHD[NotificationHistoryRepository / SQLite DB]
        DS[(AndroidX DataStore Settings)] --> SR[SmartIslandSettingsRepository]
        GH[GitHub Releases API] -->|User Opt-In| GHS[GitHubApiService]
    end

    subgraph Presentation["UI & Overlay Presentation"]
        NR --> VM[IslandViewModel]
        SR --> VM
        VM --> OS[SmartIslandOverlayService]
        OS --> WM[WindowManager Overlay]
        WM -->|Compose State| OV[IslandOverlayView / IslandExpandedContent]
        OV -->|Inline Reply Focus Switch| OS
        OV -->|5-Gesture Engine| VM
    end
```

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| **Language** | Kotlin 2.0+ (JVM Target 17) |
| **UI Framework** | Jetpack Compose & Material 3 Expressive Design System |
| **Architecture** | Clean Architecture, MVI/Repository Pattern |
| **Dependency Injection** | Dagger Hilt |
| **State & Async** | Kotlin Coroutines, StateFlow, Atomic Mutex |
| **Local Persistence** | AndroidX DataStore Preferences & SQLite (`NotificationHistoryDbHelper`) |
| **System Services** | NotificationListenerService, Accessibility Floating Service, WindowManager Overlays, Shizuku |
| **Network & Updates** | In-app GitHub Release API via `GitHubApiService` (strictly opt-in via `allowNetworkChecks`) |
| **Build & CI** | Gradle Wrapper, AGP 9.0+, GitHub Actions CI (@v4) |

---

## Requirements

- **Android Version**: Android 8.0+ (API 26 through API 36)
- **JDK**: Java Development Kit 17
- **Permissions**: Overlay (`SYSTEM_ALERT_WINDOW`) and Notification Listener access

---

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/agupta07505/SmartIsland.git
cd SmartIsland
```

### 2. Build the debug APK
**On Windows:**
```powershell
.\gradlew.bat assembleDebug
```

**On macOS / Linux:**
```bash
./gradlew assembleDebug
```

### 3. Install on a connected device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.agupta07505.smartisland/.MainActivity
```

---

## Privacy And Permissions

Smart Island is strictly privacy-first:

| Permission | Purpose |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Draws the floating dynamic island overlay above other applications. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Intercepts notification metadata to display alerts, media playback, timers, and system states. |
| `FOREGROUND_SERVICE` & `SPECIAL_USE` | Keeps the overlay service active in the background without being killed by the OS. |
| `ACCESS_RESTRICTED_SETTINGS` (Optional Shizuku) | Grants 1-tap automated permissions without manual menu navigation. |
| `INTERNET` (Opt-in) | Used **strictly** for user-opted GitHub release update checks (`allowNetworkChecks`). **Zero** notification data, metadata, or telemetry is ever transmitted over the network. |

*Note: All notification processing and notification history are stored 100% locally on-device. See [PRIVACY.md](PRIVACY.md).*

---

## Contributing

Contributions are warmly welcomed! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests, and use our issue templates for bug reports or feature suggestions.

---

## License

Smart Island is licensed under the [GNU General Public License v3.0](LICENSE).  
Copyright (C) 2026 **Animesh Gupta**.

