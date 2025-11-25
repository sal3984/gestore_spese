package com.alessandrogregorio.gestorespese

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
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
import com.alessandrogregorio.gestorespese.ui.screens.ReportScreen
import com.alessandrogregorio.gestorespese.ui.screens.SettingsScreen
import com.alessandrogregorio.gestorespese.ui.theme.GestoreSpeseTheme
import com.alessandrogregorio.gestorespese.viewmodel.ExpenseViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestoreSpeseTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: ExpenseViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Recupero gli stati dal ViewModel
    val allTransactions by viewModel.allTransactions.collectAsState()
    val currentCurrency by viewModel.currency.collectAsState()
    val currentCcLimit by viewModel.ccLimit.collectAsState()
    val currentCcDelay by viewModel.ccDelay.collectAsState()
    val currentDateFormat by viewModel.dateFormat.collectAsState()
    val earliestMonth by viewModel.earliestMonth.collectAsState() // NUOVO: Mese più vecchio

    // --- Activity Result Launchers per Backup/Restore/Export ---

    // Launcher per il ripristino
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { performRestore(context, viewModel, it) }
    }

    // Launcher per il backup
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { performBackup(context, viewModel, it) }
    }

    // Launcher per l'esportazione CSV
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                performCsvExport(
                    context = context,
                    viewModel = viewModel,
                    uri = it,
                    currencySymbol = currentCurrency,
                    dateFormat = currentDateFormat
                )
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Dashboard
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") }
                )
                // Report (Statistiche)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Download, contentDescription = "Report") },
                    label = { Text("Report") },
                    selected = currentRoute == "report",
                    onClick = { navController.navigate("report") }
                )
                // Impostazioni
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
                    label = { Text("Impostazioni") },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") }
                )
            }
        },
        floatingActionButton = {
            if (navController.currentBackStackEntryAsState().value?.destination?.route?.startsWith("add_transaction") != true) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_transaction/0") },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, "Aggiungi Transazione")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(navController, startDestination = "dashboard") {
                composable(
                    "dashboard",
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    DashboardScreen(
                        transactions = allTransactions,
                        currencySymbol = currentCurrency,
                        ccLimit = currentCcLimit,
                        dateFormat = currentDateFormat,
                        earliestMonth = earliestMonth, // PASSATO IL MESE PIÙ VECCHIO
                        onDelete = viewModel::deleteTransaction,
                        onEdit = { transactionId ->
                            navController.navigate("add_transaction/$transactionId")
                        }
                    )
                }

                composable(
                    "report",
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    ReportScreen(
                        transactions = allTransactions,
                        currencySymbol = currentCurrency,
                        dateFormat = currentDateFormat
                    )
                }

                composable(
                    "settings",
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    SettingsScreen(
                        currentCurrency = currentCurrency,
                        currentDateFormat = currentDateFormat,
                        ccDelay = currentCcDelay,
                        ccLimit = currentCcLimit,
                        onCurrencyChange = viewModel::updateCurrency,
                        onDateFormatChange = viewModel::updateDateFormat,
                        onDelayChange = viewModel::updateCcDelay,
                        onLimitChange = viewModel::updateCcLimit,
                        onBackup = { backupLauncher.launch("gestore_spese_backup_${LocalDate.now()}.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                        onExportCsv = { exportCsvLauncher.launch("gestore_spese_spese_${LocalDate.now()}.csv") }
                    )
                }

                composable(
                    route = "add_transaction/{transactionId}",
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(300)
                        )
                    }
                ) { backStackEntry ->
                    val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                    var transactionToEdit: TransactionEntity? by remember { mutableStateOf(null) }
                    var isLoading by remember { mutableStateOf(transactionId != 0L) }

                    LaunchedEffect(transactionId) {
                        if (transactionId != 0L) {
                            transactionToEdit = viewModel.getTransactionById(transactionId)
                            isLoading = false
                        } else {
                            isLoading = false
                        }
                    }

                    if (isLoading && transactionId != 0L) {
                        // Mostra un loading se necessario
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AddTransactionScreen(
                            ccDelay = currentCcDelay,
                            currencySymbol = currentCurrency,
                            onGetSuggestions = viewModel::getSuggestions,
                            dateFormatString = currentDateFormat,
                            onSave = { transaction ->
                                viewModel.saveTransaction(transaction)
                                navController.popBackStack() // Torna indietro dopo il salvataggio
                            },
                            transactionToEdit = transactionToEdit
                        )
                    }
                }
            }
        }
    }
}

// --- Funzioni di gestione file (Backup/Restore/Export) ---

// Funzione di esportazione CSV (AGGIORNATA per usare il formato data)
suspend fun performCsvExport(context: Context, viewModel: ExpenseViewModel, uri: Uri, currencySymbol: String, dateFormat: String) {
    val formatter = DateTimeFormatter.ofPattern(dateFormat)
    val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    coroutineScope.launch {
        try {
            val expenses = viewModel.getExpensesForExport()
            val csvHeader = "ID,Data,Descrizione,Importo (${currencySymbol}),Categoria,Carta di Credito,Data Addebito\n"
            val csvContent = StringBuilder(csvHeader)

            expenses.forEach { t ->
                // Formatta la data in base alla preferenza dell'utente
                val dateStr = try {
                    LocalDate.parse(t.date).format(formatter)
                } catch (e: Exception) {
                    t.date // Fallback se la conversione fallisce
                }
                val effectiveDateStr = try {
                    LocalDate.parse(t.effectiveDate).format(formatter)
                } catch (e: Exception) {
                    t.effectiveDate // Fallback
                }

                csvContent.append("${t.id},\"$dateStr\",\"${t.description.replace('\"', '\'')}\",${String.format(Locale.US, "%.2f", t.amount)},${t.categoryId},${if (t.isCreditCard) "Sì" else "No"},\"$effectiveDateStr\"\n")
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(csvContent.toString())
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Esportate ${expenses.size} spese in CSV!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore durante l'esportazione: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Funzione di Backup (JSON)
fun performBackup(context: Context, viewModel: ExpenseViewModel, uri: Uri) {
    val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    coroutineScope.launch {
        try {
            val allData = viewModel.getAllForBackup()
            val json = Gson().toJson(allData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
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

// Funzione di Ripristino (JSON)
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
                Toast.makeText(context, "Ripristinati ${list.size} movimenti! I dati appariranno a breve.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore Ripristino: File non valido o corrotto", Toast.LENGTH_LONG).show()
            }
        }
    }
}
