package com.expense.management.data

/**
 * Contenuto del backup JSON. version assente = backup v1 (prima di C4a): solo transazioni,
 * categorie e metodi di pagamento; i cicli si ricalcolano al ripristino.
 * Dalla v2 ci sono tutte le tabelle, cicli compresi.
 */
data class BackupData(
    val transactions: List<TransactionEntity>?,
    val categories: List<CategoryEntity>?,
    val creditCard: List<CreditCardEntity>?,
    val paymentMethods: List<PaymentMethodEntity>? = null,
    val creditCardDetails: List<CreditCardDetailEntity>? = null,
    val revolutDetails: List<RevolutDetailEntity>? = null,
    val satispayDetails: List<SatispayDetailEntity>? = null,
    val paypalDetails: List<PaypalDetailEntity>? = null,
    val klarnaDetails: List<KlarnaDetailEntity>? = null,
    val debitCardDetails: List<DebitCardDetailEntity>? = null,
    val version: Int? = null,
    val billingCycles: List<BillingCycleEntity>? = null,
    val installmentPlans: List<CreditCardInstallmentPlanEntity>? = null,
    val installmentScheduledPayments: List<InstallmentScheduledPaymentEntity>? = null,
    val amexStatements: List<AmexStatementEntity>? = null,
    val amexPagoFlexPlans: List<AmexPagoFlexPlanEntity>? = null,
    val amexScheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>? = null,
    val amexPlanChanges: List<AmexPagoFlexPlanChangeEntity>? = null,
    val amexRevolvingStates: List<AmexRevolvingStateEntity>? = null,
    val currencyRates: List<CurrencyRate>? = null,
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}
