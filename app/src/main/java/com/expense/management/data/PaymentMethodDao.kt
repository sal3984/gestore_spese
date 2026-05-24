package com.expense.management.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    suspend fun getAllRevolutDetails(): List<RevolutDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatispayDetail(detail: SatispayDetailEntity)

    @Query("SELECT * FROM satispay_details WHERE paymentMethodId = :paymentMethodId")
    suspend fun getSatispayDetail(paymentMethodId: String): SatispayDetailEntity?

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
}
