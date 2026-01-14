package com.silkfinik.fairsplit.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.silkfinik.fairsplit.core.data.sync.ExpenseUploader
import com.silkfinik.fairsplit.core.data.sync.GroupUploader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val groupUploader: GroupUploader,
    private val expenseUploader: ExpenseUploader
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            groupUploader.syncLocalChanges()
            expenseUploader.syncLocalChanges()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}