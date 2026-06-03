package com.expense.management

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expense.management.data.TransactionType
import com.expense.management.ui.navigation.AppBottomBar
import com.expense.management.ui.navigation.AppNavHost
import com.expense.management.ui.navigation.AppTopBar
import com.expense.management.ui.navigation.BiometricGate
import com.expense.management.ui.theme.AppTheme
import com.expense.management.utils.BackupUtils
import com.expense.management.viewmodel.CreditCardViewModel
import com.expense.management.viewmodel.CreditCardViewModelFactory
import com.expense.management.viewmodel.ExpenseViewModel
import com.expense.management.viewmodel.ExpenseViewModelFactory
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
    val currentAppStyle by viewModel.appStyle.collectAsStateWithLifecycle()
    AppTheme(appStyle = currentAppStyle, darkTheme = isDarkTheme) {
        mainAppContent(viewModel, creditCardViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainAppContent(viewModel: ExpenseViewModel, creditCardViewModel: CreditCardViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val reportTransactions by viewModel.reportTransactions.collectAsStateWithLifecycle()
    val reportData by viewModel.reportData.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val currentCurrency by viewModel.currency.collectAsStateWithLifecycle()
    val currentCcPaymentMode by viewModel.ccPaymentMode.collectAsStateWithLifecycle()
    val currentDateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentAppStyle by viewModel.appStyle.collectAsStateWithLifecycle()
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
    val receiptScanResult by viewModel.receiptScanResult.collectAsStateWithLifecycle()
    val enabledWidgets by viewModel.enabledWidgets.collectAsStateWithLifecycle()
    val defaultPaymentMethodId by viewModel.defaultPaymentMethodId.collectAsStateWithLifecycle()
    val dashboardFilteredTransactions by viewModel.dashboardFilteredTransactions.collectAsStateWithLifecycle()
    val creditCardSummaries by viewModel.creditCardSummaries.collectAsStateWithLifecycle()
    val hasTransactions = allTransactions.isNotEmpty()

    LaunchedEffect(currentDashboardMonth) {
        viewModel.refreshBnplProjections(currentDashboardMonth)
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

    val bottomNavRoutes = listOf("dashboard", "report", "categories", "settings")

    val isBottomBarVisible = currentRoute in bottomNavRoutes
    val isTopBarVisible = currentRoute?.let { route ->
        !route.startsWith("add_transaction") && !route.startsWith("add_credit_card_transaction")
    } ?: true

    BiometricGate(
        isBiometricEnabled = isBiometricEnabled,
        isAuthenticated = isAuthenticated,
        onUnlock = viewModel::unlockApp,
    ) {
        var showAddMenu by remember { mutableStateOf(false) }
        Scaffold(
            topBar = {
                if (isTopBarVisible) {
                    AppTopBar(
                        currentRoute = currentRoute,
                        isAmountHidden = isAmountHidden,
                        onToggleAmountHidden = viewModel::toggleAmountHidden,
                    )
                }
            },
            bottomBar = {
                if (isBottomBarVisible) {
                    AppBottomBar(
                        navController = navController,
                        currentRoute = currentRoute,
                    )
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
                    AppNavHost(
                        navController = navController,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        viewModel = viewModel,
                        creditCardViewModel = creditCardViewModel,
                        allTransactions = allTransactions,
                        reportTransactions = reportTransactions,
                        reportData = reportData,
                        allCategories = allCategories,
                        currentCurrency = currentCurrency,
                        currentDateFormat = currentDateFormat,
                        currentCcPaymentMode = currentCcPaymentMode,
                        earliestMonth = earliestMonth,
                        currentDashboardMonth = currentDashboardMonth,
                        isAmountHidden = isAmountHidden,
                        activeCreditCards = activeCreditCards,
                        allPaymentMethods = allPaymentMethods,
                        bnplProjections = bnplProjections,
                        csvExportColumns = csvExportColumns,
                        suggestions = suggestions,
                        allCreditCards = allCreditCards,
                        frequentExpenseCategories = frequentExpenseCategories,
                        frequentIncomeCategories = frequentIncomeCategories,
                        currencyRates = currencyRates,
                        lastRatesUpdate = lastRatesUpdate,
                        currentThemeMode = currentThemeMode,
                        currentAppStyle = currentAppStyle,
                        hasTransactions = hasTransactions,
                        isBiometricEnabled = isBiometricEnabled,
                        enabledWidgets = enabledWidgets,
                        receiptScanResult = receiptScanResult,
                        onClearReceiptScanResult = viewModel::clearReceiptScanResult,
                        defaultPaymentMethodId = defaultPaymentMethodId,
                        dashboardFilteredTransactions = dashboardFilteredTransactions,
                        creditCardSummaries = creditCardSummaries,
                        onBackup = { backupLauncher.launch("gestore_spese_backup_${LocalDate.now()}.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                        onExportCsv = { exportCsvLauncher.launch("gestore_spese_spese_${LocalDate.now()}.csv") },
                        onNavigateToDataManagement = { navController.navigate("data_management") },
                        onNavigateToSecurity = { navController.navigate("security") },
                        onNavigateToPaymentMethods = { navController.navigate("payment_methods") },
                    )
                }
            }
        }
    }
}
