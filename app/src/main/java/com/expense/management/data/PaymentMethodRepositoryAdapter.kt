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
        return methods.map {
            it.toDomain(resolveDetails(it, ccDetails, revolutDetails, satispayDetails, paypalDetails, klarnaDetails))
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
        when (details) {
            is PaymentMethodDetails.CreditCard -> repository.insertCreditCardDetail(
                CreditCardDetailEntity(
                    paymentMethodId = method.id,
                    cardType = details.cardType.name,
                    limit = details.limit,
                    closingDay = details.closingDay,
                    paymentDay = details.paymentDay,
                    linkedPaymentMethodId = details.linkedPaymentMethodId,
                ),
            )
            is PaymentMethodDetails.Revolut -> repository.insertRevolutDetail(
                RevolutDetailEntity(
                    paymentMethodId = method.id,
                    currency = details.currency,
                    iban = details.iban,
                    accountNumber = details.accountNumber,
                ),
            )
            is PaymentMethodDetails.Satispay -> repository.insertSatispayDetail(
                SatispayDetailEntity(
                    paymentMethodId = method.id,
                    weeklyBudget = details.weeklyBudget,
                    sddDay = details.sddDay,
                    iban = details.iban,
                ),
            )
            is PaymentMethodDetails.Paypal -> repository.insertPaypalDetail(
                PaypalDetailEntity(
                    paymentMethodId = method.id,
                    email = details.email,
                    bnplInstallmentCount = details.bnplInstallmentCount,
                    bnplCycleDays = details.bnplCycleDays,
                ),
            )
            is PaymentMethodDetails.Klarna -> repository.insertKlarnaDetail(
                KlarnaDetailEntity(
                    paymentMethodId = method.id,
                    bnplInstallmentCount = details.bnplInstallmentCount,
                    bnplCycleDays = details.bnplCycleDays,
                ),
            )
            is PaymentMethodDetails.DebitCard -> repository.insertDebitCardDetail(
                DebitCardDetailEntity(
                    paymentMethodId = method.id,
                    issuer = details.issuer,
                    cardNumber = details.cardNumber,
                    notes = details.notes,
                ),
            )
            is PaymentMethodDetails.Cash -> {}
            null -> {}
        }
    }

    override suspend fun updatePaymentMethod(method: PaymentMethod, details: PaymentMethodDetails?) {
        repository.updatePaymentMethod(method.toData())
        if (details != null) {
            when (details) {
                is PaymentMethodDetails.CreditCard -> repository.insertCreditCardDetail(
                    CreditCardDetailEntity(
                        paymentMethodId = method.id,
                        cardType = details.cardType.name,
                        limit = details.limit,
                        closingDay = details.closingDay,
                        paymentDay = details.paymentDay,
                        linkedPaymentMethodId = details.linkedPaymentMethodId,
                    ),
                )
                is PaymentMethodDetails.Revolut -> repository.insertRevolutDetail(
                    RevolutDetailEntity(
                        paymentMethodId = method.id,
                        currency = details.currency,
                        iban = details.iban,
                        accountNumber = details.accountNumber,
                    ),
                )
                is PaymentMethodDetails.Satispay -> repository.insertSatispayDetail(
                    SatispayDetailEntity(
                        paymentMethodId = method.id,
                        weeklyBudget = details.weeklyBudget,
                        sddDay = details.sddDay,
                        iban = details.iban,
                    ),
                )
                is PaymentMethodDetails.Paypal -> repository.insertPaypalDetail(
                    PaypalDetailEntity(
                        paymentMethodId = method.id,
                        email = details.email,
                        bnplInstallmentCount = details.bnplInstallmentCount,
                        bnplCycleDays = details.bnplCycleDays,
                    ),
                )
                is PaymentMethodDetails.Klarna -> repository.insertKlarnaDetail(
                    KlarnaDetailEntity(
                        paymentMethodId = method.id,
                        bnplInstallmentCount = details.bnplInstallmentCount,
                        bnplCycleDays = details.bnplCycleDays,
                    ),
                )
                is PaymentMethodDetails.DebitCard -> repository.insertDebitCardDetail(
                    DebitCardDetailEntity(
                        paymentMethodId = method.id,
                        issuer = details.issuer,
                        cardNumber = details.cardNumber,
                        notes = details.notes,
                    ),
                )
                is PaymentMethodDetails.Cash -> {}
            }
        }
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
            resolveDetailsV2(method, ccDetails, revolutDetails, satispayDetails, paypalDetails, klarnaDetails, debitDetails)
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
        PaymentProvider.DEBIT_CARD -> null
        PaymentProvider.CASH -> PaymentMethodDetails.Cash(name = method.name)
    }

    private fun resolveDetailsV2(
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
