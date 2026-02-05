package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPayments(groupId: String): Flow<List<Payment>>
    fun getPayment(paymentId: String): Flow<Payment?>

    suspend fun createPayment(payment: Payment): Result<Unit>
    suspend fun updatePayment(payment: Payment): Result<Unit>

    // suspend fun syncPayments(groupId: String)

    fun startSync(groupId: String)
    fun stopSync(groupId: String)
}