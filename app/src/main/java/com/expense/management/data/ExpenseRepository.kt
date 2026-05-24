package com.expense.management.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyDao: CurrencyDao,
    private val creditCardDao: CreditCardDao,
    private val paymentMethodDao: PaymentMethodDao,
) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllFlow()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.delete(id)
    }

    suspend fun deleteTransactionGroup(groupId: String) {
        transactionDao.deleteByGroupId(groupId)
    }

    suspend fun getTransactionById(id: String): TransactionEntity? = transactionDao.getById(id)

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> = transactionDao.getAllList()

    suspend fun getMinEffectiveDate(): String? = transactionDao.getMinEffectiveDate()

    suspend fun getDescriptionSuggestions(query: String): List<String> = transactionDao.getDescriptionSuggestions(query)

    // Categories
    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun getAllCategories(): List<CategoryEntity> = categoryDao.getAllCategories()

    suspend fun getAllCreditCard(): List<CreditCardEntity> = creditCardDao.getAllCreditCards()

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategoryById(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    suspend fun insertAllCategories(categories: List<CategoryEntity>) {
        categoryDao.insertAllCategories(categories)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    // Currencies
    suspend fun getAllCurrencyRates(): List<CurrencyRate> = currencyDao.getAllRates()

    // Credit Cards
    val allCreditCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCreditCardsFlow()

    suspend fun getCreditCardById(id: String): CreditCardEntity? = creditCardDao.getCreditCardById(id)

    suspend fun insertCreditCard(creditCard: CreditCardEntity) {
        creditCardDao.insertCreditCard(creditCard)
    }

    suspend fun updateCreditCard(creditCard: CreditCardEntity) {
        creditCardDao.updateCreditCard(creditCard)
    }

    suspend fun deleteCreditCard(creditCard: CreditCardEntity) {
        transactionDao.nullifyCreditCardId(creditCard.id)
        transactionDao.nullifyPaymentMethodId(creditCard.id)
        creditCardDao.deleteCreditCard(creditCard)
    }

    suspend fun insertAllCreditCard(creditCards: List<CreditCardEntity>) {
        creditCardDao.insertAllCreditCards(creditCards)
    }

    suspend fun getTransactionsByGroupId(groupId: String): List<TransactionEntity> =
        transactionDao.getByGroupId(groupId)

    suspend fun deleteAllTransactions() = transactionDao.deleteAll()
    suspend fun deleteAllCategories() = categoryDao.deleteAll()
    suspend fun deleteAllCreditCards() = creditCardDao.deleteAll()

    // Payment Methods
    val allPaymentMethods: Flow<List<PaymentMethodEntity>> = paymentMethodDao.getAllPaymentMethodsFlow()

    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity> = paymentMethodDao.getAllPaymentMethods()

    suspend fun getAllCreditCardDetails(): List<CreditCardDetailEntity> = paymentMethodDao.getAllCreditCardDetails()

    suspend fun getAllRevolutDetails(): List<RevolutDetailEntity> = paymentMethodDao.getAllRevolutDetails()

    suspend fun getAllSatispayDetails(): List<SatispayDetailEntity> = paymentMethodDao.getAllSatispayDetails()

    suspend fun getAllPaypalDetails(): List<PaypalDetailEntity> = paymentMethodDao.getAllPaypalDetails()

    suspend fun getAllKlarnaDetails(): List<KlarnaDetailEntity> = paymentMethodDao.getAllKlarnaDetails()

    suspend fun getAllDebitCardDetails(): List<DebitCardDetailEntity> = paymentMethodDao.getAllDebitCardDetails()

    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity? = paymentMethodDao.getPaymentMethodById(id)

    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.insertPaymentMethod(paymentMethod)
    }

    suspend fun deletePaymentMethod(id: String) {
        transactionDao.nullifyPaymentMethodId(id)
        transactionDao.nullifyCreditCardId(id)
        paymentMethodDao.deletePaymentMethod(id)
    }

    suspend fun insertAllPaymentMethods(paymentMethods: List<PaymentMethodEntity>) {
        paymentMethodDao.insertAllPaymentMethods(paymentMethods)
    }

    suspend fun deleteAllPaymentMethods() = paymentMethodDao.deleteAll()

    // Payment Method Details
    suspend fun insertCreditCardDetail(detail: CreditCardDetailEntity) {
        paymentMethodDao.insertCreditCardDetail(detail)
    }

    suspend fun getCreditCardDetail(paymentMethodId: String): CreditCardDetailEntity? =
        paymentMethodDao.getCreditCardDetail(paymentMethodId)

    val allCreditCardDetails: Flow<List<CreditCardDetailEntity>> =
        paymentMethodDao.getAllCreditCardDetailsFlow()

    val allPaypalDetails: Flow<List<PaypalDetailEntity>> =
        paymentMethodDao.getAllPaypalDetailsFlow()

    val allKlarnaDetails: Flow<List<KlarnaDetailEntity>> =
        paymentMethodDao.getAllKlarnaDetailsFlow()

    suspend fun insertRevolutDetail(detail: RevolutDetailEntity) {
        paymentMethodDao.insertRevolutDetail(detail)
    }

    suspend fun getRevolutDetail(paymentMethodId: String): RevolutDetailEntity? =
        paymentMethodDao.getRevolutDetail(paymentMethodId)

    suspend fun insertSatispayDetail(detail: SatispayDetailEntity) {
        paymentMethodDao.insertSatispayDetail(detail)
    }

    suspend fun getSatispayDetail(paymentMethodId: String): SatispayDetailEntity? =
        paymentMethodDao.getSatispayDetail(paymentMethodId)

    suspend fun insertPaypalDetail(detail: PaypalDetailEntity) {
        paymentMethodDao.insertPaypalDetail(detail)
    }

    suspend fun getPaypalDetail(paymentMethodId: String): PaypalDetailEntity? =
        paymentMethodDao.getPaypalDetail(paymentMethodId)

    suspend fun insertKlarnaDetail(detail: KlarnaDetailEntity) {
        paymentMethodDao.insertKlarnaDetail(detail)
    }

    suspend fun getKlarnaDetail(paymentMethodId: String): KlarnaDetailEntity? =
        paymentMethodDao.getKlarnaDetail(paymentMethodId)

    suspend fun insertDebitCardDetail(detail: DebitCardDetailEntity) {
        paymentMethodDao.insertDebitCardDetail(detail)
    }

    suspend fun getDebitCardDetail(paymentMethodId: String): DebitCardDetailEntity? =
        paymentMethodDao.getDebitCardDetail(paymentMethodId)
}
