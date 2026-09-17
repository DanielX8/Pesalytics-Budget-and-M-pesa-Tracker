<a name="top"></a>
[![Google Play](https://img.shields.io/badge/Google_Play-Live_on_Store-4285F4?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.pesalytics)
[![Android](https://img.shields.io/badge/Android-7.0+-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Privacy](https://img.shields.io/badge/Privacy-100%25_On--Device-00C853)](#)
[![Status](https://img.shields.io/badge/Status-Production_Release-brightgreen)](#)
[![License](https://img.shields.io/badge/license-Proprietary-red)](LICENSE)

## Pesalytics — Intelligent, Private M-PESA Financial Tracker

Turn your raw M-PESA and Pochi la Biashara SMS messages into a clean, searchable, and highly visual financial dashboard. **100% on-device, zero cloud servers, zero telemetry leaks.**

[![Share](https://img.shields.io/badge/share-000000?logo=x&logoColor=white)](https://x.com/intent/tweet?text=Check%20out%20Pesalytics%20-%20Privacy-first%20M-PESA%20expense%20tracker!%20%23Android%20%23PrivacyFirst%20%23MPESA)
[![Share](https://img.shields.io/badge/share-0A66C2?logo=linkedin&logoColor=white)](https://www.linkedin.com/sharing/share-offsite/?url=https://github.com/DanielX8/Pesalytics-Budget-and-M-pesa-Tracker)

<div align="center">
  <img src="banner.png" alt="Pesalytics Banner" width="100%" />
</div>

<br/>

<div align="center">
  <h3>🏛 System Architecture & Reactive Data Flow Map</h3>
  <a href="https://htmlpreview.github.io/?https://github.com/DanielX8/Pesalytics-Budget-and-M-pesa-Tracker/blob/master/docs/index.html">
    <img src="docs/architecture/pesalytics-architecture.svg" alt="Pesalytics Architecture Map" width="100%" />
  </a>
  <p>
    <a href="https://htmlpreview.github.io/?https://github.com/DanielX8/Pesalytics-Budget-and-M-pesa-Tracker/blob/master/docs/index.html">
      <img src="https://img.shields.io/badge/%F0%9F%8E%AE_Explore-Live_Interactive_Map-00C853?style=for-the-badge&logoColor=white" alt="Open Interactive Map" />
    </a>
  </p>
  <p><sub><em>100% On-Device Privacy Boundary • Room SQLite v14 • Reactive StateFlow Architecture • Click above for live interactive traces</em></sub></p>
</div>

---

## 🚀 Get Pesalytics

<div align="center">
  <a href="https://play.google.com/store/apps/details?id=com.pesalytics">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80" />
  </a>
</div>

### ⬇️ Direct Sideload / GitHub Releases
If you prefer direct APK installation without the Google Play Store:
1. Go to the [**Releases page**](https://github.com/DanielX8/Pesalytics-Budget-and-M-pesa-Tracker/releases).
2. Download the latest `.apk` file (e.g. `pesalytics-release-v1.6.3.apk`).
3. Open the file on your Android device and confirm installation.

---

## Overview

Your phone already receives an SMS for every M-PESA transaction you make. Pesalytics securely reads those messages to build a comprehensive, zero-setup expense tracker and financial statement engine. **No account creation, no password logins, and no banking data uploaded to the cloud.**

### How it works
1. **Grant Read-Only SMS Permission** — All parsing happens in-memory and in local SQLite; nothing is uploaded anywhere.
2. **Instant Transaction Parsing** — Pesalytics extracts transaction types, payees, amounts, balances, and hidden fees in real time.
3. **Smart Insights & Financial Statements** — View categorized monthly budgets, track bill schedules, and export formal PDF/CSV financial statements.

---

## Why Pesalytics?

### 🔑 Key Differentiators
- **🔒 100% On-Device & Private** — All SMS parsing, machine learning categorization, and financial aggregations run locally on your phone.
- **🇰🇪 Purpose-Built for M-PESA & Pochi** — Deep native support for Send Money, Paybill, Buy Goods Till, Pochi la Biashara, Fuliza loans, and M-Shwari savings.
- **💼 Dual-Wallet Account Scopes** — Seamlessly filter between Personal transactions and Business Pochi transactions.
- **⚡ Instant Sync with Categorized Alerts** — New transactions are automatically grouped by category (Groceries, Utilities, Transport) with direct deep-links from system notifications.
- **📄 Pro Financial Statements** — Generate formatted PDF statements with charts, CSV spreadsheets for Excel/Sheets, or complete JSON encrypted backups.

---

### 🌟 Core Capabilities
- **🤖 Offline Merchant Categorization** — Built-in offline dictionary mapping 130+ Kenyan merchants to 16 financial categories.
- **📊 Visual Spending Analytics** — Real-time spend velocity meters, interactive donut breakdowns, and daily transaction timelines.
- **💰 Category Budget Planner** — Set monthly spending caps per category with proactive overspend warnings.
- **📅 Recurring Bills & Pausing** — Track recurring utility/rent due dates with one-tap pausing and automatic cycle roll-over.
- **🎯 Financial Savings Goals** — Visual milestone trackers to help you save up for specific targets.
- **💳 M-PESA Fee Tracking** — Automatically isolate and calculate hidden Safaricom transaction fees.

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material Design 3 with Dynamic Theme Reveal)
- **Architecture**: MVVM + Clean Architecture + Unidirectional Data Flow (UDF)
- **Reactive Streams**: Kotlin Coroutines & `StateFlow`
- **Local Persistence**: Android Room Database (SQLite schema v14, 6 DAOs)
- **Background Automation**: Android WorkManager (Daily Spend & Weekly Summary workers)
- **Monetization**: Google Play Billing KTX (`billing-ktx:7.0.0`) with 30-day Free Trial support
- **Design Tokens & Fonts**: Bundled Poppins Typography & Custom Dark Mode Palette

---

## 📱 Developer Setup & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/DanielX8/Pesalytics-Budget-and-M-pesa-Tracker.git
   cd Pesalytics-Budget-and-M-pesa-Tracker
   ```

2. **Open in Android Studio:**
   - Open Android Studio Ladybug / Meerkat or newer.
   - Select `File > Open` and select the repository root.

3. **Build and Run:**
   ```bash
   ./gradlew assembleDebug
   ```

---

## ⚖️ License

Copyright © 2026 Daniel Odhiambo. All Rights Reserved.  
Source code is published for transparency, security review, and educational reference under the [Proprietary License](LICENSE).
