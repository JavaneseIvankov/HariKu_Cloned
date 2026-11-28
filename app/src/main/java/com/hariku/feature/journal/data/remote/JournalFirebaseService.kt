package com.hariku.feature.journal.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.hariku.feature.journal.data.dto.JournalEntryDto
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

class JournalFirebaseService(
    private val firestore: FirebaseFirestore,
    private val isAuthenticated: () -> Boolean = { true }
) : JournalRemoteDataSource {

    private fun <T> ProducerScope<T>.handleListenerError(
        error: FirebaseFirestoreException,
        emptyFallback: T
    ) {
        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && !isAuthenticated()) {
            trySend(emptyFallback)
            close()
        } else {
            close(error)
        }
    }

    private suspend fun getDocumentSnapshot(docRef: DocumentReference): DocumentSnapshot =
        try {
            docRef.get(Source.DEFAULT).await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                docRef.get(Source.CACHE).await()
            } else {
                throw error
            }
        }

    /**
     * Firestore write Tasks complete only after server acknowledgment. A short timeout
     * catches immediate online rejections (e.g. PERMISSION_DENIED) without hanging UI
     * state indefinitely while offline, since the write is already queued in local cache.
     */
    private suspend fun awaitWrite(task: Task<Void>) {
        withTimeoutOrNull(WRITE_ACK_TIMEOUT_MS) {
            task.await()
        }
    }

    private fun journalsCollection(userId: String) =
        firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_JOURNALS)

    override fun observeAll(userId: String): Flow<List<JournalEntryDto>> = callbackFlow {
        val listener = journalsCollection(userId)
            .orderBy(FIELD_DATE_MILLIS, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleListenerError(error, emptyList())
                    return@addSnapshotListener
                }

                val entries = snapshot?.toObjects(JournalEntryDto::class.java)
                    .orEmpty()
                    .sortedWith(
                        compareByDescending<JournalEntryDto> { it.isPinned }
                            .thenByDescending { it.dateMillis }
                            .thenByDescending { it.updatedAt }
                    )
                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    override fun observeLatestTag(userId: String): Flow<String?> = callbackFlow {
        val listener = journalsCollection(userId)
            .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleListenerError(error, null)
                    return@addSnapshotListener
                }

                val latestTag = snapshot?.documents
                    ?.firstOrNull()
                    ?.toObject(JournalEntryDto::class.java)
                    ?.tag
                trySend(latestTag)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getById(id: Long, userId: String): JournalEntryDto? {
        if (id <= 0L) return null
        val docRef = journalsCollection(userId).document(id.toString())
        return getDocumentSnapshot(docRef).toObject(JournalEntryDto::class.java)
    }

    override suspend fun save(entry: JournalEntryDto, userId: String): Long? {
        val targetId = if (entry.id == 0L) {
            generateUniqueEntryId()
        } else {
            val docRef = journalsCollection(userId).document(entry.id.toString())
            if (!getDocumentSnapshot(docRef).exists()) return null
            entry.id
        }

        val payload = entry.copy(
            id = targetId,
            userId = userId
        )
        awaitWrite(
            journalsCollection(userId)
                .document(targetId.toString())
                .set(payload)
        )
        return targetId
    }

    override suspend fun upsertBatch(entries: List<JournalEntryDto>, userId: String) {
        if (entries.isEmpty()) return
        val collection = journalsCollection(userId)
        entries.chunked(FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { entry ->
                val payload = entry.copy(userId = userId)
                batch.set(collection.document(payload.id.toString()), payload)
            }
            awaitWrite(batch.commit())
        }
    }

    override suspend fun delete(id: Long, userId: String): Boolean {
        if (id <= 0L) return false
        val docRef = journalsCollection(userId).document(id.toString())
        if (!getDocumentSnapshot(docRef).exists()) return false
        awaitWrite(docRef.delete())
        return true
    }

    override suspend fun togglePin(id: Long, userId: String): Boolean {
        if (id <= 0L) return false
        val docRef = journalsCollection(userId).document(id.toString())
        val existing = getDocumentSnapshot(docRef).toObject(JournalEntryDto::class.java) ?: return false
        awaitWrite(
            docRef.update(
                mapOf(
                    FIELD_IS_PINNED to !existing.isPinned,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                )
            )
        )
        return true
    }

    private fun generateUniqueEntryId(): Long =
        System.currentTimeMillis() * 1_000L + Random.nextLong(0L, 1_000L)

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val SUBCOLLECTION_JOURNALS = "journals"
        private const val FIELD_DATE_MILLIS = "dateMillis"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_IS_PINNED = "isPinned"
        private const val WRITE_ACK_TIMEOUT_MS = 2_500L
        private const val FIRESTORE_BATCH_LIMIT = 500
    }
}
