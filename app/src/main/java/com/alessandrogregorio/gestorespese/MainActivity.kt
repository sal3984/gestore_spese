package com.alessandrogregorio.gestorespese

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download // NUOVO IMPORT
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.AddTransactionScreen
import com.alessandrogregorio.gestorespese.ui.screens.DashboardScreen
import com.alessandrogregorio.gestorespese.ui.screens.SettingsScreen
import com.alessandrogregorio.gestorespese.viewmodel.ExpenseViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.Locale // IMPORT NECESSARIO

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppEntry()
            }
        }
    }
}

@Composable
fun MainAppEntry() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val viewModel: ExpenseViewModel = viewModel()

    // STATI RICHIESTI DAL VIEWMODEL
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val ccLimit by viewModel.ccLimit.collectAsState()
    val ccDelay by viewModel.ccDelay.collectAsState()

    // --- GESTIONE BACKUP (Launcher per salvare/aprire file) ---
    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { performBackup(context, viewModel, it) }
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { performRestore(context, viewModel, it) }
    }

    // --- GESTIONE EXPORT CSV (Launcher per salvare file CSV) ---
    val createCsvDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { performExportCsv(context, viewModel, it) }
    }

    Scaffold(
        bottomBar = {
            // Barra di Navigazione Inferiore
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('/')

                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, "Mese") },
                    label = { Text("Mese") },
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).padding(8.dp)) { Icon(Icons.Default.Add, "Add", tint = Color.White) } },
                    label = { Text("") }, // Label vuota per effetto FAB
                    selected = currentRoute == "add",
                    onClick = { navController.navigate("add/-1") { launchSingleTop = true } } // Naviga a "Add" con ID = -1L (nuova transazione)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Impostazioni") },
                    label = { Text("Impost.") },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") { launchSingleTop = true } }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Gestore della Navigazione
            NavHost(navController = navController, startDestination = "dashboard") {

                // SCHERMATA 1: DASHBOARD
                composable("dashboard") {
                    DashboardScreen(
                        transactions = transactions,
                        currencySymbol = currency,
                        ccLimit = ccLimit,
                        onDelete = { id -> viewModel.deleteTransaction(id) },
                        onEdit = { id -> navController.navigate("add/$id") } // Naviga alla rotta di modifica
                    )
                }

                // SCHERMATA 2: AGGIUNGI/MODIFICA (supporta ID opzionale)
                composable(
                    route = "add/{transactionId}",
                    arguments = listOf(navArgument("transactionId") {
                        defaultValue = -1L
                        type = NavType.LongType
                    })
                ) { backStackEntry ->
                    val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                    val isEditing = transactionId != -1L

                    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
                    var isLoading by remember { mutableStateOf(isEditing) } // Imposta Loading all'inizio se è in modalità modifica

                    // Se in modalità modifica, recupera la transazione
                    LaunchedEffect(transactionId) {
                        if (isEditing) {
                            isLoading = true
                            transactionToEdit = viewModel.getTransactionById(transactionId)
                            isLoading = false
                        } else {
                            transactionToEdit = null
                            isLoading = false
                        }
                    }

                    // Mostra la schermata solo quando non è in caricamento
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AddTransactionScreen(
                            ccDelay = ccDelay,
                            currencySymbol = currency,
                            onGetSuggestions = { q -> viewModel.getSuggestions(q) },
                            onSave = { newTransaction ->
                                viewModel.addTransaction(newTransaction)
                                navController.popBackStack()
                            },
                            transactionToEdit = transactionToEdit // Passa la transazione per precompilare
                        )
                    }
                }

                // SCHERMATA 3: IMPOSTAZIONI
                composable("settings") {
                    SettingsScreen(
                        currentCurrency = currency,
                        ccDelay = ccDelay,
                        ccLimit = ccLimit,
                        onCurrencyChange = { viewModel.updateCurrency(it) },
                        onDelayChange = { viewModel.updateCcDelay(it) },
                        onLimitChange = { viewModel.updateCcLimit(it) },
                        onBackup = { createDocumentLauncher.launch("backup_spese_${LocalDate.now()}.json") },
                        onRestore = { openDocumentLauncher.launch(arrayOf("application/json")) },
                        onExportCsv = { createCsvDocumentLauncher.launch("report_spese_${LocalDate.now()}.csv") } // NUOVO
                    )
                }
            }
        }
    }
}

// --- FUNZIONI DI UTILITÀ PER BACKUP/RESTORE/EXPORT ---

fun generateCsvContent(expenses: List<TransactionEntity>): String {
    // Intestazione con i campi richiesti
    val header = "Data Movimento,Data Addebito,Descrizione,Importo,Categoria,Tipo Pagamento\n"
    val csv = StringBuilder(header)

    // Dettaglio Movimenti
    expenses.sortedByDescending { it.date }.forEach { t ->
        val line = listOf(
            t.date,
            t.effectiveDate,
            "\"${t.description.replace("\"", "\"\"")}\"", // Escapes quotes for descriptions with commas
            // Usa Locale.US per il punto decimale, standard nei CSV
            String.format(Locale.US, "%.2f", t.amount),
            t.categoryId,
            if(t.isCreditCard) "Carta di Credito" else "Contanti/Addebito Immediato"
        ).joinToString(",")
        csv.append(line).append("\n")
    }

    // Calcolo e Aggiunta Somme Mensili
    if (expenses.isNotEmpty()) {
        val monthlySums = expenses
            .groupBy { it.effectiveDate.substring(0, 7) } // Raggruppa per Anno-Mese (data di addebito effettiva)
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toSortedMap(compareByDescending { it }) // Ordina per mese decrescente

        csv.append("\n\n")
        csv.append("--- SOMMARIO MENSILE PER ADDEBITO EFFETTIVO ---\n")
        csv.append("Mese (AAAA-MM),Totale Spese\n")

        monthlySums.forEach { (month, total) ->
            val totalStr = String.format(Locale.US, "%.2f", total)
            csv.append("$month,${totalStr}\n")
        }
    }

    return csv.toString()
}

fun performExportCsv(context: Context, viewModel: ExpenseViewModel, uri: Uri) {
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    scope.launch {
        try {
            // 1. Ottieni i dati delle sole spese
            val expenses = viewModel.getExpensesForExport()

            // 2. Genera il contenuto CSV
            val csvContent = generateCsvContent(expenses)

            // 3. Scrivi nel file
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(csvContent.toByteArray())
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Esportazione CSV completata con successo!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore durante l'esportazione CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun performBackup(context: Context, viewModel: ExpenseViewModel, uri: Uri) {
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    scope.launch {
        // Chiede i dati al ViewModel
        val list = viewModel.getAllForBackup()
        val json = Gson().toJson(list)
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray())
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Backup salvato con successo!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore durante il backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun performRestore(context: Context, viewModel: ExpenseViewModel, uri: Uri) {
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    scope.launch {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { sb.append(it) }
                }
            }
            // Converte il JSON in lista
            val type = object : TypeToken<List<TransactionEntity>>() {}.type
            val list: List<TransactionEntity> = Gson().fromJson(sb.toString(), type)

            // Passa i dati al ViewModel per salvarli nel DB
            viewModel.restoreData(list)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ripristinati ${list.size} movimenti!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore Ripristino: File non valido o corrotto", Toast.LENGTH_LONG).show()
            }
        }
    }
}