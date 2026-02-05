package com.silkfinik.fairsplit.core.data.sync.uploader

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await

abstract class BaseFirestoreUploader<T>(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {

    protected abstract val logTag: String

    protected abstract suspend fun getDirtyItems(): List<T>

    protected abstract suspend fun addToBatch(batch: WriteBatch, item: T, userId: String)

    protected abstract suspend fun markAsSynced(items: List<T>)

    suspend fun syncLocalChanges() {
        val userId = authRepository.getUserId() ?: return

        val dirtyItems = getDirtyItems()
        if (dirtyItems.isEmpty()) return

        val batch = firestore.batch()

        dirtyItems.forEach { item ->
            try {
                addToBatch(batch, item, userId)
            } catch (e: Exception) {
                Log.e(logTag, "Error preparing batch item: $item", e)
                throw e
            }
        }

        try {
            batch.commit().await()
            Log.d(logTag, "Successfully uploaded ${dirtyItems.size} items")
            markAsSynced(dirtyItems)
        } catch (e: Exception) {
            Log.e(logTag, "Failed to upload batch", e)
            throw e
        }
    }
}