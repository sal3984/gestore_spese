# Privacy Policy - Gestore Spese

**Ultimo aggiornamento:** [Inserisci Data Oggi]

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
