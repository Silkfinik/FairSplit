package com.silkfinik.fairsplit.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.silkfinik.fairsplit.core.data.sync.uploader.ExpenseUploader
import com.silkfinik.fairsplit.core.data.sync.uploader.GroupUploader
import com.silkfinik.fairsplit.core.data.sync.uploader.PaymentUploader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val groupUploader: GroupUploader,
    private val expenseUploader: ExpenseUploader,
    private val paymentUploader: PaymentUploader
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Запуск синхронизации...")

            groupUploader.syncLocalChanges()
            expenseUploader.syncLocalChanges()
            paymentUploader.syncLocalChanges()

            Log.d("SyncWorker", "Синхронизация успешно завершена")
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            Log.e("SyncWorker", "Ошибка при синхронизации", e)

            Result.retry()
        }
    }
}