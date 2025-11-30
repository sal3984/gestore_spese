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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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

// Modifica: MainActivity ora estende FragmentActivity per supportare BiometricPrompt
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Recupero gli stati dal ViewModel
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState() // CATEGORIE DAL DB
    val currentCurrency by viewModel.currency.collectAsState()
    val currentCcLimit by viewModel.ccLimit.collectAsState()
    val currentCcDelay by viewModel.ccDelay.collectAsState()
    val currentCcPaymentMode by viewModel.ccPaymentMode.collectAsState() // NUOVO STATO
    val currentDateFormat by viewModel.dateFormat.collectAsState()
    val earliestMonth by viewModel.earliestMonth.collectAsState()
    val currentDashboardMonth by viewModel.currentDashboardMonth.collectAsState() // RECUPERO LO STATO DEL MESE DASHBOARD
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
                 onError = { /* Gestisci errore o chiudi app */ }
             )
        } else {
            viewModel.isAppUnlocked.value = true
        }
    }

    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("App Bloccata", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Autenticati per accedere ai tuoi dati", style = MaterialTheme.typography.bodyLarge)

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

    // Definizione delle rotte principali
    val bottomNavRoutes = listOf("dashboard", "report")
    // drawerRoutes ora include anche security
    val drawerRoutes = listOf("categories", "settings", "data_management", "security")
    val isBottomBarVisible = currentRoute in bottomNavRoutes
    // Mostra TopBar con menu in tutte le schermate principali
    val isTopBarVisible = isBottomBarVisible || currentRoute in drawerRoutes

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Menu Laterale: Home, Categorie e Impostazioni
                NavigationDrawerItem(
                    label = { Text("Dashboard") }, // Rinominato da Home a Dashboard per coerenza
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.categories_title)) },
                    selected = currentRoute == "categories",
                    onClick = {
                        navController.navigate("categories")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.data_management)) },
                    selected = currentRoute == "data_management",
                    onClick = {
                        navController.navigate("data_management")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.security_usability)) },
                    selected = currentRoute == "security",
                    onClick = {
                        navController.navigate("security")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.exit)) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        (context as? Activity)?.finish()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Esci") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isTopBarVisible) {
                    CenterAlignedTopAppBar(
                        title = {
                            val title = when (currentRoute) {
                                "dashboard" -> "Dashboard"
                                "report" -> "Report"
                                "categories" -> stringResource(R.string.categories_title)
                                "settings" -> stringResource(R.string.settings)
                                "data_management" -> stringResource(R.string.data_management)
                                "security" -> stringResource(R.string.security_usability)
                                else -> stringResource(R.string.app_name)
                            }
                            Text(title)
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            bottomBar = {
                // Barra Inferiore: Dashboard e Report (Solo quando visibile)
                if (isBottomBarVisible) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            selected = currentRoute == "dashboard",
                            onClick = {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Download, contentDescription = "Report") },
                            label = { Text("Report") },
                            selected = currentRoute == "report",
                            onClick = {
                                navController.navigate("report") {
                                    popUpTo("dashboard") { saveState = true }
                                    restoreState = true
                                    launchSingleTop = true
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
                            categories = allCategories, // PASSATA
                            currencySymbol = currentCurrency,
                            ccLimit = currentCcLimit,
                            dateFormat = currentDateFormat,
                            earliestMonth = earliestMonth,
                            currentDashboardMonth = currentDashboardMonth, // PASSATO LO STATO AL SCREEN
                            onMonthChange = viewModel::updateDashboardMonth, // CALLBACK PER AGGIORNARE LO STATO
                            onDelete = viewModel::deleteTransaction, // DELETE DEVE ACCETTARE STRING
                            onEdit = { transactionId -> // transactionId è ora String
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
                            categories = allCategories, // PASSATA
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
                            // onBack RIMOSSO
                        )
                    }

                    composable(
                        "data_management",
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        DataManagementScreen(
                            onBackup = { backupLauncher.launch("gestore_spese_backup_${LocalDate.now()}.json") },
                            onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                            onExportCsv = { exportCsvLauncher.launch("gestore_spese_spese_${LocalDate.now()}.csv") }
                        )
                    }

                    composable(
                        "security",
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        SecurityScreen(
                            isAmountHidden = isAmountHidden,
                            isBiometricEnabled = isBiometricEnabled,
                            onAmountHiddenChange = viewModel::updateIsAmountHidden,
                            onBiometricEnabledChange = { isEnabled ->
                                // Se si tenta di abilitare, chiedi conferma biometrica
                                if (isEnabled) {
                                    BiometricUtils.authenticateUser(context,
                                        onSuccess = { viewModel.updateBiometricEnabled(true) },
                                        onError = { }
                                    )
                                } else {
                                    viewModel.updateBiometricEnabled(false)
                                }
                            }
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
                            onCcPaymentModeChange = viewModel::updateCcPaymentMode
                        )
                    }

                    composable(
                        route = "add_transaction/{transactionId}",
                        // AGGIORNATO: L'ID è ora NavType.StringType e il default è "0"
                        arguments = listOf(navArgument("transactionId") { type = NavType.StringType; defaultValue = "0" }),
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
                        // ID è Stringa (UUID)
                        val transactionId = backStackEntry.arguments?.getString("transactionId") ?: "0"
                        var transactionToEdit: TransactionEntity? by remember { mutableStateOf(null) }
                        // Condizione per caricamento: se ID è diverso dal placeholder "0"
                        var isLoading by remember { mutableStateOf(transactionId != "0") }

                        LaunchedEffect(transactionId) {
                            if (transactionId != "0") {
                                // CHIAMATA AL VIEWMODEL: getTransactionById DEVE accettare String
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
                                // Aggiunto argomento suggestions (vuoto per ora) e rimosso onGetSuggestions non necessario
                                suggestions = suggestions,
                                dateFormat = currentDateFormat, // Nome argomento corretto
                                onSave = { transaction ->
                                    viewModel.saveTransaction(transaction)
                                    // RIMOSSO popBackStack() qui perché AddTransactionScreen chiama onBack() alla fine di trySave
                                },
                                onDelete = { id -> // ID è Stringa
                                    viewModel.deleteTransaction(id)
                                    navController.popBackStack() // Qui va bene perché AddTransactionScreen non chiama onBack dopo onDelete
                                },
                                transactionToEdit = transactionToEdit,
                                onBack = { navController.popBackStack() },
                                availableCategories = allCategories,
                                onDescriptionChange = { query ->
                                    viewModel.searchDescriptionSuggestions(query) // Chiama la funzione del ViewModel
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
