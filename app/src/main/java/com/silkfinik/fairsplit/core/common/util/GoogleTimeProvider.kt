package com.silkfinik.fairsplit.core.data.util

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleTimeProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TimeProvider {
    private val clientRef = AtomicReference<TrustedTimeClient?>(null)

    private var initTask: Task<TrustedTimeClient>? = null

    override suspend fun initialize() {
        if (clientRef.get() != null) return

        try {
            val task = synchronized(this) {
                initTask ?: TrustedTime.createClient(context).also { initTask = it }
            }

            val client = task.await()
            clientRef.set(client)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun now(): Long {
        val client = clientRef.get()

        val trustedTime = client?.computeCurrentUnixEpochMillis()

        return trustedTime ?: System.currentTimeMillis()
    }
}