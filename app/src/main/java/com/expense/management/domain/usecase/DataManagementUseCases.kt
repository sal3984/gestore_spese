package com.expense.management.domain.usecase

import com.expense.management.data.BackupData
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider

class GetBackupDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(): BackupData = BackupData(
        transactions = repository.getAllTransactionsList(),
        categories = repository.getAllCategories(),
        creditCard = repository.getAllCreditCard(),
        paymentMethods = repository.getAllPaymentMethods(),
        creditCardDetails = repository.getAllCreditCardDetails(),
        revolutDetails = repository.getAllRevolutDetails(),
        satispayDetails = repository.getAllSatispayDetails(),
        paypalDetails = repository.getAllPaypalDetails(),
        klarnaDetails = repository.getAllKlarnaDetails(),
        debitCardDetails = repository.getAllDebitCardDetails(),
    )
}

class RestoreDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(backupData: BackupData) {
        repository.deleteAllTransactions()
        repository.deleteAllCategories()
        repository.deleteAllCreditCards()
        repository.deleteAllPaymentMethods()
        repository.insertAllTransactions(backupData.transactions ?: emptyList())
        repository.insertAllCategories(backupData.categories ?: emptyList())

        val paymentMethods = backupData.paymentMethods
        if (!paymentMethods.isNullOrEmpty()) {
            repository.insertAllPaymentMethods(paymentMethods)
            backupData.creditCardDetails?.let { details ->
                details.forEach { repository.insertCreditCardDetail(it) }
            }
            backupData.revolutDetails?.let { details ->
                details.forEach { repository.insertRevolutDetail(it) }
            }
            backupData.satispayDetails?.let { details ->
                details.forEach { repository.insertSatispayDetail(it) }
            }
            backupData.paypalDetails?.let { details ->
                details.forEach { repository.insertPaypalDetail(it) }
            }
            backupData.klarnaDetails?.let { details ->
                details.forEach { repository.insertKlarnaDetail(it) }
            }
            backupData.debitCardDetails?.let { details ->
                details.forEach { repository.insertDebitCardDetail(it) }
            }
        }

        val legacyCards = backupData.creditCard
        if (!legacyCards.isNullOrEmpty()) {
            repository.insertAllCreditCard(legacyCards)
            if (paymentMethods.isNullOrEmpty()) {
                migrateLegacyCreditCards(legacyCards)
            }
        }
    }

    private suspend fun migrateLegacyCreditCards(legacyCards: List<CreditCardEntity>) {
        val methods = legacyCards.map { card ->
            PaymentMethodEntity(
                id = card.id,
                name = card.name,
                provider = if (card.type == com.expense.management.data.CardType.SALDO) {
                    PaymentProvider.CREDIT_CARD_SALDO.name
                } else {
                    PaymentProvider.CREDIT_CARD_REVOLVING.name
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
