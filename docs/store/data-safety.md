# Modulo Data Safety (Play Console): risposte proposte

> BOZZA. Le risposte sono responsabilità dello sviluppatore: rileggerle in Play Console prima dell'invio.
> Basate sul codice al commit df71cbc (2026-11-03). Da rivedere se si aggiungono SDK (es. crash reporting in C5).

## Fonti
- Definizioni e esenzioni del modulo: https://support.google.com/googleplay/android-developer/answer/10787469
- Dati raccolti da ML Kit: https://developers.google.com/ml-kit/android-data-disclosure

## Cosa fa l'app con i dati (riassunto per compilare)
| Flusso | Lascia il telefono? | Classificazione proposta |
|---|---|---|
| Movimenti, carte, categorie nel DB locale | No | Non raccolti (solo sul dispositivo) |
| Foto scontrino, OCR | Il testo/la foto no (OCR sul dispositivo) | Non raccolti |
| Tassi di cambio BCE | Solo una richiesta HTTPS del listino pubblico, nessun dato utente | Non raccolti |
| ML Kit via Play Services | Sì: info dispositivo/app, ID per installazione, diagnostica | **Raccolti** dall'SDK, vedi sotto |
| Backup JSON / CSV | Solo su azione dell'utente, verso destinazione scelta dall'utente, app senza accesso dopo | Non raccolti e non condivisi (esenzione "azione avviata dall'utente") |
| Backup automatico Android | Gestito da Android sull'account Google dell'utente | Non raccolti dallo sviluppatore (vedi nota 2) |
| Orologio Wear OS -> telefono | Tra dispositivi dell'utente, via Play Services | Non raccolti (vedi nota 3) |
| Biometria | No, l'app riceve solo esito | Non raccolti |

## Risposte, sezione per sezione

### Raccolta e sicurezza dei dati
- **La tua app raccoglie o condivide uno dei tipi di dati utente richiesti?** Sì (solo per ML Kit).
- **Tutti i dati raccolti sono criptati in transito?** Sì (ML Kit usa HTTPS; la richiesta BCE è HTTPS e non contiene dati utente).
- **Gli utenti possono chiedere l'eliminazione dei dati?** Vedi nota 1: decisione dello sviluppatore.

### Tipi di dati da dichiarare (tutti: Raccolti = Sì, Condivisi = No)
| Categoria Play | Tipo | Elaborazione temporanea | Obbligatorio/facoltativo | Finalità |
|---|---|---|---|---|
| Info e prestazioni app | Diagnostica | No | Obbligatorio (non disattivabile nell'app) | Analisi |
| Info e prestazioni app | Altri dati sulle prestazioni dell'app | No | Obbligatorio | Analisi |
| ID dispositivo o altri ID | ID dispositivo o altri ID | No | Obbligatorio | Analisi |

Motivo: ML Kit invia a Google "device information, application information, per-installation identifiers,
performance metrics, error codes" per "diagnostics and usage analytics" e non li trasferisce a terzi. Google qui
agisce come fornitore di servizi dell'SDK: niente "condivisione".

### Tutti gli altri tipi (Posizione, Info personali, Info finanziarie, Foto e video, Messaggi, Contatti, ecc.)
Non raccolti, non condivisi. In particolare **Info finanziarie: No**, perché i movimenti restano sul telefono.

## Note da decidere
1. **Eliminazione dei dati.** I dati dell'app sono sul telefono (si cancellano in-app o disinstallando). I dati
   diagnostici di ML Kit li conserva Google e lo sviluppatore non ha un modo per cancellarli. Opzioni: rispondere
   "No" alla domanda sull'eliminazione, oppure indicare l'email di contatto spiegando cosa è possibile.
2. **Backup automatico Android.** La guida di Play non lo nomina. È gestito da Android sull'account Google
   dell'utente, cifrato con il blocco schermo su Android 9+, e lo sviluppatore non vi accede: la lettura più
   comune è che non sia "raccolta" dell'app. Se si preferisce la prudenza, dichiarare Info finanziarie come
   raccolte per "Funzionalità app" con nota esplicativa.
3. **Wear OS.** Il movimento passa dall'orologio al telefono dello stesso utente tramite il Data Layer di Play
   Services, tra due app dello stesso sviluppatore. La guida esclude dalla condivisione i trasferimenti avviati
   dall'utente che se li aspetta; non c'è un server dello sviluppatore. Proposta: non dichiararlo.
4. **Se in C5 si aggiunge un crash reporting** (es. Firebase Crashlytics), questo modulo va aggiornato:
   Diagnostica e ID, finalità "Analisi", più eventuale "Arresti anomali".
