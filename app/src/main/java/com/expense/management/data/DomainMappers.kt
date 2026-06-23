package com.expense.management.data

import com.expense.management.domain.model.CreditCardDetails
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentMethod
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.model.PaymentProvider

fun com.expense.management.domain.model.TransactionType.toData(): TransactionType = when (this) {
    com.expense.management.domain.model.TransactionType.INCOME -> TransactionType.INCOME
    com.expense.management.domain.model.TransactionType.EXPENSE -> TransactionType.EXPENSE
}

fun TransactionType.toDomain(): com.expense.management.domain.model.TransactionType = when (this) {
    TransactionType.INCOME -> com.expense.management.domain.model.TransactionType.INCOME
    TransactionType.EXPENSE -> com.expense.management.domain.model.TransactionType.EXPENSE
}

fun com.expense.management.domain.model.RecurrenceType.toData(): RecurrenceType = when (this) {
    com.expense.management.domain.model.RecurrenceType.NONE -> RecurrenceType.NONE
    com.expense.management.domain.model.RecurrenceType.DAILY -> RecurrenceType.DAILY
    com.expense.management.domain.model.RecurrenceType.WEEKLY -> RecurrenceType.WEEKLY
    com.expense.management.domain.model.RecurrenceType.MONTHLY -> RecurrenceType.MONTHLY
    com.expense.management.domain.model.RecurrenceType.YEARLY -> RecurrenceType.YEARLY
}

fun RecurrenceType.toDomain(): com.expense.management.domain.model.RecurrenceType = when (this) {
    RecurrenceType.NONE -> com.expense.management.domain.model.RecurrenceType.NONE
    RecurrenceType.DAILY -> com.expense.management.domain.model.RecurrenceType.DAILY
    RecurrenceType.WEEKLY -> com.expense.management.domain.model.RecurrenceType.WEEKLY
    RecurrenceType.MONTHLY -> com.expense.management.domain.model.RecurrenceType.MONTHLY
    RecurrenceType.YEARLY -> com.expense.management.domain.model.RecurrenceType.YEARLY
}

fun CategoryEntity.toDomain(): com.expense.management.domain.model.Category = com.expense.management.domain.model.Category(
    id = id,
    label = label,
    icon = icon,
    type = type.toDomain(),
    isCustom = isCustom,
    imageUri = imageUri,
)

fun com.expense.management.domain.model.Category.toData(): CategoryEntity = CategoryEntity(
    id = id,
    label = label,
    icon = icon,
    type = type.toData(),
    isCustom = isCustom,
    imageUri = imageUri,
)

fun TransactionEntity.toDomain(): com.expense.management.domain.model.Transaction = com.expense.management.domain.model.Transaction(
    id = id,
    date = date,
    description = description,
    amount = amount,
    categoryId = categoryId,
    type = type.toDomain(),
    isCreditCard = isCreditCard,
    effectiveDate = effectiveDate,
    originalAmount = originalAmount,
    originalCurrency = originalCurrency,
    installmentNumber = installmentNumber,
    totalInstallments = totalInstallments,
    groupId = groupId,
    creditCardId = creditCardId,
    paymentMethodId = paymentMethodId,
    recurrenceType = recurrenceType.toDomain(),
    recurrenceEndDate = recurrenceEndDate,
    recurrenceLimit = recurrenceLimit,
)

fun com.expense.management.domain.model.Transaction.toData(): TransactionEntity = TransactionEntity(
    id = id,
    date = date,
    description = description,
    amount = amount,
    categoryId = categoryId,
    type = type.toData(),
    isCreditCard = isCreditCard,
    effectiveDate = effectiveDate,
    originalAmount = originalAmount,
    originalCurrency = originalCurrency,
    installmentNumber = installmentNumber,
    totalInstallments = totalInstallments,
    groupId = groupId,
    creditCardId = creditCardId,
    paymentMethodId = paymentMethodId,
    recurrenceType = recurrenceType.toData(),
    recurrenceEndDate = recurrenceEndDate,
    recurrenceLimit = recurrenceLimit,
)

fun PaymentMethodEntity.toDomain(): PaymentMethod = when (provider) {
    PaymentProvider.CASH -> PaymentMethod.Cash(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
    )
    PaymentProvider.CREDIT_CARD_SALDO -> PaymentMethod.CreditCard(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
        cardType = CreditCardType.SALDO,
        limit = 0.0,
        closingDay = 0,
        paymentDay = 0,
    )
    PaymentProvider.CREDIT_CARD_REVOLVING -> PaymentMethod.CreditCard(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
        cardType = CreditCardType.REVOLVING,
        limit = 0.0,
        closingDay = 0,
        paymentDay = 0,
    )
    PaymentProvider.CREDIT_CARD_INSTALLMENT -> PaymentMethod.CreditCard(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
        cardType = CreditCardType.INSTALLMENT,
        limit = 0.0,
        closingDay = 0,
        paymentDay = 0,
    )
    PaymentProvider.CREDIT_CARD_AMEX -> PaymentMethod.CreditCard(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
        cardType = CreditCardType.AMEX_HYBRID,
        limit = 0.0,
        closingDay = 0,
        paymentDay = 0,
    )
    PaymentProvider.DEBIT_CARD -> PaymentMethod.Cash(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
    )
    PaymentProvider.REVOLUT -> PaymentMethod.Revolut(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
    )
    PaymentProvider.SATISPAY -> PaymentMethod.Satispay(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
        weeklyBudget = 0.0,
    )
    PaymentProvider.PAYPAL -> PaymentMethod.PayPal(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
        email = "",
    )
    PaymentProvider.KLARNA -> PaymentMethod.Klarna(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
    )
}

fun PaymentMethodEntity.toDomain(details: PaymentMethodDetails?): PaymentMethod = when (provider) {
    PaymentProvider.CASH -> PaymentMethod.Cash(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
    )
    PaymentProvider.CREDIT_CARD_SALDO,
    PaymentProvider.CREDIT_CARD_REVOLVING,
    PaymentProvider.CREDIT_CARD_INSTALLMENT,
    PaymentProvider.CREDIT_CARD_AMEX,
    -> {
        val detail = details as? PaymentMethodDetails.CreditCard
        PaymentMethod.CreditCard(
            id = id,
            name = name,
            isActive = isActive,
            issuer = issuer,
            currency = currency,
            cardType = detail?.cardType ?: CreditCardType.SALDO,
            limit = detail?.limit ?: 0.0,
            closingDay = detail?.closingDay ?: 0,
            paymentDay = detail?.paymentDay ?: 0,
            interestRate = null,
        )
    }
    PaymentProvider.DEBIT_CARD -> PaymentMethod.Cash(
        id = id,
        name = name,
        isActive = isActive,
        issuer = issuer,
        currency = currency,
    )
    PaymentProvider.REVOLUT -> {
        val detail = details as? PaymentMethodDetails.Revolut
        PaymentMethod.Revolut(
            id = id,
            name = name,
            isActive = isActive,
            issuer = issuer,
            currency = currency,
            iban = detail?.iban,
            accountNumber = detail?.accountNumber,
        )
    }
    PaymentProvider.SATISPAY -> {
        val detail = details as? PaymentMethodDetails.Satispay
        PaymentMethod.Satispay(
            id = id,
            name = name,
            isActive = isActive,
            issuer = issuer,
            currency = currency,
            weeklyBudget = detail?.weeklyBudget ?: 0.0,
            sddDay = detail?.sddDay ?: 1,
            iban = detail?.iban,
        )
    }
    PaymentProvider.PAYPAL -> {
        val detail = details as? PaymentMethodDetails.Paypal
        PaymentMethod.PayPal(
            id = id,
            name = name,
            isActive = isActive,
            issuer = issuer,
            currency = currency,
            email = detail?.email ?: "",
            bnplInstallmentCount = detail?.bnplInstallmentCount ?: 3,
            bnplCycleDays = detail?.bnplCycleDays ?: 14,
        )
    }
    PaymentProvider.KLARNA -> {
        val detail = details as? PaymentMethodDetails.Klarna
        PaymentMethod.Klarna(
            id = id,
            name = name,
            isActive = isActive,
            issuer = issuer,
            currency = currency,
            bnplInstallmentCount = detail?.bnplInstallmentCount ?: 4,
            bnplCycleDays = detail?.bnplCycleDays ?: 14,
        )
    }
}

fun PaymentMethod.toData(): PaymentMethodEntity = PaymentMethodEntity(
    id = id,
    name = name,
    provider = provider,
    isActive = isActive,
    issuer = issuer,
    currency = currency,
)

fun CreditCardDetailEntity.toDomain(): CreditCardDetails = CreditCardDetails(
    paymentMethodId = paymentMethodId,
    cardType = CreditCardType.safeValueOf(cardType) ?: CreditCardType.SALDO,
    limit = limit,
    closingDay = closingDay,
    paymentDay = paymentDay,
    interestRate = interestRate,
    issuer = issuer,
    linkedPaymentMethodId = linkedPaymentMethodId,
)

fun CreditCardDetails.toData(): CreditCardDetailEntity = CreditCardDetailEntity(
    paymentMethodId = paymentMethodId,
    cardType = cardType.name,
    limit = limit,
    closingDay = closingDay,
    paymentDay = paymentDay,
    interestRate = interestRate,
    issuer = issuer,
    linkedPaymentMethodId = linkedPaymentMethodId,
)

fun CreditCardDetailEntity.toDomain(name: String): PaymentMethodDetails.CreditCard = PaymentMethodDetails.CreditCard(
    name = name,
    cardType = CreditCardType.safeValueOf(cardType) ?: CreditCardType.SALDO,
    limit = limit,
    closingDay = closingDay,
    paymentDay = paymentDay,
    linkedPaymentMethodId = linkedPaymentMethodId,
)

fun PaymentMethodDetails.CreditCard.toData(paymentMethodId: String): CreditCardDetailEntity = CreditCardDetailEntity(
    paymentMethodId = paymentMethodId,
    cardType = cardType.name,
    limit = limit,
    closingDay = closingDay,
    paymentDay = paymentDay,
    linkedPaymentMethodId = linkedPaymentMethodId,
)

fun RevolutDetailEntity.toDomain(name: String): PaymentMethodDetails.Revolut = PaymentMethodDetails.Revolut(
    name = name,
    currency = currency,
    iban = iban,
    accountNumber = accountNumber,
)

fun PaymentMethodDetails.Revolut.toData(paymentMethodId: String): RevolutDetailEntity = RevolutDetailEntity(
    paymentMethodId = paymentMethodId,
    currency = currency,
    iban = iban,
    accountNumber = accountNumber,
)

fun SatispayDetailEntity.toDomain(name: String): PaymentMethodDetails.Satispay = PaymentMethodDetails.Satispay(
    name = name,
    weeklyBudget = weeklyBudget,
    sddDay = sddDay,
    iban = iban,
)

fun PaymentMethodDetails.Satispay.toData(paymentMethodId: String): SatispayDetailEntity = SatispayDetailEntity(
    paymentMethodId = paymentMethodId,
    weeklyBudget = weeklyBudget,
    sddDay = sddDay,
    iban = iban,
)

fun PaypalDetailEntity.toDomain(name: String): PaymentMethodDetails.Paypal = PaymentMethodDetails.Paypal(
    name = name,
    email = email,
    bnplInstallmentCount = bnplInstallmentCount,
    bnplCycleDays = bnplCycleDays,
)

fun PaymentMethodDetails.Paypal.toData(paymentMethodId: String): PaypalDetailEntity = PaypalDetailEntity(
    paymentMethodId = paymentMethodId,
    email = email,
    bnplInstallmentCount = bnplInstallmentCount,
    bnplCycleDays = bnplCycleDays,
)

fun KlarnaDetailEntity.toDomain(name: String): PaymentMethodDetails.Klarna = PaymentMethodDetails.Klarna(
    name = name,
    bnplInstallmentCount = bnplInstallmentCount,
    bnplCycleDays = bnplCycleDays,
)

fun PaymentMethodDetails.Klarna.toData(paymentMethodId: String): KlarnaDetailEntity = KlarnaDetailEntity(
    paymentMethodId = paymentMethodId,
    bnplInstallmentCount = bnplInstallmentCount,
    bnplCycleDays = bnplCycleDays,
)

fun DebitCardDetailEntity.toDomain(name: String): PaymentMethodDetails.DebitCard = PaymentMethodDetails.DebitCard(
    name = name,
    issuer = issuer,
    cardNumber = cardNumber,
    notes = notes,
)

fun PaymentMethodDetails.DebitCard.toData(paymentMethodId: String): DebitCardDetailEntity = DebitCardDetailEntity(
    paymentMethodId = paymentMethodId,
    issuer = issuer,
    cardNumber = cardNumber,
    notes = notes,
)

fun ActiveCreditCardEntity.toDomain(): com.expense.management.domain.model.ActiveCreditCard = com.expense.management.domain.model.ActiveCreditCard(
    id = paymentMethod.id,
    name = paymentMethod.name,
    provider = paymentMethod.provider,
    cardType = CreditCardType.safeValueOf(creditCardDetail.cardType) ?: CreditCardType.SALDO,
    limit = creditCardDetail.limit,
    closingDay = creditCardDetail.closingDay,
    paymentDay = creditCardDetail.paymentDay,
    linkedPaymentMethodId = creditCardDetail.linkedPaymentMethodId,
)
