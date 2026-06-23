package com.expense.management.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.CreditCardInstallmentPlanEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.KlarnaDetailEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.PaypalDetailEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.data.toData
import com.expense.management.data.toDomain
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.BnplProjection
import com.expense.management.domain.model.CreditCardSummary
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.domain.model.SatispayStatus
import com.expense.management.domain.repository.PaymentMethodRepository
import com.expense.management.domain.usecase.CalculateBnplProjectionsUseCase
import com.expense.management.domain.usecase.CalculateSatispayStatusUseCase
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GenerateCreditCardPaymentUseCase
import com.expense.management.domain.usecase.GetActiveCreditCardsUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetPaymentMethodsUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.ManagePaymentMethodUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodsViewModel(
    private val repository: ExpenseRepository,
    private val prefs: SharedPreferences?,
    private val paymentMethodRepo: PaymentMethodRepository,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val managePaymentMethodUseCase: ManagePaymentMethodUseCase,
    private val getActiveCreditCardsUseCase: GetActiveCreditCardsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val calculateBnplProjectionsUseCase: CalculateBnplProjectionsUseCase,
    private val calculateSatispayStatusUseCase: CalculateSatispayStatusUseCase,
    private val generateCreditCardPaymentUseCase: GenerateCreditCardPaymentUseCase,
) : ViewModel() {

    companion object {
        private const val KEY_DEFAULT_PAYMENT_METHOD = "default_payment_method_id"
    }

    val allTransactions: StateFlow<List<TransactionEntity>> =
        getTransactionsUseCase()
            .map { it.map { transaction -> transaction.toData() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaymentMethods: StateFlow<List<PaymentMethodEntity>> =
        getPaymentMethodsUseCase()
            .map { methods ->
                val cash = PaymentMethodEntity(
                    id = "__cash__",
                    name = "Contante",
                    provider = PaymentProvider.CASH,
                    isActive = true,
                )
                methods.map { it.toData() } + cash
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaypalDetails: StateFlow<List<PaypalDetailEntity>> =
        repository.allPaypalDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKlarnaDetails: StateFlow<List<KlarnaDetailEntity>> =
        repository.allKlarnaDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInstallmentPlans: StateFlow<List<CreditCardInstallmentPlanEntity>> =
        repository.allInstallmentPlans
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScheduledPayments: StateFlow<List<InstallmentScheduledPaymentEntity>> =
        repository.allScheduledPayments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allAmexStatements: StateFlow<List<AmexStatementEntity>> =
        repository.allAmexStatements
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allAmexPagoFlexPlans: StateFlow<List<AmexPagoFlexPlanEntity>> =
        repository.allAmexPagoFlexPlans
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allAmexRevolvingStates: StateFlow<List<AmexRevolvingStateEntity>> =
        repository.allAmexRevolvingStates
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allAmexScheduledPayments: StateFlow<List<AmexPagoFlexScheduledPaymentEntity>> =
        repository.allAmexScheduledPayments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCreditCards: StateFlow<List<ActiveCreditCard>> =
        getActiveCreditCardsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDashboardMonth = MutableStateFlow(YearMonth.now())
    val currentDashboardMonth: StateFlow<YearMonth> = _currentDashboardMonth.asStateFlow()

    fun updateDashboardMonth(month: YearMonth) {
        _currentDashboardMonth.value = month
    }

    private val _defaultPaymentMethodId = MutableStateFlow(
        prefs?.getString(KEY_DEFAULT_PAYMENT_METHOD, "__cash__") ?: "__cash__",
    )
    val defaultPaymentMethodId: StateFlow<String?> = _defaultPaymentMethodId.asStateFlow()

    private val amexPendingByCard: StateFlow<Map<String, Double>> = combine(
        allAmexStatements,
        allAmexPagoFlexPlans,
        allAmexScheduledPayments,
    ) { statements, plans, payments ->
        val statementIdsByCard = statements
            .groupBy { it.paymentMethodId }
            .mapValues { it.value.map { s -> s.id }.toSet() }
        statementIdsByCard.mapValues { (_, statementIds) ->
            plans
                .filter { it.statementId in statementIds }
                .flatMap { plan -> payments.filter { it.planId == plan.id && it.status == "PENDING" } }
                .sumOf { it.amount }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val amexPaidByCard: StateFlow<Map<String, Double>> = combine(
        allAmexStatements,
        allAmexPagoFlexPlans,
        allAmexScheduledPayments,
    ) { statements, plans, payments ->
        val statementIdsByCard = statements
            .groupBy { it.paymentMethodId }
            .mapValues { it.value.map { s -> s.id }.toSet() }
        statementIdsByCard.mapValues { (_, statementIds) ->
            plans
                .filter { it.statementId in statementIds }
                .flatMap { plan -> payments.filter { it.planId == plan.id && it.status == "PAID" } }
                .sumOf { it.amount }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val creditCardSummaries: StateFlow<Map<String, CreditCardSummary>> = combine(
        allTransactions,
        activeCreditCards,
        _currentDashboardMonth,
        amexPendingByCard,
        amexPaidByCard,
    ) { tx, cards, month, pendingByCard, paidByCard ->
        cards.associate { card ->
            card.id to when (card.cardType) {
                CreditCardType.REVOLVING,
                CreditCardType.INSTALLMENT,
                -> {
                    val cardTx = tx.filter {
                        (it.creditCardId == card.id || it.paymentMethodId == card.id)
                    }
                    val totalUtilized = cardTx
                        .filter { it.type == TransactionType.EXPENSE && it.isCreditCard }
                        .sumOf { it.amount }
                    val totalRepaid = cardTx
                        .filter { it.type == TransactionType.INCOME && it.isCreditCard }
                        .sumOf { it.amount }
                    val displayed = totalUtilized - totalRepaid
                    CreditCardSummary(
                        cardId = card.id,
                        name = card.name,
                        limit = card.limit,
                        cardType = card.cardType,
                        displayedSpent = displayed,
                        totalUtilized = totalUtilized,
                        totalPaid = 0.0,
                        totalRepaid = totalRepaid,
                        progress = if (card.limit > 0) (displayed / card.limit).toFloat() else 0f,
                    )
                }
                CreditCardType.AMEX_HYBRID -> {
                    val cardTx = tx.filter {
                        (it.creditCardId == card.id || it.paymentMethodId == card.id)
                    }
                    val totalUtilized = cardTx
                        .filter { it.type == TransactionType.EXPENSE && it.isCreditCard }
                        .sumOf { it.amount }
                    val incomeRepaid = cardTx
                        .filter { it.type == TransactionType.INCOME && it.isCreditCard }
                        .sumOf { it.amount }
                    val pagoflexPaid = paidByCard[card.id] ?: 0.0
                    val totalRepaid = incomeRepaid + pagoflexPaid
                    val displayed = (totalUtilized - totalRepaid).coerceAtLeast(0.0)
                    CreditCardSummary(
                        cardId = card.id,
                        name = card.name,
                        limit = card.limit,
                        cardType = card.cardType,
                        displayedSpent = displayed,
                        totalUtilized = totalUtilized,
                        totalPaid = totalRepaid,
                        totalRepaid = totalRepaid,
                        progress = if (card.limit > 0) (displayed / card.limit).toFloat() else 0f,
                    )
                }
                CreditCardType.SALDO -> {
                    val spent = tx
                        .filter { (it.creditCardId == card.id || it.paymentMethodId == card.id) && it.type == TransactionType.EXPENSE }
                        .filter { t ->
                            try {
                                YearMonth.from(LocalDate.parse(t.effectiveDate)) == month
                            } catch (_: Exception) {
                                false
                            }
                        }
                        .sumOf { it.amount }
                    CreditCardSummary(
                        cardId = card.id,
                        name = card.name,
                        limit = card.limit,
                        cardType = card.cardType,
                        displayedSpent = spent,
                        totalUtilized = spent,
                        totalPaid = 0.0,
                        progress = if (card.limit > 0) (spent / card.limit).toFloat() else 0f,
                    )
                }
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val bnplProjections: StateFlow<List<BnplProjection>> = combine(
        allTransactions,
        allPaymentMethods,
        allPaypalDetails,
        allKlarnaDetails,
        _currentDashboardMonth,
    ) { tx, methods, paypal, klarna, month ->
        calculateBnplProjectionsUseCase.execute(
            allTransactions = tx,
            allPaymentMethods = methods,
            paypalDetails = paypal,
            klarnaDetails = klarna,
            targetMonth = month,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            syncInstallmentPlanOutflows()
        }
    }

    fun addPaymentMethod(
        paymentMethod: PaymentMethodEntity,
        closingDay: Int = 0,
        paymentDay: Int = 0,
        creditLimit: Double = 0.0,
        debitIssuer: String? = null,
        debitCardNumber: String? = null,
        debitNotes: String? = null,
        linkedPaymentMethodId: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val domainMethod = paymentMethod.toDomain()
            val details = when (paymentMethod.provider) {
                PaymentProvider.CREDIT_CARD_SALDO,
                PaymentProvider.CREDIT_CARD_REVOLVING,
                PaymentProvider.CREDIT_CARD_INSTALLMENT,
                PaymentProvider.CREDIT_CARD_AMEX,
                -> PaymentMethodDetails.CreditCard(
                    name = paymentMethod.name,
                    cardType = when (paymentMethod.provider) {
                        PaymentProvider.CREDIT_CARD_SALDO -> CreditCardType.SALDO
                        PaymentProvider.CREDIT_CARD_REVOLVING -> CreditCardType.REVOLVING
                        PaymentProvider.CREDIT_CARD_INSTALLMENT -> CreditCardType.INSTALLMENT
                        PaymentProvider.CREDIT_CARD_AMEX -> CreditCardType.AMEX_HYBRID
                    },
                    limit = creditLimit,
                    closingDay = closingDay,
                    paymentDay = paymentDay,
                    linkedPaymentMethodId = linkedPaymentMethodId,
                )
                PaymentProvider.DEBIT_CARD -> PaymentMethodDetails.DebitCard(
                    name = paymentMethod.name,
                    issuer = debitIssuer,
                    cardNumber = debitCardNumber,
                    notes = debitNotes,
                )
                else -> null
            }
            managePaymentMethodUseCase.add(domainMethod, details)
        }
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.update(paymentMethod.toDomain())
        }
    }

    fun deletePaymentMethod(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.delete(id)
        }
    }

    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity? =
        paymentMethodRepo.getPaymentMethodById(id)?.toData()

    suspend fun getPaymentMethodDetails(method: PaymentMethodEntity): com.expense.management.domain.model.PaymentMethodDetails? =
        paymentMethodRepo.getPaymentMethodDetails(method.id)

    suspend fun calculateSatispayStatus(method: PaymentMethodEntity, detail: com.expense.management.data.SatispayDetailEntity): SatispayStatus {
        return calculateSatispayStatusUseCase.execute(method, detail)
    }

    fun syncInstallmentPlanOutflows() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val plans = repository.getAllInstallmentPlans()
            if (plans.isEmpty()) return@launch
            val methods = repository.getAllPaymentMethods()
            val details = repository.getAllCreditCardDetails()
            val categories = repository.getAllCategories()
            for (plan in plans) {
                val method = methods.find { it.id == plan.paymentMethodId } ?: continue
                val detail = details.find { it.paymentMethodId == plan.paymentMethodId }
                val card = ActiveCreditCard(
                    id = method.id,
                    name = method.name,
                    provider = method.provider,
                    cardType = detail?.cardType?.let { CreditCardType.safeValueOf(it) } ?: CreditCardType.INSTALLMENT,
                    limit = detail?.limit ?: 0.0,
                    closingDay = detail?.closingDay ?: 0,
                    paymentDay = detail?.paymentDay ?: 0,
                    linkedPaymentMethodId = detail?.linkedPaymentMethodId,
                )
                val payments = repository.getScheduledPaymentsByPlan(plan.id)
                    .filter { it.status == "PENDING" && it.expenseTransactionId == null }
                for (payment in payments) {
                    val dueDate = try {
                        LocalDate.parse(payment.dueDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (_: Exception) {
                        null
                    } ?: continue
                    if (dueDate.isAfter(today)) continue
                    val transactions = generateCreditCardPaymentUseCase.execute(card, payment.amount, dueDate, categories)
                    transactions.forEach { repository.insertTransaction(it) }
                    val expenseTx = transactions.firstOrNull()
                    repository.updateScheduledPaymentStatus(payment.id, "PAID", expenseTx?.id)
                }
                val paidCount = repository.getScheduledPaymentsByPlan(plan.id).count { it.status == "PAID" }
                repository.updateInstallmentPlanPaidCount(plan.id, paidCount)
            }
        }
    }

    fun payCreditCardInstallment(
        card: ActiveCreditCard,
        paymentAmount: Double,
        paymentDate: LocalDate,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = repository.getAllCategories()
            val transactions = generateCreditCardPaymentUseCase.execute(card, paymentAmount, paymentDate, categories)
            transactions.forEach { repository.insertTransaction(it) }
        }
    }

    fun payInstallmentPlan(
        plan: CreditCardInstallmentPlanEntity,
        card: ActiveCreditCard,
        paymentDate: LocalDate,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextPayment = repository.getNextPendingScheduledPayment(plan.id) ?: return@launch
            val categories = repository.getAllCategories()
            val transactions = generateCreditCardPaymentUseCase.execute(card, nextPayment.amount, paymentDate, categories)
            transactions.forEach { repository.insertTransaction(it) }
            val expenseTx = transactions.firstOrNull()
            repository.updateScheduledPaymentStatus(nextPayment.id, "PAID", expenseTx?.id)
            repository.updateInstallmentPlanPaidCount(plan.id, plan.paidCount + 1)
        }
    }

    fun saveInstallmentPlan(
        paymentMethodId: String,
        totalAmount: Double,
        installmentCount: Int,
        installmentAmount: Double,
        startDate: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingPlan = repository.getInstallmentPlanByCard(paymentMethodId)
            if (existingPlan != null) {
                val existingPayments = repository.getScheduledPaymentsByPlan(existingPlan.id)
                val paidCount = existingPayments.count { it.status == "PAID" }
                val paidAmount = existingPayments.filter { it.status == "PAID" }.sumOf { it.amount }
                val newTotal = existingPlan.totalAmount + totalAmount
                val residual = newTotal - paidAmount
                val remainingCount = installmentCount
                val newInstallmentAmount = residual / remainingCount
                val updatedPlan = existingPlan.copy(
                    totalAmount = newTotal,
                    installmentCount = paidCount + remainingCount,
                    installmentAmount = newInstallmentAmount,
                )
                repository.insertInstallmentPlan(updatedPlan)
                repository.deletePendingScheduledPaymentsByPlan(existingPlan.id)
                val payments = generateScheduledPayments(
                    planId = existingPlan.id,
                    totalAmount = residual,
                    installmentCount = remainingCount,
                    installmentAmount = newInstallmentAmount,
                    startDate = startDate,
                )
                repository.insertScheduledPayments(payments)
            } else {
                val planId = java.util.UUID.randomUUID().toString()
                val plan = CreditCardInstallmentPlanEntity(
                    id = planId,
                    paymentMethodId = paymentMethodId,
                    totalAmount = totalAmount,
                    installmentCount = installmentCount,
                    installmentAmount = installmentAmount,
                    paidCount = 0,
                    startDate = startDate,
                )
                repository.insertInstallmentPlan(plan)
                val payments = generateScheduledPayments(planId, totalAmount, installmentCount, installmentAmount, startDate)
                repository.insertScheduledPayments(payments)
            }
        }
    }

    private fun generateScheduledPayments(
        planId: String,
        totalAmount: Double,
        installmentCount: Int,
        installmentAmount: Double,
        startDate: String,
    ): List<InstallmentScheduledPaymentEntity> {
        val parts = startDate.split("-")
        val baseYear = parts[0].toIntOrNull() ?: 2024
        val baseMonth = parts[1].toIntOrNull() ?: 1
        val baseDay = (parts[2].toIntOrNull() ?: 1).coerceIn(1, 28)
        return List(installmentCount) { i ->
            val monthOffset = i
            val year = baseYear + (baseMonth - 1 + monthOffset) / 12
            val month = ((baseMonth - 1 + monthOffset) % 12) + 1
            val dueDate = "%04d-%02d-%02d".format(year, month, baseDay)
            val amount = if (i == installmentCount - 1) {
                totalAmount - (installmentAmount * (installmentCount - 1))
            } else {
                installmentAmount
            }
            InstallmentScheduledPaymentEntity(
                id = java.util.UUID.randomUUID().toString(),
                planId = planId,
                dueDate = dueDate,
                amount = amount,
                status = "PENDING",
            )
        }
    }

    fun updatePaymentMethodWithDetails(method: PaymentMethodEntity, details: com.expense.management.domain.model.PaymentMethodDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.update(method.toDomain(), details)
        }
    }

    fun updateDefaultPaymentMethod(id: String?) {
        val value = id ?: "__cash__"
        _defaultPaymentMethodId.value = value
        prefs?.edit { putString(KEY_DEFAULT_PAYMENT_METHOD, value) }
    }

    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            saveTransactionUseCase(transaction)
        }
    }

    fun deleteTransaction(
        transactionId: String,
        deleteType: com.expense.management.domain.model.DeleteType,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTransactionUseCase(transactionId, deleteType)
        }
    }
}
