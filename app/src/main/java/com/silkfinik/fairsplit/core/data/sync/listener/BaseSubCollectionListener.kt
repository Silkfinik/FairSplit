package com.silkfinik.fairsplit.core.data.sync.listener

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseSubCollectionListener<Dto>(
    private val externalScope: CoroutineScope
) : BaseFirestoreListener() {

    protected abstract val logTag: String

    protected abstract fun getCollectionReference(groupId: String): CollectionReference

    protected abstract fun parseSnapshot(snapshot: QuerySnapshot): List<Dto>

    protected abstract suspend fun saveServerDataToLocal(groupId: String, dtos: List<Dto>)

    private val listeners = mutableMapOf<String, ListenerRegistration>()

    fun startListening(groupId: String) {
        if (listeners.containsKey(groupId)) return

        Log.d("Sync", "Starting $logTag listener for group: $groupId")

        val query = getCollectionReference(groupId)

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Sync", "$logTag listen failed for group $groupId", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val dtos = parseSnapshot(snapshot)
                externalScope.launch {
                    saveServerDataToLocal(groupId, dtos)
                }
            }
        }

        listeners[groupId] = registration
    }

    fun stopListening(groupId: String) {
        listeners[groupId]?.remove()
        listeners.remove(groupId)
        Log.d("Sync", "Stopped $logTag listener for group: $groupId")
    }
}