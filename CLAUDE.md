# CLAUDE.md — Gestore Spese

Guida per Claude Code su questo repo. Derivata da `AGENTS.md` (usato da OpenCode), ma allineata al codice reale.

> `AGENTS.md` contiene fatti **obsoleti** (DB v15, 11 entity, `fallbackToDestructiveMigration` sempre attivo). Per i fatti di architettura/DB vale questo file.

---

## 1. Stile di lavoro atteso

- **Output compresso.** Niente preamboli, niente "Certo!", niente spiegazioni non richieste. Codice + una riga di contesto.
- **Diff mirati.** Non riscrivere file interi. Modifica solo le righe necessarie.
- **Codice finale.** Nessun `// TODO`, nessun placeholder, nessun log di debug nel codice consegnato.
- **No wildcard import.** Import espliciti, tipi impliciti dove possibile.
- **Italiano per i commenti utente e le stringhe**, inglese per nomi di codice.
- Se serve ancora più compressione: skill `anthropic-skills:caveman`.

### GOLDEN RULES
- **Nessun push** non ti è permesso pushare, quello lo farà un utente umano
- **Commit** puoi fare i commit, cerca di fargli sintetici, al massimo 150 caratteri

### Gating su feature multi-step
Per feature che toccano DB + domain + UI: entra in **plan mode**, fai approvare il piano, poi esegui **un passo per messaggio**. Non mescolare nello stesso edit codice UI e codice di persistenza/migrazione. Le migrazioni Room vanno sempre in un passo isolato e verificato.

### Stato sessione
- Leggi `.tasks/current_state.txt` all'inizio per capire dove si era rimasti (branch, cosa non è committato, test manuali pendenti).
- A feature completa: aggiorna `.tasks/current_state.txt` e scrivi un report `.tasks/YYYY-MM-DD-nome-feature.md` (vedi i precedenti come template).

### Limiti
- **Mai cancellare o sostituire interamente un file** senza permesso esplicito.
- Non committare né pushare se non richiesto. Il merge su `main` lo fa l'utente a mano.

---

## 2. Comandi

```bash
./gradlew :app:compileDebugKotlin   # check rapido di compilazione
./gradlew spotlessApply             # formattazione ktlint (obbligatoria prima del commit)
./gradlew spotlessCheck             # quello che gira in CI
./gradlew test                      # unit test (JUnit 5 + MockK)
./gradlew testDebugUnitTest         # quello che gira in CI
./gradlew lint                      # Android lint
./gradlew assembleDebug             # APK debug -> app/build/outputs/apk/debug/app-debug.apk
```

Ciclo tipico prima di dichiarare "fatto": `spotlessApply` → `compileDebugKotlin` → `test` → (se UI toccata) `assembleDebug`.

CI (`.github/workflows/android_ci.yml`, su push/PR verso `main`/`master`): `spotlessCheck` → `testDebugUnitTest` → `assembleDebug` + upload APK.

---

## 3. Struttura del progetto

Moduli: `:app` (namespace `com.expense.management`) e `:wear` (companion Wear OS, minimale).

Toolchain: compileSdk/targetSdk **36**, minSdk **26**, Java/JVM target **21**, Gradle KTS + version catalog (`gradle/libs.versions.toml`), KSP per Room.

```
app/src/main/java/com/expense/management/
├── MainActivity.kt            FragmentActivity (biometria), crea repo + 4 ViewModel, Scaffold/TopBar/BottomBar/FAB
├── data/                      Room: entity, 5 DAO, ExpenseRepository, mapper entity→domain
├── domain/
│   ├── model/                 Kotlin puro, zero dipendenze Android
│   ├── repository/            PaymentMethodRepository (port esagonale)
│   └── usecase/               ~25 use case, uno per file
├── ui/
│   ├── navigation/            AppNavHost, AppTopBar, AppBottomBar, BiometricGate
│   ├── screens/               una cartella/file per schermata (+ amex/, category/, report/, settings/)
│   ├── theme/                 AppStyle, Color, Theme, Shape, Type
│   └── model/
├── viewmodel/                 ExpenseViewModel, AmexViewModel, PaymentMethodsViewModel, CreditCardViewModel (+ 2 Factory)
├── utils/                     DateUtils, CurrencyUtils, BackupUtils, BiometricUtils
└── services/                  WearableDataListenerService
```

### Dependency injection
**Manuale**, nessun Hilt/Koin. `MainActivity` costruisce `AppDatabase` → `ExpenseRepository` → passa tutto a `ExpenseViewModelFactory` / `CreditCardViewModelFactory`. Un nuovo use case si aggancia qui: non introdurre un framework DI.

### Navigation
`AppNavHost.kt` (604 righe), rotte stringa semplici: `dashboard`, `report`, `categories`, `settings`, `payment_methods`, `data_management`, `security`, `amex_hub`, più `add_transaction/{transactionId}` e `add_credit_card_transaction/{transactionId}`. Sentinella `__cash__` per il metodo di pagamento contanti.

### ViewModel
4 ViewModel, non uno solo. Divisione per dominio:
- `ExpenseViewModel` — transazioni, categorie, delete flow, valute
- `AmexViewModel` — statement Amex, piani PagoFlex, proiezioni, Hub
- `PaymentMethodsViewModel` — metodi di pagamento, carte attive, rate, BNPL
- `CreditCardViewModel` — carte legacy

Stato esposto come `StateFlow`, raccolto nelle composable con `collectAsStateWithLifecycle()`. Composable di layout **stateless**: stato + lambda di evento dall'alto (UDF stretto).

---

## 4. Database (fatti chiave)

- Nome: **`spese_db_v6`** — versione **22** — `exportSchema = true`, schemi in `app/schemas/`.
- **17 entity** dichiarate in `AppDatabase.kt`.
- **5 DAO**: `transactionDao`, `categoryDao`, `currencyDao`, `paymentMethodDao`, `amexDao`.
- Migrazioni **6→22 tutte registrate** in `addMigrations(...)`. Ogni bump di versione richiede una `Migration` nuova + il JSON di schema committato.
- `fallbackToDestructiveMigration(dropAllTables = true)` è attivo **solo su build debuggable**. In release una migrazione mancante crasha: **non deve mai succedere**.
- `ExpenseRepository` espone `suspend fun <T> withTransaction(block)` — usalo per ogni operazione multi-tabella (es. ricalcolo piani Amex con relink delle mirror transaction).

### Sistema metodi di pagamento
`PaymentMethodEntity` + tabella di dettaglio per provider, con FK `ON DELETE CASCADE` verso `payment_methods`.

`PaymentProvider`: `CASH`, `CREDIT_CARD_SALDO`, `CREDIT_CARD_REVOLVING`, `CREDIT_CARD_INSTALLMENT`, `CREDIT_CARD_AMEX`, `DEBIT_CARD`, `REVOLUT`, `SATISPAY`, `PAYPAL`, `KLARNA`. Usa `PaymentProvider.safeValueOf()` quando leggi dal DB.

`CreditCardEntity` legacy è ancora vivo in parallelo: confinato alla UI di gestione nelle Settings. I flussi nuovi usano `activeCreditCards: StateFlow<List<ActiveCreditCard>>` (merge di `PaymentMethodEntity` + `CreditCardDetailEntity`).

### Transazioni
`TransactionEntity` ha `date` vs `effectiveDate` (data di addebito reale, calcolata da `DateUtils.calculateEffectiveDate` che riceve un `CardDateInfo` per restare disaccoppiata dalle entity), `originalAmount`/`originalCurrency` per il multivaluta, `groupId` per rate e ricorrenze, `creditCardId` + `paymentMethodId` (doppio sistema), campi `recurrence*`.

### Amex
Sottosistema più complesso: `AmexStatementEntity`, `AmexPagoFlexPlanEntity`, `AmexPagoFlexScheduledPaymentEntity`, `AmexPagoFlexPlanChangeEntity`, `AmexRevolvingStateEntity`. Pagamenti statement e rate generano **mirror transaction** sul conto corrente: ogni modifica o cancellazione di un piano deve passare da `ApplyAmexPlanRecalculationUseCase` (relink mirror + cleanup orfani, dentro `withTransaction`) per non lasciare duplicati.

---

## 5. UI / Tema

- Material 3 + Compose BOM, `SharedTransitionLayout` in `MainActivity`.
- 4 temi in `AppStyle`: `MATERIAL_YOU` (dynamic color), `NORDIC`, `CYBERPUNK`, `CORPORATE`. Schema light/dark per ognuno in `Color.kt`, selezione in `AppTheme()` in `Theme.kt`.
- Mai colori hardcoded: usa i token `MaterialTheme.colorScheme`.
- Stringhe sempre in `strings.xml` + `values-it/strings.xml`. Niente testo hardcoded nelle composable.
- `ktlint_function_naming_ignore_when_annotated_with = Composable`: le `@Composable` possono essere PascalCase.
- File grossi da tenere d'occhio: `DashboardScreen.kt` (1554), `AddCreditCardTransactionScreen.kt` (877), `AddTransactionSections.kt` (844). Se tocchi questi, estrai invece di gonfiare.

---

## 6. Test

JUnit 5 (`useJUnitPlatform()`) + MockK. 14 test file in `app/src/test/`, concentrati su `domain/usecase/` (Amex, cash flow, BNPL, report, save/delete transaction).

Regola: **ogni use case nuovo o modificato in `domain/usecase/` vuole un test**. Il domain è Kotlin puro, quindi testabile senza Robolectric.

---

## 7. Sub-agent e skill

`AGENTS.md` elenca sub-agent OpenCode in `.opencode/agents/` (android-architect, android-hex, room-specialist, compose-specialist, reviewer-android, …). Quei file restano **ottima documentazione delle convenzioni di dominio**: leggili quando lavori nell'area corrispondente (es. `.opencode/agents/room-specialist.md` prima di toccare lo schema, `.opencode/agents/amexSpecialist.md` per la logica Amex).

In Claude Code però usa gli strumenti nativi:
- skill `kotlinsense:kotlin-android-patterns` / `kotlinsense:kotlin-diagnostics` per codice Kotlin LSP-clean
- `/code-review` per la review (sostituisce `reviewer-android`)
- agent `Explore` per cercare in largo, `Plan` per progettare
- `.opencode/node_modules/` è rumore: ignoralo nelle ricerche
