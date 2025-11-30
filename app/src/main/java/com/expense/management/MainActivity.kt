package com.expense.management

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expense.management.data.TransactionEntity
import com.expense.management.ui.screens.AddTransactionScreen
import com.expense.management.ui.screens.DashboardScreen
import com.expense.management.ui.screens.DataManagementScreen
import com.expense.management.ui.screens.ReportScreen
import com.expense.management.ui.screens.SecurityScreen
import com.expense.management.ui.screens.SettingsScreen
import com.expense.management.ui.screens.category.CategoryScreen
import com.expense.management.ui.theme.GestoreSpeseTheme
import com.expense.management.utils.BackupUtils
import com.expense.management.utils.BiometricUtils
import com.expense.management.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val allCategories by viewModel.allCategories.collectAsState()
    val currentCurrency by viewModel.currency.collectAsState()
    val currentCcLimit by viewModel.ccLimit.collectAsState()
    val currentCcDelay by viewModel.ccDelay.collectAsState()
    val currentCcPaymentMode by viewModel.ccPaymentMode.collectAsState()
    val currentDateFormat by viewModel.dateFormat.collectAsState()
    val earliestMonth by viewModel.earliestMonth.collectAsState()
    val currentDashboardMonth by viewModel.currentDashboardMonth.collectAsState()
    val isAmountHidden by viewModel.isAmountHidden.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    // Stato per gestire l'autenticazione
    var isAuthenticated by remember { viewModel.isAppUnlocked }

    // Effetto per avviare l'autenticazione biometrica se abilitata
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && !isAuthenticated) {
             BiometricUtils.authenticateUser(context,
                 onSuccess = { viewModel.isAppUnlocked.value = true },
                 onError = { /* Nessun Toast qui, lo gestisce BiometricUtils */ }
             )
        } else {
            viewModel.isAppUnlocked.value = true
        }
    }

    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.biometric_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.biometric_subtitle), style = MaterialTheme.typography.bodyLarge)

                Button(onClick = {
                     BiometricUtils.authenticateUser(context,
                         onSuccess = { viewModel.isAppUnlocked.value = true},
                         onError = { }
                     )
                }, modifier = Modifier.padding(top = 24.dp)) {
                    Text(stringResource(R.string.unlock))
                }
            }
        }
        return
    }

    // --- Activity Result Launchers per Backup/Restore/Export ---

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { BackupUtils.performRestore(context, viewModel, it) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { BackupUtils.performBackup(context, viewModel, it) }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                BackupUtils.performCsvExport(
                    context = context,
                    viewModel = viewModel,
                    uri = it,
                    currencySymbol = currentCurrency,
                    dateFormat = currentDateFormat
                )
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Definizione delle rotte principali per la Bottom Bar
    val bottomNavRoutes = listOf("dashboard", "report", "categories", "settings")

    // La Bottom Bar è visibile solo nelle schermate principali
    val isBottomBarVisible = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Download, contentDescription = "Report") },
                        label = { Text("Report") },
                        selected = currentRoute == "report",
                        onClick = {
                            navController.navigate("report") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Category, contentDescription = stringResource(R.string.categories_title)) },
                        label = { Text(stringResource(R.string.categories_title)) },
                        selected = currentRoute == "categories",
                        onClick = {
                            navController.navigate("categories") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) },
                        label = { Text(stringResource(R.string.settings)) },
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "dashboard") {
                FloatingActionButton(
                    onClick = { navController.navigate("add_transaction/0") },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.add_transaction))
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            NavHost(navController, startDestination = "dashboard") {
                composable(
                    "dashboard",
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    DashboardScreen(
                        transactions = allTransactions,
                        categories = allCategories,
                        currencySymbol = currentCurrency,
                        ccLimit = currentCcLimit,
                        dateFormat = currentDateFormat,
                        earliestMonth = earliestMonth,
                        currentDashboardMonth = currentDashboardMonth,
                        onMonthChange = viewModel::updateDashboardMonth,
                        onDelete = viewModel::deleteTransaction,
                        onEdit = { transactionId ->
                            navController.navigate("add_transaction/$transactionId")
                        },
                        isAmountHidden = isAmountHidden
                    )
                }

                composable(
                    "report",
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    ReportScreen(
                        transactions = allTransactions,
                        categories = allCategories,
                        currencySymbol = currentCurrency,
                        dateFormat = currentDateFormat,
                        isAmountHidden = isAmountHidden
                    )
                }

                composable(
                    "categories",
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    CategoryScreen(
                        categories = allCategories,
                        onAddCategory = viewModel::addCategory,
                        onDeleteCategory = viewModel::removeCategory,
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
                        ccPaymentMode = currentCcPaymentMode,
                        onCurrencyChange = viewModel::updateCurrency,
                        onDateFormatChange = viewModel::updateDateFormat,
                        onDelayChange = viewModel::updateCcDelay,
                        onLimitChange = viewModel::updateCcLimit,
                        onCcPaymentModeChange = viewModel::updateCcPaymentMode,
                        onNavigateToSecurity = { navController.navigate("security") },
                        onNavigateToDataManagement = { navController.navigate("data_management") }
                    )
                }

                // Sottosezioni di Impostazioni (senza Bottom Bar)

                composable(
                    "data_management",
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
                    }
                ) {
                    DataManagementScreen(
                        onBackup = { backupLauncher.launch("gestore_spese_backup_${LocalDate.now()}.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                        onExportCsv = { exportCsvLauncher.launch("gestore_spese_spese_${LocalDate.now()}.csv") },
                        onBack = { navController.popBackStack() } // Aggiunto callback mancante
                    )
                }

                composable(
                    "security",
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
                    }
                ) {
                    SecurityScreen(
                        isAmountHidden = isAmountHidden,
                        isBiometricEnabled = isBiometricEnabled,
                        onAmountHiddenChange = viewModel::updateIsAmountHidden,
                        onBiometricEnabledChange = { isEnabled ->
                            if (isEnabled) {
                                BiometricUtils.authenticateUser(context,
                                    onSuccess = { viewModel.updateBiometricEnabled(true) },
                                    onError = { }
                                )
                            } else {
                                viewModel.updateBiometricEnabled(false)
                            }
                        },
                        onBack = { navController.popBackStack() } // Aggiunto callback mancante
                    )
                }

                composable(
                    route = "add_transaction/{transactionId}",
                    arguments = listOf(navArgument("transactionId") { type = NavType.StringType; defaultValue = "0" }),
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    val transactionId = backStackEntry.arguments?.getString("transactionId") ?: "0"
                    var transactionToEdit: TransactionEntity? by remember { mutableStateOf(null) }
                    var isLoading by remember { mutableStateOf(transactionId != "0") }

                    LaunchedEffect(transactionId) {
                        if (transactionId != "0") {
                            transactionToEdit = viewModel.getTransactionById(transactionId)
                            isLoading = false
                        } else {
                            isLoading = false
                        }
                    }

                    if (isLoading && transactionId != "0") {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AddTransactionScreen(
                            ccDelay = currentCcDelay,
                            currencySymbol = currentCurrency,
                            ccPaymentMode = currentCcPaymentMode,
                            suggestions = suggestions,
                            dateFormat = currentDateFormat,
                            onSave = { transaction ->
                                viewModel.saveTransaction(transaction)
                            },
                            onDelete = { id ->
                                viewModel.deleteTransaction(id)
                                navController.popBackStack()
                            },
                            transactionToEdit = transactionToEdit,
                            onBack = { navController.popBackStack() },
                            availableCategories = allCategories,
                            onDescriptionChange = { query ->
                                viewModel.searchDescriptionSuggestions(query)
                            }
                        )
                    }
                }
            }
        }
    }
}
