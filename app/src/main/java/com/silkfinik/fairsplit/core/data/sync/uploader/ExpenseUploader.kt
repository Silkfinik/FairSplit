package com.silkfinik.fairsplit.core.data.sync.uploader

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.sync.FirestoreRoutes
import com.silkfinik.fairsplit.core.database.dao.ExpenseDao
import com.silkfinik.fairsplit.core.database.entity.ExpenseEntity
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseUploader @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val expenseDao: ExpenseDao,
    authRepository: AuthRepository
) : BaseFirestoreUploader<ExpenseEntity>(firestore, authRepository) {

    override val logTag: String = "ExpenseSync"

    override suspend fun getDirtyItems(): List<ExpenseEntity> {
        return expenseDao.getDirtyExpenses()
    }

    override suspend fun addToBatch(batch: WriteBatch, item: ExpenseEntity, userId: String) {
        val docRef = firestore.collection(FirestoreRoutes.GROUPS)
            .document(item.groupId)
            .collection(FirestoreRoutes.EXPENSES)
            .document(item.id)

        val dto = item.asDto()
        batch.set(docRef, dto)
    }

    override suspend fun markAsSynced(items: List<ExpenseEntity>) {
        items.forEach { expense ->
            expenseDao.markExpenseAsSynced(expense.id, expense.updatedAt)
        }
    }
}