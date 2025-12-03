# 💰 Gestore Spese / Expense Manager

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green.svg)

[🇮🇹 Italiano](#italiano) | [🇬🇧 English](#english)

---

## Italiano

**Gestore Spese** è una moderna applicazione Android nativa sviluppata con **Kotlin** e **Jetpack Compose**, progettata per offrire un controllo completo e flessibile sulle finanze personali.

A differenza delle classiche app di tracciamento spese, Gestore Spese include funzionalità avanzate per gestire scenari reali complessi come **carte di credito** (con addebito posticipato) e **pagamenti rateali**.

### ✨ Funzionalità Principali

#### 📊 Gestione Transazioni
*   **Spese ed Entrate**: Registra facilmente ogni movimento finanziario.
*   **Categorie Intelligenti**: Le categorie si adattano automaticamente in base al tipo di transazione (Spesa o Entrata).
*   **Descrizioni Veloci**: Autocompletamento delle descrizioni basato sulle transazioni precedenti.

#### 💳 Supporto Avanzato Carte di Credito
Gestisci i pagamenti con carta di credito come un vero professionista:
*   **Calcolo Data Addebito**: Imposta il giorno di chiusura/addebito della carta. L'app calcolerà automaticamente la *Data Valuta* (Effective Date) nel mese successivo, permettendoti di sapere esattamente quando i soldi usciranno dal conto.

#### 📅 Pianificazione Rateale
*   **Suddivisione Spese**: Hai fatto un acquisto importante a rate? Inserisci l'importo totale e il numero di rate.
*   **Generazione Automatica**: L'app crea automaticamente le transazioni future per ogni mese, offrendo una proiezione chiara delle uscite future.

#### 🌍 Multi-Valuta (Viaggi)
*   **Doppia Valuta**: Registra l'importo convertito nella tua valuta principale, ma conserva anche l'importo e la valuta originale (es. USD, GBP). Utile per riconciliare le spese fatte all'estero.

#### 🎨 Interfaccia Moderna
*   Design pulito basato su **Material Design 3**.
*   Supporto nativo per Tema Chiaro e Scuro.
*   Componenti UI reattivi e animazioni fluide.

### 🛠️ Stack Tecnologico

Il progetto segue le moderne best practices di sviluppo Android:
*   **Linguaggio**: [Kotlin](https://kotlinlang.org/)
*   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architettura**: MVVM (Model-View-ViewModel)
*   **Database Locale**: Room (SQLite)
*   **Gestione Date**: Java Time API (`java.time.LocalDate`, `YearMonth`)
*   **Navigazione**: Jetpack Navigation

---

## English

**Expense Manager** is a native Android application developed with **Kotlin** and **Jetpack Compose**, designed to offer complete and flexible control over personal finances.

Unlike standard expense tracking apps, Expense Manager includes advanced features to handle complex real-world scenarios such as **credit cards** (with deferred debit) and **installment payments**.

### ✨ Key Features

#### 📊 Transaction Management
*   **Expenses and Incomes**: Easily record every financial movement.
*   **Smart Categories**: Categories automatically adapt based on the transaction type (Expense or Income).
*   **Quick Descriptions**: Autocomplete descriptions based on previous transactions.

#### 💳 Advanced Credit Card Support
Manage credit card payments like a pro:
*   **Debit Date Calculation**: Set the card's closing/debit day. The app automatically calculates the *Effective Date* in the following month, letting you know exactly when money will leave your account.

#### 📅 Installment Planning
*   **Expense Splitting**: Made a large purchase in installments? Enter the total amount and the number of installments.
*   **Automatic Generation**: The app automatically creates future transactions for each month, offering a clear projection of future outflows.

#### 🌍 Multi-Currency (Travel)
*   **Dual Currency**: Record the converted amount in your main currency, but also keep the original amount and currency (e.g., USD, GBP). Useful for reconciling expenses made abroad.

#### 🎨 Modern Interface
*   Clean design based on **Material Design 3**.
*   Native support for Light and Dark Themes.
*   Reactive UI components and fluid animations.

### 🛠️ Tech Stack

The project follows modern Android development best practices:
*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Local Database**: Room (SQLite)
*   **Date Management**: Java Time API (`java.time.LocalDate`, `YearMonth`)
*   **Navigation**: Jetpack Navigation

---
[Licence MIT ](LICENCE.md)
