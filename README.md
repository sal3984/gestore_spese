# 💰 Gestore Spese / Expense Manager

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green.svg)

[🇮🇹 Italiano](#italiano) | [🇬🇧 English](#english)

---

## Italiano

**Gestore Spese** è una moderna applicazione Android nativa sviluppata con **Kotlin** e **Jetpack Compose**, progettata per offrire un controllo completo e flessibile sulle finanze personali.

A differenza delle classiche app di tracciamento spese, Gestore Spese include funzionalità avanzate per gestire scenari reali complessi come **carte di credito** (con addebito posticipato), **pagamenti rateali** e **conversioni valuta**.

### ✨ Funzionalità Principali

#### 📊 Gestione Transazioni
*   **Spese ed Entrate**: Registra facilmente ogni movimento finanziario.
*   **Categorie Personalizzabili**: Crea e gestisci le tue categorie con icone ed emoji personalizzate.
*   **Descrizioni Veloci**: Autocompletamento delle descrizioni basato sulle transazioni precedenti.

#### 💳 Supporto Avanzato Carte di Credito
Gestisci i pagamenti con carta di credito come un vero professionista:
*   **Calcolo Data Addebito**: Imposta il giorno di chiusura/addebito della carta o il ritardo in mesi. L'app calcolerà automaticamente quando i soldi usciranno effettivamente dal conto.
*   **Modalità Flessibili**: Supporto per saldo unico, rateale o gestione manuale.

#### 📅 Pianificazione Rateale
*   **Suddivisione Spese**: Hai fatto un acquisto importante a rate? Inserisci l'importo totale e il numero di rate.
*   **Generazione Automatica**: L'app crea automaticamente le transazioni future per ogni mese, offrendo una proiezione chiara delle uscite future.

#### 🌍 Multi-Valuta e Tassi di Cambio
*   **Doppia Valuta**: Registra l'importo convertito nella tua valuta principale, mantenendo il riferimento all'originale.
*   **Tassi BCE**: L'app scarica automaticamente i tassi di cambio giornalieri dalla **Banca Centrale Europea** per conversioni precise. Possibilità di aggiornamento forzato manuale.

#### 💾 Backup e Sicurezza
*   **Export Dati**: Esporta le tue transazioni in **CSV** (per Excel) o effettua backup completi in **JSON**.
*   **Privacy**: Proteggi l'accesso all'app con **autenticazione biometrica** e nascondi gli importi sensibili nella Dashboard.

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

Unlike standard expense tracking apps, Expense Manager includes advanced features to handle complex real-world scenarios such as **credit cards** (with deferred debit), **installment payments** and **currency conversions**.

### ✨ Key Features

#### 📊 Transaction Management
*   **Expenses and Incomes**: Easily record every financial movement.
*   **Custom Categories**: Create and manage your own categories with custom icons and emojis.
*   **Quick Descriptions**: Autocomplete descriptions based on previous transactions.

#### 💳 Advanced Credit Card Support
Manage credit card payments like a pro:
*   **Debit Date Calculation**: Set the card's closing/debit day or monthly delay. The app automatically calculates exactly when money will leave your account.
*   **Flexible Modes**: Support for single balance, installments, or manual management.

#### 📅 Installment Planning
*   **Expense Splitting**: Made a large purchase in installments? Enter the total amount and the number of installments.
*   **Automatic Generation**: The app automatically creates future transactions for each month, offering a clear projection of future outflows.

#### 🌍 Multi-Currency & Exchange Rates
*   **Dual Currency**: Record the converted amount in your main currency while keeping the original reference.
*   **ECB Rates**: The app automatically downloads daily exchange rates from the **European Central Bank** for accurate conversions. Manual force update available.

#### 💾 Backup & Security
*   **Data Export**: Export your transactions to **CSV** (for Excel) or perform full backups in **JSON**.
*   **Privacy**: Protect app access with **biometric authentication** and hide sensitive amounts on the Dashboard.

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
