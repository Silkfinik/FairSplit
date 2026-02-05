package com.silkfinik.fairsplit.core.common.util

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> DocumentReference.asFlow(dataType: Class<T>): Flow<T?> = callbackFlow {
    val listener = addSnapshotListener { snapshot, e ->
        if (e != null) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                close()
            } else {
                close(e)
            }
            return@addSnapshotListener
        }

        if (snapshot != null && snapshot.exists()) {
            trySend(snapshot.toObject(dataType))
        } else {
            trySend(null)
        }
    }
    awaitClose { listener.remove() }
}