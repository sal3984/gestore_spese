# Privacy Policy — Gestore Spese / Expense Manager

**Last updated:** 24 May 2026

## Data Collection and Storage

Gestore Spese ("the App") is a personal expense tracking application. The App **does not collect, transmit, or share any personal data** with third parties.

### What data is stored

All data is stored **exclusively on your device** in a local SQLite database (Room):

| Data | Purpose | Storage |
|---|---|---|
| Transactions (amount, description, date, category) | Expense/income tracking | Local device only |
| Categories (name, icon, image) | Transaction categorization | Local device only |
| Credit card configurations | Card management features | Local device only |
| Currency exchange rates | Multi-currency conversion | Local device only |
| App preferences (currency, date format, CC settings) | User experience customization | Local device only |
| Category images | Custom category icon display | Local device's internal storage |

### Biometric authentication

The App may use your device's biometric API (fingerprint, face recognition) **solely for local app unlock**. No biometric data is ever stored or transmitted by the App.

### Wear OS companion

When using the Wear OS companion app, transaction data is sent from the watch to the phone **only via Google Play Services Wearable APIs**. This communication is encrypted by the platform and occurs exclusively between your paired devices.

### Third-party services

The App makes the following network requests:

1. **European Central Bank (ECB) exchange rate feed** — Used exclusively to download daily currency exchange rates. No user data is included in this request.
2. **Google Play Services** — Standard Android/Wear OS API calls (no personal data transmitted).

The App uses the following third-party libraries:

- **Coil** — Loads images from local URIs. No network image loading.
- **uCrop** — Image cropping interface. Processes images entirely on-device.
- **Gson** — Local JSON serialization for backup files.

### Backup files

JSON and CSV backup files are created **only when you explicitly request them** and are saved to the location **you choose** on your device or cloud storage. These files contain your full transaction history and should be handled with care.

### Data deletion

You can delete all data at any time by:
1. Uninstalling the App — this removes the entire local database and stored images.
2. Using the App's delete functions — individual transactions, categories, or credit cards.
3. Clearing the App's data from Android system settings.

### Permissions

The App may request the following permissions:

| Permission | Purpose |
|---|---|
| `USE_BIOMETRIC` | Optional app unlock via fingerprint/face |
| `INTERNET` | Downloading ECB exchange rates |
| `READ_MEDIA_IMAGES` (Android 13+) / `READ_EXTERNAL_STORAGE` (Android 12-) | Selecting images for categories |
| Wearable data sync | Wear OS communication |

### Children's privacy

The App is not directed at children under the age of 13. It does not knowingly collect any personal information from children.

### Changes to this policy

Updates to this privacy policy will be reflected in the App's repository and in the next app update.

### Contact

For questions about this privacy policy, open an issue at the project repository or contact the developer directly.

---

**Summary:** Your data stays on your device. Always.
