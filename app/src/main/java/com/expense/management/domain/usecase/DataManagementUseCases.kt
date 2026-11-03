package com.expense.management.domain.usecase

import com.expense.management.data.BackupData
import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.isCycleSettlement
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider

class GetBackupDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(): BackupData = repository.withTransaction {
        BackupData(
            transactions = repository.getAllTransactionsList(),
            categories = repository.getAllCategories(),
            creditCard = emptyList(),
            paymentMethods = repository.getAllPaymentMethods(),
            creditCardDetails = repository.getAllCreditCardDetails(),
            revolutDetails = repository.getAllRevolutDetails(),
            satispayDetails = repository.getAllSatispayDetails(),
            paypalDetails = repository.getAllPaypalDetails(),
            klarnaDetails = repository.getAllKlarnaDetails(),
            debitCardDetails = repository.getAllDebitCardDetails(),
            version = BackupData.CURRENT_VERSION,
            billingCycles = repository.getAllBillingCycles(),
            installmentPlans = repository.getAllInstallmentPlans(),
            installmentScheduledPayments = repository.getAllInstallmentScheduledPayments(),
            amexStatements = repository.getAllAmexStatements(),
            amexPagoFlexPlans = repository.getAllAmexPagoFlexPlans(),
            amexScheduledPayments = repository.getAllAmexScheduledPaymentsList(),
            amexPlanChanges = repository.getAllAmexPlanChanges(),
            amexRevolvingStates = repository.getAllAmexRevolvingStates(),
            currencyRates = repository.getAllCurrencyRates(),
        )
    }
}

class RestoreDataUseCase(
    private val repository: ExpenseRepository,
    private val assignBillingCycle: AssignBillingCycleUseCase = AssignBillingCycleUseCase(repository),
) {
    /**
     * Sostituisce tutti i dati con quelli del backup, in una sola transazione: se una scrittura
     * fallisce restano i dati di prima. Le tabelle si scrivono dopo quelle a cui puntano; una riga
     * che punta a un genitore assente dal backup viene scartata invece di far fallire il ripristino.
     */
    suspend operator fun invoke(backupData: BackupData) = repository.withTransaction {
        // Le altre tabelle dipendono da transazioni e metodi di pagamento: si svuotano a cascata
        repository.deleteAllTransactions()
        repository.deleteAllCategories()
        repository.deleteAllPaymentMethods()

        val paymentMethods = backupData.paymentMethods
        if (!paymentMethods.isNullOrEmpty()) {
            repository.insertAllPaymentMethods(paymentMethods)
            backupData.creditCardDetails?.forEach { repository.insertCreditCardDetail(it) }
            backupData.revolutDetails?.forEach { repository.insertRevolutDetail(it) }
            backupData.satispayDetails?.forEach { repository.insertSatispayDetail(it) }
            backupData.paypalDetails?.forEach { repository.insertPaypalDetail(it) }
            backupData.klarnaDetails?.forEach { repository.insertKlarnaDetail(it) }
            backupData.debitCardDetails?.forEach { repository.insertDebitCardDetail(it) }
        }

        val legacyCards = backupData.creditCard
        if (!legacyCards.isNullOrEmpty() && paymentMethods.isNullOrEmpty()) {
            migrateLegacyCreditCards(legacyCards)
        }

        val methodIds = repository.getAllPaymentMethods().map { it.id }.toSet()
        // Backup v1: niente cicli, si ricalcolano in fondo. Dalla v2 tornano con il loro stato e addebito
        val cycles = backupData.billingCycles.orEmpty().filter { it.paymentMethodId in methodIds }
        cycles.forEach { repository.insertBillingCycle(it) }
        val cycleIds = cycles.map { it.id }.toSet()
        val transactions = backupData.transactions.orEmpty().map { tx ->
            if (tx.billingCycleId in cycleIds) tx else tx.copy(billingCycleId = null)
        }
        repository.insertAllTransactions(transactions)
        repository.insertAllCategories(backupData.categories.orEmpty())

        val transactionIds = transactions.map { it.id }.toSet()
        val statements = backupData.amexStatements.orEmpty().filter { it.paymentMethodId in methodIds }
        statements.forEach { repository.insertAmexStatement(it) }
        val statementIds = statements.map { it.id }.toSet()
        val pagoFlexPlans = backupData.amexPagoFlexPlans.orEmpty()
            .filter { it.statementId in statementIds && it.transactionId in transactionIds }
        pagoFlexPlans.forEach { repository.insertAmexPagoFlexPlan(it) }
        val pagoFlexPlanIds = pagoFlexPlans.map { it.id }.toSet()
        repository.insertAmexScheduledPayments(backupData.amexScheduledPayments.orEmpty().filter { it.planId in pagoFlexPlanIds })
        backupData.amexPlanChanges.orEmpty().filter { it.planId in pagoFlexPlanIds }.forEach { repository.insertAmexPlanChange(it) }
        backupData.amexRevolvingStates.orEmpty().filter { it.statementId in statementIds }.forEach { repository.insertAmexRevolvingState(it) }

        val installmentPlans = backupData.installmentPlans.orEmpty().filter { it.paymentMethodId in methodIds }
        installmentPlans.forEach { repository.insertInstallmentPlan(it) }
        val installmentPlanIds = installmentPlans.map { it.id }.toSet()
        repository.insertScheduledPayments(backupData.installmentScheduledPayments.orEmpty().filter { it.planId in installmentPlanIds })

        backupData.currencyRates?.takeIf { it.isNotEmpty() }?.let { repository.insertCurrencyRates(it) }

        // Solo ora le carte esistono di nuovo: si possono ricalcolare i cicli mancanti (tutti, per un backup v1)
        assignBillingCycle.assignMissing()
        relinkSettlements(backupData.transactions.orEmpty())
    }

    /**
     * Gli addebiti dei cicli gia' liquidati tornano dal backup con il loro sourceRef, ma i cicli sono
     * ricreati senza: senza ricollegarli lo storico li perde e le spese aggiunte dopo non contano.
     * Un addebito il cui ciclo non esiste piu' resta una semplice uscita sul conto.
     */
    private suspend fun relinkSettlements(transactions: List<TransactionEntity>) {
        transactions.filter { it.isCycleSettlement }.forEach { settlement ->
            val cycleId = BillingCycleEntity.cycleIdOfSettlement(settlement.sourceRef) ?: return@forEach
            if (repository.getBillingCycleById(cycleId) != null) {
                repository.markBillingCycleSettled(cycleId, settlement.id)
            }
        }
    }

    private suspend fun migrateLegacyCreditCards(legacyCards: List<CreditCardEntity>) {
        val methods = legacyCards.map { card ->
            PaymentMethodEntity(
                id = card.id,
                name = card.name,
                provider = if (card.type == com.expense.management.data.CardType.SALDO) {
                    PaymentProvider.CREDIT_CARD_SALDO
                } else {
                    PaymentProvider.CREDIT_CARD_REVOLVING
                },
                isActive = true,
                issuer = null,
                currency = null,
            )
        }
        repository.insertAllPaymentMethods(methods)
        legacyCards.forEach { card ->
            repository.insertCreditCardDetail(
                CreditCardDetailEntity(
                    paymentMethodId = card.id,
                    cardType = if (card.type == com.expense.management.data.CardType.SALDO) {
                        CreditCardType.SALDO.name
                    } else {
                        CreditCardType.REVOLVING.name
                    },
                    limit = card.limit,
                    closingDay = card.closingDay,
                    paymentDay = card.paymentDay,
                ),
            )
        }
    }
}
