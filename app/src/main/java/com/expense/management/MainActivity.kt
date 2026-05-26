package com.expense.management

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.expense.management.data.TransactionType
import com.expense.management.ui.screens.AddCreditCardTransactionScreen
import com.expense.management.ui.screens.AddRegularTransactionScreen
import com.expense.management.ui.screens.DashboardScreen
import com.expense.management.ui.screens.DataManagementScreen
import com.expense.management.ui.screens.PaymentMethodSettingsScreen
import com.expense.management.ui.screens.ReportScreen
import com.expense.management.ui.screens.category.CategoryScreen
import com.expense.management.ui.screens.securityScreen
import com.expense.management.ui.screens.settingsScreen
import com.expense.management.ui.theme.gestoreSpeseTheme
import com.expense.management.utils.BackupUtils
import com.expense.management.utils.BiometricUtils
import com.expense.management.viewmodel.CreditCardViewModel
import com.expense.management.viewmodel.CreditCardViewModelFactory
import com.expense.management.viewmodel.ExpenseViewModel
import com.expense.management.viewmodel.ExpenseViewModelFactory
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            mainApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainApp() {
    val context = LocalContext.current
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(context))
    val creditCardViewModel: CreditCardViewModel = viewModel(factory = CreditCardViewModelFactory(context))
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    gestoreSpeseTheme(darkTheme = isDarkTheme) {
        mainAppContent(viewModel, creditCardViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainAppContent(viewModel: ExpenseViewModel, creditCardViewModel: CreditCardViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val reportTransactions by viewModel.reportTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val currentCurrency by viewModel.currency.collectAsStateWithLifecycle()
    val currentCcLimit by viewModel.ccLimit.collectAsStateWithLifecycle()
    val currentCcPaymentMode by viewModel.ccPaymentMode.collectAsStateWithLifecycle()
    val currentDateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val earliestMonth by viewModel.earliestMonth.collectAsStateWithLifecycle()
    val currentDashboardMonth by viewModel.currentDashboardMonth.collectAsStateWithLifecycle()
    val isAmountHidden by viewModel.isAmountHidden.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val csvExportColumns by viewModel.csvExportColumns.collectAsStateWithLifecycle()

    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val allCreditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val activeCreditCards by viewModel.activeCreditCards.collectAsStateWithLifecycle()
    val allPaymentMethods by viewModel.allPaymentMethods.collectAsStateWithLifecycle()
    val currencyRates by viewModel.currencyRates.collectAsStateWithLifecycle()
    val lastRatesUpdate by viewModel.currencyRatesUpdate.collectAsStateWithLifecycle()
    val frequentExpenseCategories by viewModel.getFrequentCategories(TransactionType.EXPENSE).collectAsStateWithLifecycle()
    val frequentIncomeCategories by viewModel.getFrequentCategories(TransactionType.INCOME).collectAsStateWithLifecycle()

    val isAuthenticated by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val bnplProjections by viewModel.bnplProjections.collectAsStateWithLifecycle()

    // Determina se ci sono transazioni
    val hasTransactions = allTransactions.isNotEmpty()

    var showAddMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && !isAuthenticated) {
            BiometricUtils.authenticateUser(
                context,
                onSuccess = { viewModel.unlockApp() },
                onError = { /* Handle error */ },
            )
        } else {
            viewModel.unlockApp()
        }
    }

    LaunchedEffect(currentDashboardMonth) {
        viewModel.refreshBnplProjections(currentDashboardMonth)
    }

    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_blocked), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.authenticate_to_access), style = MaterialTheme.typography.bodyLarge)

                Button(onClick = {
                    BiometricUtils.authenticateUser(
                        context,
                        onSuccess = { viewModel.unlockApp() },
                        onError = { },
                    )
                }, modifier = Modifier.padding(top = 24.dp)) {
                    Text(stringResource(R.string.unlock))
                }
            }
        }
        return
    }

    val restoreLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let { BackupUtils.performRestore(coroutineScope, context, viewModel, it) }
        }

    val backupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            uri?.let { BackupUtils.performBackup(coroutineScope, context, viewModel, it) }
        }

    val exportCsvLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri: Uri? ->
            uri?.let {
                BackupUtils.performCsvExport(
                    scope = coroutineScope,
                    context = context,
                    viewModel = viewModel,
                    uri = it,
                    currencySymbol = currentCurrency,
                    dateFormat = currentDateFormat,
                    selectedColumns = csvExportColumns,
                )
            }
        }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // AGGIORNAMENTO ROUTES: Categorie spostato nella BottomBar
    val bottomNavRoutes = listOf("dashboard", "report", "categories")
    // Drawer Routes mantiene solo le sezioni di configurazione/gestione
    val drawerRoutes = listOf("data_management", "security", "payment_methods", "settings")

    val isBottomBarVisible = currentRoute in bottomNavRoutes
    val isTopBarVisible = isBottomBarVisible || currentRoute in drawerRoutes

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                // Dashboard mantenuta nel Drawer come "Home"
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.dashboard)) },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                // Categorie rimosse dal Drawer (ora sono nella BottomBar)

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.data_management)) },
                    selected = currentRoute == "data_management",
                    onClick = {
                        navController.navigate("data_management")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.security_usability)) },
                    selected = currentRoute == "security",
                    onClick = {
                        navController.navigate("security")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.payment_methods)) },
                    selected = currentRoute == "payment_methods",
                    onClick = {
                        navController.navigate("payment_methods")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings")
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.exit)) },
                    selected = false,
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.exit)) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (isTopBarVisible) {
                    CenterAlignedTopAppBar(
                        title = {
                            val title =
                                when (currentRoute) {
                                    "dashboard" -> stringResource(R.string.dashboard)
                                    "report" -> stringResource(R.string.report)
                                    "categories" -> stringResource(R.string.categories_title)
                                    "settings" -> stringResource(R.string.settings)
                                    "data_management" -> stringResource(R.string.data_management)
                                    "security" -> stringResource(R.string.security_usability)
                                    "payment_methods" -> stringResource(R.string.payment_methods)
                                    else -> stringResource(R.string.app_name)
                                }
                            Text(
                                text = title,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu),
                                )
                            }
                        },
                        colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                }
            },
            bottomBar = {
                if (isBottomBarVisible) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.dashboard)) },
                            label = { Text(stringResource(R.string.dashboard)) },
                            selected = currentRoute == "dashboard",
                            onClick = {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            },
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Download, contentDescription = stringResource(R.string.report)) },
                            label = { Text(stringResource(R.string.report)) },
                            selected = currentRoute == "report",
                            onClick = {
                                navController.navigate("report") {
                                    popUpTo("dashboard") { saveState = true }
                                    restoreState = true
                                    launchSingleTop = true
                                }
                            },
                        )
                        // Aggiunto Categorie alla BottomBar
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Category, contentDescription = stringResource(R.string.categories_title)) },
                            label = { Text(stringResource(R.string.categories_title)) },
                            selected = currentRoute == "categories",
                            onClick = {
                                navController.navigate("categories") {
                                    popUpTo("dashboard") { saveState = true }
                                    restoreState = true
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == "dashboard") {
                    Box {
                        FloatingActionButton(
                            onClick = { showAddMenu = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_transaction),
                            )
                        }

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.transaction)) },
                                leadingIcon = {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                                },
                                onClick = {
                                    showAddMenu = false
                                    navController.navigate("add_transaction/0")
                                },
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.credit_card_transaction)) },
                                leadingIcon = {
                                    Icon(Icons.Default.CreditCard, contentDescription = null)
                                },
                                onClick = {
                                    showAddMenu = false
                                    navController.navigate("add_credit_card_transaction/0")
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                SharedTransitionLayout {
                    NavHost(navController, startDestination = "dashboard") {
                        composable(
                            "dashboard",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            DashboardScreen(
                                transactions = allTransactions,
                                categories = allCategories,
                                currencySymbol = currentCurrency,
                                dateFormat = currentDateFormat,
                                earliestMonth = earliestMonth,
                                currentDashboardMonth = currentDashboardMonth,
                                onMonthChange = viewModel::updateDashboardMonth,
                                onDelete = { transactionId, deleteType ->
                                    viewModel.deleteTransaction(transactionId, deleteType)
                                },
                                onEdit = { transactionId, isCreditCard ->
                                    if (isCreditCard) {
                                        navController.navigate("add_credit_card_transaction/$transactionId")
                                    } else {
                                        navController.navigate("add_transaction/$transactionId")
                                    }
                                },
                                isAmountHidden = isAmountHidden,
                                creditCards = activeCreditCards,
                                bnplProjections = bnplProjections,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                allPaymentMethods = allPaymentMethods,
                            )
                        }

                        composable(
                            "report",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            ReportScreen(
                                transactions = reportTransactions,
                                categories = allCategories,
                                currencySymbol = currentCurrency,
                                dateFormat = currentDateFormat,
                                isAmountHidden = isAmountHidden,
                                allPaymentMethods = allPaymentMethods,
                                onRangeChanged = { start, end ->
                                    viewModel.setReportRange(start, end)
                                },
                            )
                        }

                        composable(
                            "categories",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            CategoryScreen(
                                categories = allCategories,
                                onAddCategory = viewModel::addCategory,
                                onUpdateCategory = viewModel::updateCategory,
                                onDeleteCategory = viewModel::removeCategory,
                            )
                        }

                        composable(
                            "data_management",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            DataManagementScreen(
                                onBackup = { backupLauncher.launch("gestore_spese_backup_${LocalDate.now()}.json") },
                                onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                                onExportCsv = { exportCsvLauncher.launch("gestore_spese_spese_${LocalDate.now()}.csv") },
                            )
                        }

                        composable(
                            "security",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            securityScreen(
                                isAmountHidden = isAmountHidden,
                                isBiometricEnabled = isBiometricEnabled,
                                onAmountHiddenChange = viewModel::updateIsAmountHidden,
                                onBiometricEnabledChange = { isEnabled ->
                                    if (isEnabled) {
                                        BiometricUtils.authenticateUser(
                                            context,
                                            onSuccess = { viewModel.updateBiometricEnabled(true) },
                                            onError = { },
                                        )
                                    } else {
                                        viewModel.updateBiometricEnabled(false)
                                    }
                                },
                            )
                        }

                        composable(
                            "settings",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            settingsScreen(
                                currentCurrency = currentCurrency,
                                currentDateFormat = currentDateFormat,
                                currentThemeMode = currentThemeMode,
                                csvExportColumns = csvExportColumns,
                                hasTransactions = hasTransactions,
                                currencyRates = currencyRates,
                                lastRatesUpdate = lastRatesUpdate,
                                allCreditCards = allCreditCards,
                                onRefreshCurrencyRates = { viewModel.refreshCurrencyRates() },
                                onForceCurrencyRatesUpdate = { viewModel.forceCurrencyRatesUpdateSuspend() },
                                onAddCreditCard = { viewModel.addCreditCard(it) },
                                onUpdateCreditCard = { viewModel.updateCreditCard(it) },
                                onDeleteCreditCard = { viewModel.deleteCreditCard(it) },
                                onCurrencyChange = viewModel::updateCurrency,
                                onDateFormatChange = viewModel::updateDateFormat,
                                onCcPaymentModeChange = viewModel::updateCcPaymentMode,
                                onCsvExportColumnsChange = viewModel::updateCsvExportColumns,
                                onThemeModeChange = viewModel::updateThemeMode,
                            )
                        }

                        composable(
                            "payment_methods",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                        ) {
                            PaymentMethodSettingsScreen(
                                currentCurrency = currentCurrency,
                                allPaymentMethods = allPaymentMethods,
                                legacyCreditCards = allCreditCards,
                                onNavigateBack = { navController.popBackStack() },
                                onAdd = { method, closingDay, paymentDay, debitIssuer, debitCardNumber, debitNotes -> viewModel.addPaymentMethod(method, closingDay, paymentDay, debitIssuer, debitCardNumber, debitNotes) },
                                onDelete = { viewModel.deletePaymentMethod(it) },
                                onEditPaymentMethod = { method, details ->
                                    viewModel.updatePaymentMethodWithDetails(method, details)
                                },
                                onLoadDetails = { id ->
                                    viewModel.allPaymentMethods.value.find { it.id == id }?.let { method ->
                                        viewModel.getPaymentMethodDetails(method)
                                    }
                                },
                                onAddLegacyCard = { viewModel.addCreditCard(it) },
                                onUpdateLegacyCard = { viewModel.updateCreditCard(it) },
                                onDeleteLegacyCard = { viewModel.deleteCreditCard(it) },
                            )
                        }

                        composable(
                            route = "add_transaction/{transactionId}",
                            arguments = listOf(
                                navArgument("transactionId") {
                                    type = NavType.StringType
                                    defaultValue = "0"
                                },
                            ),
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = tween(300),
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = tween(300),
                                )
                            },
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
                                AddRegularTransactionScreen(
                                    currencySymbol = currentCurrency,
                                    suggestions = suggestions,
                                    dateFormat = currentDateFormat,
                                    onSave = { transaction ->
                                        viewModel.saveTransaction(transaction)
                                    },
                                    onDelete = { id, deleteType ->
                                        viewModel.deleteTransaction(id, deleteType)
                                        navController.popBackStack()
                                    },
                                    transactionToEdit = transactionToEdit,
                                    onBack = { navController.popBackStack() },
                                    availableCategories = allCategories,
                                    onDescriptionChange = { query ->
                                        viewModel.searchDescriptionSuggestions(query)
                                    },
                                    onConvertAmount = { from, to, amount ->
                                        viewModel.updateCurrencyRate(amount, from, to)
                                    },
                                    allPaymentMethods = allPaymentMethods,
                                    frequentExpenseCategories = frequentExpenseCategories,
                                    frequentIncomeCategories = frequentIncomeCategories,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                )
                            }
                        }

                        composable(
                            route = "add_credit_card_transaction/{transactionId}",
                            arguments = listOf(
                                navArgument("transactionId") {
                                    type = NavType.StringType
                                    defaultValue = "0"
                                },
                            ),
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = tween(300),
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = tween(300),
                                )
                            },
                        ) { backStackEntry ->
                            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: "0"

                            var transactionToEdit: TransactionEntity? by remember { mutableStateOf(null) }
                            var isLoading by remember { mutableStateOf(transactionId != "0") }

                            LaunchedEffect(transactionId) {
                                if (transactionId != "0") {
                                    transactionToEdit = creditCardViewModel.getTransactionById(transactionId)
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
                                AddCreditCardTransactionScreen(
                                    currencySymbol = currentCurrency,
                                    ccPaymentMode = currentCcPaymentMode,
                                    suggestions = suggestions,
                                    dateFormat = currentDateFormat,
                                    onSave = { transaction ->
                                        creditCardViewModel.saveTransaction(transaction)
                                    },
                                    onDelete = { id, deleteType ->
                                        creditCardViewModel.deleteTransaction(id, deleteType)
                                        navController.popBackStack()
                                    },
                                    transactionToEdit = transactionToEdit,
                                    onBack = { navController.popBackStack() },
                                    availableCategories = allCategories,
                                    onDescriptionChange = { query ->
                                        viewModel.searchDescriptionSuggestions(query)
                                    },
                                    onConvertAmount = { from, to, amount ->
                                        creditCardViewModel.updateCurrencyRate(amount, from, to)
                                    },
                                    activeCreditCards = activeCreditCards,
                                    allPaymentMethods = allPaymentMethods,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    frequentExpenseCategories = frequentExpenseCategories,
                                    frequentIncomeCategories = frequentIncomeCategories,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
