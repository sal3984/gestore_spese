package com.expense.management.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {

    @Query("SELECT * FROM payment_methods ORDER BY name ASC")
    fun getAllPaymentMethodsFlow(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods")
    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity>

    @Query("SELECT * FROM payment_methods WHERE id = :id")
    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity)

    @Update
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPaymentMethods(paymentMethods: List<PaymentMethodEntity>)

    @Query("DELETE FROM payment_methods WHERE id = :id")
    suspend fun deletePaymentMethod(id: String)

    @Query("DELETE FROM payment_methods")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCardDetail(detail: CreditCardDetailEntity)

    @Query("SELECT * FROM credit_card_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getCreditCardDetail(paymentMethodId: String): CreditCardDetailEntity?

    @Query("SELECT * FROM credit_card_details")
    fun getAllCreditCardDetailsFlow(): Flow<List<CreditCardDetailEntity>>

    @Query("SELECT * FROM credit_card_details")
    suspend fun getAllCreditCardDetails(): List<CreditCardDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevolutDetail(detail: RevolutDetailEntity)

    @Query("SELECT * FROM revolut_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getRevolutDetail(paymentMethodId: String): RevolutDetailEntity?

    @Query("SELECT * FROM revolut_details")
    fun getAllRevolutDetailsFlow(): Flow<List<RevolutDetailEntity>>

    @Query("SELECT * FROM revolut_details")
    suspend fun getAllRevolutDetails(): List<RevolutDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatispayDetail(detail: SatispayDetailEntity)

    @Query("SELECT * FROM satispay_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getSatispayDetail(paymentMethodId: String): SatispayDetailEntity?

    @Query("SELECT * FROM satispay_details")
    fun getAllSatispayDetailsFlow(): Flow<List<SatispayDetailEntity>>

    @Query("SELECT * FROM satispay_details")
    suspend fun getAllSatispayDetails(): List<SatispayDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaypalDetail(detail: PaypalDetailEntity)

    @Query("SELECT * FROM paypal_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getPaypalDetail(paymentMethodId: String): PaypalDetailEntity?

    @Query("SELECT * FROM paypal_details")
    fun getAllPaypalDetailsFlow(): Flow<List<PaypalDetailEntity>>

    @Query("SELECT * FROM paypal_details")
    suspend fun getAllPaypalDetails(): List<PaypalDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKlarnaDetail(detail: KlarnaDetailEntity)

    @Query("SELECT * FROM klarna_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getKlarnaDetail(paymentMethodId: String): KlarnaDetailEntity?

    @Query("SELECT * FROM klarna_details")
    fun getAllKlarnaDetailsFlow(): Flow<List<KlarnaDetailEntity>>

    @Query("SELECT * FROM klarna_details")
    suspend fun getAllKlarnaDetails(): List<KlarnaDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitCardDetail(detail: DebitCardDetailEntity)

    @Query("SELECT * FROM debit_card_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getDebitCardDetail(paymentMethodId: String): DebitCardDetailEntity?

    @Query("SELECT * FROM debit_card_details")
    fun getAllDebitCardDetailsFlow(): Flow<List<DebitCardDetailEntity>>

    @Query("SELECT * FROM debit_card_details")
    suspend fun getAllDebitCardDetails(): List<DebitCardDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentPlan(plan: CreditCardInstallmentPlanEntity)

    @Query("SELECT * FROM credit_card_installment_plans WHERE paymentMethodId = :paymentMethodId LIMIT 1")
    suspend fun getInstallmentPlanByCard(paymentMethodId: String): CreditCardInstallmentPlanEntity?

    @Query("SELECT * FROM credit_card_installment_plans")
    fun getAllInstallmentPlansFlow(): Flow<List<CreditCardInstallmentPlanEntity>>

    @Query("SELECT * FROM credit_card_installment_plans")
    suspend fun getAllInstallmentPlans(): List<CreditCardInstallmentPlanEntity>

    @Query("UPDATE credit_card_installment_plans SET paidCount = :paidCount WHERE id = :planId")
    suspend fun updateInstallmentPlanPaidCount(planId: String, paidCount: Int)

    @Query("DELETE FROM credit_card_installment_plans WHERE id = :planId")
    suspend fun deleteInstallmentPlan(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayment(payment: InstallmentScheduledPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayments(payments: List<InstallmentScheduledPaymentEntity>)

    @Query("SELECT * FROM installment_scheduled_payments WHERE planId = :planId ORDER BY dueDate ASC")
    suspend fun getScheduledPaymentsByPlan(planId: String): List<InstallmentScheduledPaymentEntity>

    @Query("SELECT * FROM installment_scheduled_payments WHERE planId = :planId ORDER BY dueDate ASC")
    fun getScheduledPaymentsByPlanFlow(planId: String): Flow<List<InstallmentScheduledPaymentEntity>>

    @Query("SELECT * FROM installment_scheduled_payments ORDER BY dueDate ASC")
    fun getAllScheduledPaymentsFlow(): Flow<List<InstallmentScheduledPaymentEntity>>

    @Query("SELECT * FROM installment_scheduled_payments WHERE planId = :planId AND status = 'PENDING' ORDER BY dueDate ASC LIMIT 1")
    suspend fun getNextPendingScheduledPayment(planId: String): InstallmentScheduledPaymentEntity?

    @Query("SELECT isp.* FROM installment_scheduled_payments isp INNER JOIN credit_card_installment_plans ccip ON isp.planId = ccip.id WHERE ccip.paymentMethodId = :paymentMethodId AND isp.status = 'PENDING' ORDER BY isp.dueDate ASC")
    fun getPendingScheduledPaymentsByCardFlow(paymentMethodId: String): Flow<List<InstallmentScheduledPaymentEntity>>

    @Query("UPDATE installment_scheduled_payments SET status = :status, expenseTransactionId = :expenseTransactionId WHERE id = :paymentId")
    suspend fun updateScheduledPaymentStatus(paymentId: String, status: String, expenseTransactionId: String?)

    @Query("DELETE FROM installment_scheduled_payments WHERE planId = :planId")
    suspend fun deleteScheduledPaymentsByPlan(planId: String)
}
