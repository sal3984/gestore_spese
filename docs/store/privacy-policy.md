# Gestore Spese / Expense Manager: Privacy Policy

> BOZZA da rivedere e pubblicare (sostituisce il gist del 08/12/2025). Da sistemare prima della pubblicazione:
> la data, l'indirizzo pubblico dove la si pubblica, e il link in Impostazioni > Info se cambia.

---

## Italiano

**Ultimo aggiornamento: [DATA DI PUBBLICAZIONE]**

Gestore Spese è un'app per tenere traccia di spese, entrate, carte di credito e rate. È pensata per funzionare
sul tuo telefono: lo sviluppatore non ha server e non riceve i tuoi dati.

### Dati che l'app conserva sul telefono
- Movimenti, categorie, metodi di pagamento, carte, cicli di fatturazione, piani rateali e impostazioni.
- Le immagini che scegli per le categorie personalizzate.
- Tutto resta nel database e nella memoria privata dell'app. Disinstallando l'app i dati vengono cancellati,
  salvo i backup che hai creato tu (vedi sotto).

### Cosa esce dal telefono, e perché
| Cosa | Verso chi | Perché | Contiene dati tuoi? |
|---|---|---|---|
| Richiesta dei tassi di cambio | Banca Centrale Europea (ecb.europa.eu) | Convertire importi in altre valute | No: è un semplice download del listino pubblico |
| Dati tecnici del riconoscimento testo (ML Kit) | Google, tramite Google Play Services | Diagnostica e statistiche d'uso della funzione | Non i tuoi movimenti né le foto: modello e versione del telefono, versione dell'app, identificativi tecnici per installazione, prestazioni ed errori |
| Backup automatico di Android (se attivo sul telefono) | Il tuo account Google, tramite Android | Ritrovare i dati su un nuovo telefono | Sì: database, impostazioni e immagini delle categorie. Lo gestisce Android, lo sviluppatore non vi ha accesso |
| Backup JSON ed esportazione CSV | Il file e la posizione che scegli tu | Copia di sicurezza o analisi dei dati | Sì, ma solo quando lo chiedi e dove decidi tu. Lo sviluppatore non vi ha accesso |
| Movimenti inseriti dall'orologio Wear OS | Dal tuo orologio al tuo telefono, tramite Google Play Services | Aggiungere una spesa dall'orologio | Sì, il movimento inserito. Passa solo tra i tuoi dispositivi |

### Fotocamera e scansione degli scontrini
La foto di uno scontrino viene letta **sul telefono** per proporre importo e data. La foto non viene inviata a
nessuno e non entra nei tuoi dati né nei backup: resta solo nella memoria temporanea (cache) dell'app, che Android
può svuotare in qualsiasi momento.

### Sblocco con biometria
Se attivi il blocco, lo sblocco è gestito da Android: l'app riceve solo "riuscito" o "non riuscito" e non vede
mai impronte, volto o PIN. Con il blocco attivo l'app nasconde il suo contenuto nelle app recenti, impedisce gli
screenshot e chiede di nuovo lo sblocco dopo un minuto fuori dall'app.

### Permessi
- **Internet**: tassi di cambio della BCE e servizi di Google Play per il riconoscimento del testo.
- **Fotocamera**: solo quando scegli di fotografare uno scontrino.
- **Biometria**: solo se attivi il blocco dell'app.
- **File**: l'app legge e scrive solo i file che scegli tu nel selettore di Android.

### Pubblicità, tracciamento, vendita dei dati
Nessuna pubblicità, nessun profilo pubblicitario, nessuna vendita o cessione dei tuoi dati.

### I tuoi diritti
I dati sono sul tuo telefono: puoi modificarli o cancellarli dall'app, o cancellarli tutti disinstallandola. Per
i dati tecnici raccolti da Google per ML Kit valgono le norme sulla privacy di Google
(https://policies.google.com/privacy). Il backup automatico si gestisce dalle impostazioni di Android.

### Minori
L'app non è rivolta a minori di 13 anni.

### Modifiche
Se questa informativa cambia, la nuova versione sarà pubblicata a questo indirizzo con una nuova data.

### Contatti
alsampognaro@gmail.com

---

## English

**Last updated: [PUBLICATION DATE]**

Expense Manager is an app to track expenses, income, credit cards and installments. It is built to work on your
phone: the developer runs no servers and does not receive your data.

### Data the app keeps on your phone
- Transactions, categories, payment methods, cards, billing cycles, installment plans and settings.
- Images you choose for custom categories.
- Everything stays in the app's private database and storage. Uninstalling the app deletes it, except backups
  you created yourself (see below).

### What leaves your phone, and why
| What | To whom | Why | Does it include your data? |
|---|---|---|---|
| Exchange rate request | European Central Bank (ecb.europa.eu) | Converting amounts in other currencies | No: it downloads the public rate list |
| Text recognition technical data (ML Kit) | Google, through Google Play Services | Diagnostics and usage statistics of the feature | Not your transactions or photos: device model and OS version, app version, per-installation technical identifiers, performance and errors |
| Android Auto Backup (if enabled on your phone) | Your Google account, through Android | Restoring your data on a new phone | Yes: database, settings and category images. Handled by Android; the developer has no access |
| JSON backup and CSV export | The file and location you choose | Safety copy or data analysis | Yes, but only when you ask and where you decide. The developer has no access |
| Transactions entered on a Wear OS watch | From your watch to your phone, through Google Play Services | Adding an expense from the watch | Yes, the entered transaction. It only moves between your own devices |

### Camera and receipt scanning
A receipt photo is read **on the phone** to suggest amount and date. The photo is not sent anywhere and is not
part of your data or backups: it only stays in the app's temporary cache, which Android can clear at any time.

### Biometric unlock
If you turn on the lock, unlocking is handled by Android: the app only gets "succeeded" or "failed" and never
sees fingerprints, face or PIN. With the lock on, the app hides its content in recent apps, blocks screenshots and
asks to unlock again after one minute away.

### Permissions
- **Internet**: ECB exchange rates and Google Play services for text recognition.
- **Camera**: only when you choose to photograph a receipt.
- **Biometrics**: only if you turn on the app lock.
- **Files**: the app reads and writes only the files you pick in the Android file picker.

### Ads, tracking, sale of data
No ads, no advertising profiles, no sale or transfer of your data.

### Your rights
Your data is on your phone: you can edit or delete it in the app, or delete all of it by uninstalling. Technical
data collected by Google for ML Kit is covered by Google's Privacy Policy (https://policies.google.com/privacy).
Android Auto Backup is managed in Android settings.

### Children
The app is not directed to children under 13.

### Changes
If this policy changes, the new version will be published at this address with a new date.

### Contact
alsampognaro@gmail.com
