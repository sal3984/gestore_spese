package com.expense.management.data

import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentMethod
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PaymentMethodRepositoryAdapter(
    private val repository: ExpenseRepository,
) : PaymentMethodRepository {

    override fun getAllPaymentMethodsFlow(): Flow<List<PaymentMethod>> =
        repository.allPaymentMethods.map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPaymentMethodById(id: String): PaymentMethod? {
        val entity = repository.getPaymentMethodById(id) ?: return null
        return entity.toDomain(getDetails(entity))
    }

    override suspend fun getAllPaymentMethods(): List<PaymentMethod> {
        val methods = repository.getAllPaymentMethods()
        val ccDetails = repository.getAllCreditCardDetails()
        val revolutDetails = repository.getAllRevolutDetails()
        val satispayDetails = repository.getAllSatispayDetails()
        val paypalDetails = repository.getAllPaypalDetails()
        val klarnaDetails = repository.getAllKlarnaDetails()
        val debitDetails = repository.getAllDebitCardDetails()
        return methods.map {
            it.toDomain(resolveDetails(it, ccDetails, revolutDetails, satispayDetails, paypalDetails, klarnaDetails, debitDetails))
        }
    }

    override fun getAllActiveCreditCardsFlow(): Flow<List<ActiveCreditCard>> = combine(
        repository.allPaymentMethods,
        repository.allCreditCardDetails,
    ) { methods, details ->
        methods
            .filter {
                it.provider == PaymentProvider.CREDIT_CARD_SALDO ||
                    it.provider == PaymentProvider.CREDIT_CARD_REVOLVING ||
                    it.provider == PaymentProvider.CREDIT_CARD_INSTALLMENT ||
                    it.provider == PaymentProvider.CREDIT_CARD_AMEX
            }
            .mapNotNull { method ->
                val detail = details.find { it.paymentMethodId == method.id }
                val provider = method.provider
                if (provider == PaymentProvider.CREDIT_CARD_AMEX) {
                    ActiveCreditCard(
                        id = method.id,
                        name = method.name,
                        provider = provider,
                        cardType = detail?.let { CreditCardType.safeValueOf(it.cardType) } ?: CreditCardType.AMEX_HYBRID,
                        limit = detail?.limit ?: 0.0,
                        closingDay = detail?.closingDay ?: 0,
                        paymentDay = detail?.paymentDay ?: 0,
                        linkedPaymentMethodId = detail?.linkedPaymentMethodId,
                    )
                } else {
                    detail?.let {
                        val cardType = CreditCardType.safeValueOf(it.cardType) ?: return@mapNotNull null
                        ActiveCreditCard(
                            id = method.id,
                            name = method.name,
                            provider = provider,
                            cardType = cardType,
                            limit = it.limit,
                            closingDay = it.closingDay,
                            paymentDay = it.paymentDay,
                            linkedPaymentMethodId = it.linkedPaymentMethodId,
                        )
                    }
                }
            }
    }

    override suspend fun savePaymentMethod(method: PaymentMethod, details: PaymentMethodDetails?) {
        repository.insertPaymentMethod(method.toData())
        writeDetails(method.id, details)
    }

    override suspend fun updatePaymentMethod(method: PaymentMethod, details: PaymentMethodDetails?) {
        repository.updatePaymentMethod(method.toData())
        writeDetails(method.id, details)
    }

    private suspend fun writeDetails(paymentMethodId: String, details: PaymentMethodDetails?) {
        when (details) {
            is PaymentMethodDetails.CreditCard -> repository.insertCreditCardDetail(
                creditCardDetailEntity(paymentMethodId, details),
            )
            is PaymentMethodDetails.Revolut -> repository.insertRevolutDetail(
                RevolutDetailEntity(
                    paymentMethodId = paymentMethodId,
                    currency = details.currency,
                    iban = details.iban,
                    accountNumber = details.accountNumber,
                ),
            )
            is PaymentMethodDetails.Satispay -> repository.insertSatispayDetail(
                SatispayDetailEntity(
                    paymentMethodId = paymentMethodId,
                    weeklyBudget = details.weeklyBudget,
                    sddDay = details.sddDay,
                    iban = details.iban,
                ),
            )
            is PaymentMethodDetails.Paypal -> repository.insertPaypalDetail(
                PaypalDetailEntity(
                    paymentMethodId = paymentMethodId,
                    email = details.email,
                    bnplInstallmentCount = details.bnplInstallmentCount,
                    bnplCycleDays = details.bnplCycleDays,
                ),
            )
            is PaymentMethodDetails.Klarna -> repository.insertKlarnaDetail(
                KlarnaDetailEntity(
                    paymentMethodId = paymentMethodId,
                    bnplInstallmentCount = details.bnplInstallmentCount,
                    bnplCycleDays = details.bnplCycleDays,
                ),
            )
            is PaymentMethodDetails.DebitCard -> repository.insertDebitCardDetail(
                DebitCardDetailEntity(
                    paymentMethodId = paymentMethodId,
                    issuer = details.issuer,
                    cardNumber = details.cardNumber,
                    notes = details.notes,
                ),
            )
            is PaymentMethodDetails.Cash -> {}
            null -> {}
        }
    }

    private suspend fun creditCardDetailEntity(
        paymentMethodId: String,
        details: PaymentMethodDetails.CreditCard,
    ): CreditCardDetailEntity {
        val existing = repository.getCreditCardDetail(paymentMethodId)
        return CreditCardDetailEntity(
            paymentMethodId = paymentMethodId,
            cardType = details.cardType.name,
            limit = details.limit,
            closingDay = details.closingDay,
            paymentDay = details.paymentDay,
            interestRate = existing?.interestRate,
            issuer = existing?.issuer,
            linkedPaymentMethodId = details.linkedPaymentMethodId,
        )
    }

    override suspend fun deletePaymentMethod(id: String) {
        repository.deletePaymentMethod(id)
    }

    override suspend fun deleteAllPaymentMethods() {
        repository.deleteAllPaymentMethods()
    }

    override suspend fun getAllPaymentMethodDetails(): List<PaymentMethodDetails> {
        val methods = repository.getAllPaymentMethods()
        val ccDetails = repository.getAllCreditCardDetails()
        val revolutDetails = repository.getAllRevolutDetails()
        val satispayDetails = repository.getAllSatispayDetails()
        val paypalDetails = repository.getAllPaypalDetails()
        val klarnaDetails = repository.getAllKlarnaDetails()
        val debitDetails = repository.getAllDebitCardDetails()
        return methods.mapNotNull { method ->
            resolveDetails(method, ccDetails, revolutDetails, satispayDetails, paypalDetails, klarnaDetails, debitDetails)
        }
    }

    override suspend fun getPaymentMethodDetails(paymentMethodId: String): PaymentMethodDetails? {
        val method = repository.getPaymentMethodById(paymentMethodId) ?: return null
        return getDetails(method)
    }

    private suspend fun getDetails(method: PaymentMethodEntity): PaymentMethodDetails? = when (method.provider) {
        PaymentProvider.CREDIT_CARD_SALDO,
        PaymentProvider.CREDIT_CARD_REVOLVING,
        PaymentProvider.CREDIT_CARD_INSTALLMENT,
        PaymentProvider.CREDIT_CARD_AMEX,
        -> repository.getCreditCardDetail(method.id)?.toDomain(method.name)
        PaymentProvider.REVOLUT -> repository.getRevolutDetail(method.id)?.toDomain(method.name)
        PaymentProvider.SATISPAY -> repository.getSatispayDetail(method.id)?.toDomain(method.name)
        PaymentProvider.PAYPAL -> repository.getPaypalDetail(method.id)?.toDomain(method.name)
        PaymentProvider.KLARNA -> repository.getKlarnaDetail(method.id)?.toDomain(method.name)
        PaymentProvider.DEBIT_CARD -> repository.getDebitCardDetail(method.id)?.toDomain(method.name)
        PaymentProvider.CASH -> PaymentMethodDetails.Cash(name = method.name)
    }

    private fun resolveDetails(
        method: PaymentMethodEntity,
        ccDetails: List<CreditCardDetailEntity>,
        revolutDetails: List<RevolutDetailEntity>,
        satispayDetails: List<SatispayDetailEntity>,
        paypalDetails: List<PaypalDetailEntity>,
        klarnaDetails: List<KlarnaDetailEntity>,
        debitDetails: List<DebitCardDetailEntity>,
    ): PaymentMethodDetails? = when (method.provider) {
        PaymentProvider.CREDIT_CARD_SALDO,
        PaymentProvider.CREDIT_CARD_REVOLVING,
        PaymentProvider.CREDIT_CARD_INSTALLMENT,
        PaymentProvider.CREDIT_CARD_AMEX,
        -> ccDetails.find { it.paymentMethodId == method.id }?.toDomain(method.name)
        PaymentProvider.REVOLUT -> revolutDetails.find { it.paymentMethodId == method.id }?.toDomain(method.name)
        PaymentProvider.SATISPAY -> satispayDetails.find { it.paymentMethodId == method.id }?.toDomain(method.name)
        PaymentProvider.PAYPAL -> paypalDetails.find { it.paymentMethodId == method.id }?.toDomain(method.name)
        PaymentProvider.KLARNA -> klarnaDetails.find { it.paymentMethodId == method.id }?.toDomain(method.name)
        PaymentProvider.DEBIT_CARD -> debitDetails.find { it.paymentMethodId == method.id }?.toDomain(method.name)
        PaymentProvider.CASH -> PaymentMethodDetails.Cash(name = method.name)
    }
}
