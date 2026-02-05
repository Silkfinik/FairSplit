package com.silkfinik.fairsplit.core.data.sync.listener

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.sync.FirestoreRoutes
import com.silkfinik.fairsplit.core.database.dao.ExpenseDao
import com.silkfinik.fairsplit.core.network.model.ExpenseDto
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val expenseDao: ExpenseDao,
    @ApplicationScope externalScope: CoroutineScope
) : BaseSubCollectionListener<ExpenseDto>(externalScope) {

    override val logTag: String = "ExpenseSync"

    override fun getCollectionReference(groupId: String): CollectionReference {
        return firestore.collection(FirestoreRoutes.GROUPS)
            .document(groupId)
            .collection(FirestoreRoutes.EXPENSES)
    }

    override fun parseSnapshot(snapshot: QuerySnapshot): List<ExpenseDto> {
        return snapshot.toObjects(ExpenseDto::class.java)
    }

    override suspend fun saveServerDataToLocal(groupId: String, dtos: List<ExpenseDto>) {
        dtos.forEach { dto ->
            val localEntity = expenseDao.getExpenseById(dto.id)

            val shouldUpdate = shouldUpdate(
                localEntityExists = localEntity != null,
                localIsDirty = localEntity?.isDirty == true,
                localUpdatedAt = localEntity?.updatedAt ?: 0L,
                serverUpdatedAt = dto.updatedAt
            )

            if (shouldUpdate) {
                expenseDao.insertExpense(dto.asEntity(groupId))
            }
        }
    }
}