package com.expense.management.domain.usecase

import com.expense.management.data.KlarnaDetailEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.PaypalDetailEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.domain.model.BnplInstallment
import com.expense.management.domain.model.BnplProjection
import com.expense.management.domain.model.PaymentProvider
import java.time.LocalDate
import java.time.YearMonth

class CalculateBnplProjectionsUseCase {

    fun execute(
        allTransactions: List<TransactionEntity>,
        allPaymentMethods: List<PaymentMethodEntity>,
        paypalDetails: List<PaypalDetailEntity>,
        klarnaDetails: List<KlarnaDetailEntity>,
        targetMonth: YearMonth,
    ): List<BnplProjection> {
        val paypalMethods = allPaymentMethods.filter {
            it.provider == PaymentProvider.PAYPAL
        }
        val klarnaMethods = allPaymentMethods.filter {
            it.provider == PaymentProvider.KLARNA
        }

        val paypalMethodIds = paypalMethods.map { it.id }.toSet()
        val klarnaMethodIds = klarnaMethods.map { it.id }.toSet()

        val bnplTransactions = allTransactions.filter { tx ->
            tx.paymentMethodId != null &&
                (tx.paymentMethodId in paypalMethodIds || tx.paymentMethodId in klarnaMethodIds)
        }

        val projections = mutableListOf<BnplProjection>()

        for (method in paypalMethods) {
            val detail = paypalDetails.find { it.paymentMethodId == method.id } ?: continue
            val txs = bnplTransactions.filter { it.paymentMethodId == method.id }
            if (txs.isEmpty()) continue

            val installments = projectInstallments(
                transactions = txs,
                bnplCount = detail.bnplInstallmentCount,
                bnplCycleDays = detail.bnplCycleDays,
                targetMonth = targetMonth,
                provider = PaymentProvider.PAYPAL,
            )
            if (installments.isNotEmpty()) {
                projections.add(
                    BnplProjection(method.name, PaymentProvider.PAYPAL, installments),
                )
            }
        }

        for (method in klarnaMethods) {
            val detail = klarnaDetails.find { it.paymentMethodId == method.id } ?: continue
            val txs = bnplTransactions.filter { it.paymentMethodId == method.id }
            if (txs.isEmpty()) continue

            val installments = projectInstallments(
                transactions = txs,
                bnplCount = detail.bnplInstallmentCount,
                bnplCycleDays = detail.bnplCycleDays,
                targetMonth = targetMonth,
                provider = PaymentProvider.KLARNA,
            )
            if (installments.isNotEmpty()) {
                projections.add(
                    BnplProjection(method.name, PaymentProvider.KLARNA, installments),
                )
            }
        }

        return projections
    }

    private fun projectInstallments(
        transactions: List<TransactionEntity>,
        bnplCount: Int,
        bnplCycleDays: Int,
        targetMonth: YearMonth,
        provider: PaymentProvider,
    ): List<BnplInstallment> {
        val result = mutableListOf<BnplInstallment>()
        for (tx in transactions) {
            val txDate = try {
                LocalDate.parse(tx.date)
            } catch (_: Exception) {
                continue
            }
            val installmentAmount = if (bnplCount > 0) tx.amount / bnplCount else 0.0

            for (i in 0 until bnplCount) {
                val installmentDate = txDate.plusDays((i * bnplCycleDays).toLong())
                val month = YearMonth.from(installmentDate)

                if (month == targetMonth && installmentAmount > 0) {
                    result.add(
                        BnplInstallment(
                            expectedDate = installmentDate,
                            amount = installmentAmount,
                            description = tx.description,
                            transactionId = tx.id,
                        ),
                    )
                }
            }
        }
        return result
    }
}
