# Privacy Policy - Gestore Spese

**Ultimo aggiornamento:** [04/12/2025]

Questa Informativa sulla Privacy descrive come l'applicazione "Gestore Spese" gestisce i tuoi dati. Rispettiamo la tua privacy e ci impegniamo a proteggere le tue informazioni personali.

## 1. Raccolta e Utilizzo dei Dati
L'applicazione è progettata per funzionare offline. I dati finanziari inseriti (transazioni, categorie, ecc.) sono salvati in un database locale sul tuo dispositivo.

**Lo sviluppatore non raccoglie, trasmette o ha accesso ai tuoi dati personali o finanziari.** Nessun dato viene inviato a server esterni gestiti dallo sviluppatore.

## 2. Dati Biometrici
L'applicazione utilizza l'autenticazione biometrica (impronta digitale o riconoscimento facciale) per proteggere l'accesso ai tuoi dati.

*   **Come viene utilizzata:** Utilizziamo le API standard di Android (`androidx.biometric`) per delegare l'autenticazione al sistema operativo.
*   **Archiviazione:** L'applicazione **NON raccoglie, archivia, memorizza o condivide** i tuoi dati biometrici.
*   **Funzionamento:** L'app riceve solo una notifica di "successo" o "fallimento" dal sistema operativo Android una volta completata la verifica. I dati biometrici grezzi rimangono protetti all'interno dell'hardware di sicurezza del tuo dispositivo (Secure Enclave/TEE).

## 3. File e Backup
L'applicazione consente di esportare dati (CSV) e creare backup completi (JSON).

*   **Permessi:** L'app utilizza il sistema di selezione file di Android (Storage Access Framework). L'utente sceglie esplicitamente dove salvare il file (memoria interna o servizi cloud come Google Drive).
*   **Accesso:** L'app ha accesso in lettura/scrittura solo ai file specifici selezionati dall'utente per le operazioni di backup o ripristino.
*   **Destinazione:** I file di backup rimangono sotto il controllo esclusivo dell'utente. Lo sviluppatore non ha accesso a questi file.

## 4. Permessi Richiesti
*   `USE_BIOMETRIC`: Necessario per verificare l'identità dell'utente tramite i sensori del dispositivo.
*   Accesso allo Storage (tramite SAF): Necessario per leggere/scrivere i file di backup e export scelti dall'utente.

## 5. Contatti
Per domande riguardanti questa privacy policy, puoi contattare lo sviluppatore a: [Inserisci la tua Email]

---

# Privacy Policy - Expense Manager (Gestore Spese)

**Last Updated:** [04/12/2025]

This Privacy Policy describes how the "Gestore Spese" application handles your data. We respect your privacy and are committed to protecting your personal information.

## 1. Data Collection and Usage
The application is designed to work offline. The financial data you enter (transactions, categories, etc.) is saved in a local database on your device.

**The developer does not collect, transmit, or have access to your personal or financial data.** No data is sent to external servers managed by the developer.

## 2. Biometric Data
The application uses biometric authentication (fingerprint or facial recognition) to protect access to your data.

*   **How it is used:** We use standard Android APIs (`androidx.biometric`) to delegate authentication to the operating system.
*   **Storage:** The application **does NOT collect, archive, store, or share** your biometric data.
*   **Operation:** The app only receives a "success" or "failure" notification from the Android operating system once verification is complete. Raw biometric data remains protected within your device's security hardware (Secure Enclave/TEE).

## 3. Files and Backup
The application allows you to export data (CSV) and create full backups (JSON).

*   **Permissions:** The app uses the Android file selection system (Storage Access Framework). The user explicitly chooses where to save the file (internal storage or cloud services like Google Drive).
*   **Access:** The app has read/write access only to the specific files selected by the user for backup or restore operations.
*   **Destination:** Backup files remain under the exclusive control of the user. The developer has no access to these files.

## 4. Required Permissions
*   `USE_BIOMETRIC`: Required to verify the user's identity via the device sensors.
*   Storage Access (via SAF): Required to read/write the backup and export files chosen by the user.

## 5. Contact
For questions regarding this privacy policy, you can contact the developer at: [Insert your Email]
