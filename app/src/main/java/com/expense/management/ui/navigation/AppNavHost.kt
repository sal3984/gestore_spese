package com.expense.management.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.BnplProjection
import com.expense.management.ui.screens.AddCreditCardTransactionScreen
import com.expense.management.ui.screens.AddRegularTransactionScreen
import com.expense.management.ui.screens.DashboardScreen
import com.expense.management.ui.screens.DataManagementScreen
import com.expense.management.ui.screens.PaymentMethodSettingsScreen
import com.expense.management.ui.screens.ReportScreen
import com.expense.management.ui.screens.category.CategoryScreen
import com.expense.management.ui.screens.securityScreen
import com.expense.management.ui.screens.settingsScreen
import com.expense.management.ui.theme.AppStyle
import com.expense.management.utils.BiometricUtils
import com.expense.management.viewmodel.CreditCardViewModel
import com.expense.management.viewmodel.ExpenseViewModel
import java.time.YearMonth

@Composable
fun AppNavHost(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    viewModel: ExpenseViewModel,
    creditCardViewModel: CreditCardViewModel,
    allTransactions: List<TransactionEntity>,
    reportTransactions: List<TransactionEntity>,
    allCategories: List<CategoryEntity>,
    currentCurrency: String,
    currentDateFormat: String,
    currentCcPaymentMode: String,
    earliestMonth: YearMonth,
    currentDashboardMonth: YearMonth,
    isAmountHidden: Boolean,
    activeCreditCards: List<ActiveCreditCard>,
    allPaymentMethods: List<PaymentMethodEntity>,
    bnplProjections: List<BnplProjection>,
    csvExportColumns: Set<String>,
    suggestions: List<String>,
    allCreditCards: List<CreditCardEntity>,
    frequentExpenseCategories: List<CategoryEntity>,
    frequentIncomeCategories: List<CategoryEntity>,
    currencyRates: List<CurrencyRate>,
    lastRatesUpdate: Long?,
    currentThemeMode: String,
    currentAppStyle: AppStyle,
    hasTransactions: Boolean,
    isBiometricEnabled: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit,
) {
    val context = LocalContext.current

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
                sharedTransitionScope = sharedTransitionScope,
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
                onBackup = onBackup,
                onRestore = onRestore,
                onExportCsv = onExportCsv,
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
                            context = context,
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
                currentAppStyle = currentAppStyle,
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
                onAppStyleChange = viewModel::updateAppStyle,
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
                onAdd = { method, closingDay, paymentDay, debitIssuer, debitCardNumber, debitNotes ->
                    viewModel.addPaymentMethod(method, closingDay, paymentDay, debitIssuer, debitCardNumber, debitNotes)
                },
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
                    sharedTransitionScope = sharedTransitionScope,
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
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@composable,
                    frequentExpenseCategories = frequentExpenseCategories,
                    frequentIncomeCategories = frequentIncomeCategories,
                )
            }
        }
    }
}
