# 💰 Gestore Spese / Expense Manager

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-purple.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green.svg)
![AGP](https://img.shields.io/badge/AGP-9.2.1-orange.svg)
![API](https://img.shields.io/badge/minSdk-26-brightgreen.svg)

[🇮🇹 Italiano](#italiano) | [🇬🇧 English](#english)

---

## Italiano

**Gestore Spese** è una moderna applicazione Android nativa sviluppata con **Kotlin** e **Jetpack Compose**, progettata per offrire un controllo completo e flessibile sulle finanze personali. Include anche un'app **Wear OS** companion per inserimento rapido dal polso.

### ✨ Funzionalità Principali

#### 📊 Gestione Transazioni
*   **Spese ed Entrate**: Registra facilmente ogni movimento finanziario.
*   **Categorie Personalizzabili**: Crea e gestisci le tue categorie con icone, emoji e **immagini personali** (ritagliabili con crop integrato).
*   **Descrizioni Veloci**: Autocompletamento delle descrizioni basato sulle transazioni precedenti.
*   **Transazioni Ricorrenti**: Supporto per spese giornaliere, settimanali, mensili e annuali con limite configurabile.
*   **Cancellazione di Gruppo**: Elimina una transazione ricorrente e tutte le successive con un tap.

#### 💳 Supporto Avanzato Carte di Credito
Gestisci i pagamenti con carta di credito come un vero professionista:
*   **Calcolo Data Addebito**: Imposta il giorno di chiusura/addebito della carta o il ritardo in mesi. L'app calcolerà automaticamente quando i soldi usciranno effettivamente dal conto.
*   **Modalità Flessibili**: Supporto per **saldo unico**, **rateale** o gestione manuale.
*   **Transazioni Speculari**: Per ogni spesa su carta, viene creata automaticamente una transazione di "giroconto" per tracciare l'effettivo addebito futuro.

#### 📅 Pianificazione Rateale
*   **Suddivisione Spese**: Hai fatto un acquisto importante a rate? Inserisci l'importo totale e il numero di rate.
*   **Generazione Automatica**: L'app crea automaticamente le transazioni future per ogni mese, offrendo una proiezione chiara delle uscite future.
*   **Supporto Carta di Credito**: Le rate su carta vengono gestite con le relative transazioni speculari di addebito.

#### 🌍 Multi-Valuta e Tassi di Cambio
*   **Doppia Valuta**: Registra l'importo convertito nella tua valuta principale, mantenendo il riferimento all'originale.
*   **Tassi BCE**: L'app scarica automaticamente i tassi di cambio giornalieri dalla **Banca Centrale Europea** per conversioni precise. Possibilità di aggiornamento forzato manuale.
*   **Cache Locale**: I tassi vengono memorizzati in Room per accesso offline.

#### 🖼️ Immagini Categorie con Crop
*   **Selezione Gallery**: Scegli un'immagine dalla galleria del telefono.
*   **Ritaglio Integrato**: Ridimensiona e ritaglia l'immagine con l'interfaccia **uCrop** prima di salvarla (aspect ratio 1:1, max 512×512).
*   **Persistenza**: Le immagini vengono copiate nello storage interno dell'app e restano visibili anche dopo il riavvio.

#### ⌚ Wear OS Companion
*   **Inserimento Rapido**: Aggiungi spese veloci direttamente dal tuo orologio Wear OS.
*   **Invio Senza Rischi**: I dati vengono inviati al telefono tramite Google Play Services Wearable. Non richiede installazione separata.

#### 💾 Backup e Sicurezza
*   **Export JSON**: Backup completo di transazioni, categorie e carte di credito in formato JSON.
*   **Export CSV**: Esporta le tue transazioni in CSV (per Excel/Google Sheets) con colonne selezionabili.
*   **Restore Intelligente**: Ripristina i dati da backup JSON con normalizzazione automatica dei formati legacy.
*   **Auto Backup**: Backup automatico Android con regole personalizzate (`backup_rules.xml`).
*   **Privacy**: Proteggi l'accesso all'app con **autenticazione biometrica** (impronta/riconoscimento facciale) e nascondi gli importi sensibili nella Dashboard.

#### 📈 Dashboard e Report
*   **Paginazione Mensile**: Naviga tra i mesi con gesti di scorrimento orizzontale.
*   **Riepilogo Entrate/Uscite**: Saldo mensile, confronto con il mese precedente e categorie più frequenti.
*   **Dashboard Cards**: Riepilogo economico del mese con saldo mensile, spese del periodo, entrate totali e spese per carta di credito.

#### 🎨 Interfaccia Moderna
*   Design pulito basato su **Material Design 3** con supporto **Dynamic Colors** (Android 12+).
*   Supporto nativo per Tema Chiaro e Scuro.
*   Transizioni condivise e animazioni fluide.

### 🛠️ Stack Tecnologico

| Categoria | Tecnologia |
|---|---|
| **Linguaggio** | [Kotlin](https://kotlinlang.org/) 2.3.21 |
| **UI Toolkit** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3, BOM 2026.05.00) |
| **Architettura** | MVVM + Repository + Use Cases (Clean Architecture) |
| **Database Locale** | Room (SQLite) 2.8.4 — 4 entità (Transaction, Category, CreditCard, CurrencyRate) |
| **DI** | Manuale con `ViewModelProvider.Factory` |
| **Navigazione** | Jetpack Navigation Compose 2.9.8 |
| **Async** | Coroutines & Flow (StateFlow per UI) |
| **Caricamento Immagini** | [Coil](https://coil-kt.github.io/coil/compose/) 2.7.0 |
| **Crop Immagini** | [uCrop](https://github.com/Yalantis/uCrop) 2.2.9 |
| **Serializzazione** | Gson 2.14.0 |
| **Biometrico** | AndroidX Biometric 1.1.0 |
| **Wear OS** | Wear Compose Material 1.6.1, Play Services Wearable 20.0.1 |
| **Build** | Gradle 9.5.1, AGP 9.2.1, KSP 2.3.8 |
| **Formattazione** | Spotless 8.5.1 con ktlint 1.0.1 |
| **Min SDK / Target** | 26 / 36 |
| **Java** | Java 21 |

### 📁 Struttura del Progetto

```
app/
├── src/main/java/com/expense/management/
│   ├── data/                  # Room DB, DAOs, Entities, Repository
│   ├── domain/usecase/        # Use Case classes (business logic pura)
│   ├── ui/
│   │   ├── model/             # UI data classes
│   │   ├── screens/           # Compose screens
│   │   │   └── category/      # Category management + dialog
│   │   └── theme/             # Tema Material 3 (chiaro/scuro/dinamico)
│   ├── utils/                 # Utility functions
│   ├── viewmodel/             # ViewModel + Factory
│   └── MainActivity.kt
wear/                           # App companion Wear OS
gradle/libs.versions.toml       # Version catalog
```

### 🚀 Build

```bash
# Build debug APK
./gradlew assembleDebug

# Esegui lint
./gradlew lint

# Formatta codice
./gradlew spotlessApply
```

### 📄 Licenza

```
Copyright 2026 Alessandro Sampognaro

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## English

**Expense Manager** is a native Android application developed with **Kotlin** and **Jetpack Compose**, designed to offer complete and flexible control over personal finances. It includes a **Wear OS** companion app for quick entry from your wrist.

### ✨ Key Features

#### 📊 Transaction Management
*   **Expenses and Incomes**: Easily record every financial movement.
*   **Custom Categories**: Create and manage your own categories with icons, emojis, and **custom images** (with built-in crop).
*   **Quick Descriptions**: Autocomplete descriptions based on previous transactions.
*   **Recurring Transactions**: Support for daily, weekly, monthly, and yearly recurring expenses with configurable limits.
*   **Group Deletion**: Delete a recurring transaction and all its future occurrences with one tap.

#### 💳 Advanced Credit Card Support
Manage credit card payments like a pro:
*   **Debit Date Calculation**: Set the card's closing/debit day or monthly delay. The app automatically calculates exactly when money will leave your account.
*   **Flexible Modes**: Support for **single balance**, **installments**, or manual management.
*   **Mirror Transactions**: Each credit card expense auto-generates a matching "adjustment" transaction to track the actual future debit.

#### 📅 Installment Planning
*   **Expense Splitting**: Made a large purchase in installments? Enter the total amount and the number of installments.
*   **Automatic Generation**: The app automatically creates future transactions for each month, offering a clear projection of future outflows.
*   **Credit Card Support**: Installments on credit cards are handled with related debit mirror transactions.

#### 🌍 Multi-Currency & Exchange Rates
*   **Dual Currency**: Record the converted amount in your main currency while keeping the original reference.
*   **ECB Rates**: The app automatically downloads daily exchange rates from the **European Central Bank** for accurate conversions.
*   **Local Cache**: Rates are stored in Room for offline access.

#### 🖼️ Category Images with Crop
*   **Gallery Picker**: Choose an image from your device gallery.
*   **Built-in Crop**: Resize and crop the image with the **uCrop** interface before saving (1:1 aspect ratio, max 512×512).
*   **Persistence**: Images are copied to internal storage and remain visible after app restart.

#### ⌚ Wear OS Companion
*   **Quick Entry**: Add expenses directly from your Wear OS watch.
*   **Safe Delivery**: Data is sent to the phone via Google Play Services Wearable. No separate installation required.

#### 💾 Backup & Security
*   **JSON Export**: Full backup of transactions, categories, and credit cards in JSON format.
*   **CSV Export**: Export transactions to CSV (Excel/Google Sheets) with selectable columns.
*   **Smart Restore**: Restore from JSON backup with automatic legacy format normalization.
*   **Auto Backup**: Android automatic backup with custom rules (`backup_rules.xml`).
*   **Privacy**: Protect app access with **biometric authentication** and hide sensitive amounts on the Dashboard.

#### 📈 Dashboard & Reports
*   **Monthly Navigation**: Swipe horizontally between months.
*   **Income/Expense Summary**: Monthly balance, comparison with previous month, and frequent categories.
*   **Dashboard Cards**: Monthly economic summary with balance, period expenses, total income, and credit card spending.

#### 🎨 Modern Interface
*   Clean design based on **Material Design 3** with **Dynamic Colors** (Android 12+).
*   Native Light and Dark Theme support.
*   Shared transitions and smooth animations.

### 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) 2.3.21 |
| **UI Toolkit** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3, BOM 2026.05.00) |
| **Architecture** | MVVM + Repository + Use Cases (Clean Architecture) |
| **Local Database** | Room (SQLite) 2.8.4 — 4 entities (Transaction, Category, CreditCard, CurrencyRate) |
| **DI** | Manual with `ViewModelProvider.Factory` |
| **Navigation** | Jetpack Navigation Compose 2.9.8 |
| **Async** | Coroutines & Flow (StateFlow for UI) |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/compose/) 2.7.0 |
| **Image Crop** | [uCrop](https://github.com/Yalantis/uCrop) 2.2.9 |
| **Serialization** | Gson 2.14.0 |
| **Biometric** | AndroidX Biometric 1.1.0 |
| **Wear OS** | Wear Compose Material 1.6.1, Play Services Wearable 20.0.1 |
| **Build** | Gradle 9.5.1, AGP 9.2.1, KSP 2.3.8 |
| **Formatting** | Spotless 8.5.1 with ktlint 1.0.1 |
| **Min SDK / Target** | 26 / 36 |
| **Java** | Java 21 |

### 📁 Project Structure

```
app/
├── src/main/java/com/expense/management/
│   ├── data/                  # Room DB, DAOs, Entities, Repository
│   ├── domain/usecase/        # Use Case classes (pure business logic)
│   ├── ui/
│   │   ├── model/             # UI data classes
│   │   ├── screens/           # Compose screens
│   │   │   └── category/      # Category management + dialog
│   │   └── theme/             # Material 3 theme (light/dark/dynamic)
│   ├── utils/                 # Utility functions
│   ├── viewmodel/             # ViewModel + Factory
│   └── MainActivity.kt
wear/                           # Wear OS companion app
gradle/libs.versions.toml       # Version catalog
```

### 🚀 Build

```bash
# Build debug APK
./gradlew assembleDebug

# Run lint
./gradlew lint

# Format code
./gradlew spotlessApply
```

### 📄 License

```
Copyright 2026 Alessandro Sampognaro

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
