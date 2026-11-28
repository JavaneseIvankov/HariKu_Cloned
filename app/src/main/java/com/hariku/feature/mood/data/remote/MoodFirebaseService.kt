package com.hariku.feature.mood.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.hariku.feature.mood.data.dto.MoodEntryDto
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

class MoodFirebaseService(
    private val firestore: FirebaseFirestore,
    private val isAuthenticated: () -> Boolean = { true }
) : MoodRemoteDataSource {

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
            withTimeoutOrNull(READ_TIMEOUT_MS) {
                docRef.get(Source.DEFAULT).await()
            } ?: docRef.get(Source.CACHE).await()
        } catch (error: FirebaseFirestoreException) {
            when (error.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.ABORTED -> docRef.get(Source.CACHE).await()
                else -> throw error
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

    private fun moodsCollection(userId: String) =
        firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_MOODS)

    override fun observeAll(userId: String): Flow<List<MoodEntryDto>> = callbackFlow {
        val listener = moodsCollection(userId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleListenerError(error, emptyList())
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { doc ->
                        val dto = doc.toObject(MoodEntryDto::class.java) ?: return@mapNotNull null
                        if (dto.id > 0L) {
                            dto
                        } else {
                            val fallbackId = doc.id.toLongOrNull() ?: return@mapNotNull null
                            dto.copy(id = fallbackId)
                        }
                    }
                    .sortedWith(
                        compareByDescending<MoodEntryDto> { it.timestamp }
                            .thenByDescending { it.id }
                    )
                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun save(entry: MoodEntryDto, userId: String): Long? {
        val targetId = if (entry.id == 0L) {
            generateUniqueEntryId()
        } else {
            val docRef = moodsCollection(userId).document(entry.id.toString())
            if (!getDocumentSnapshot(docRef).exists()) return null
            entry.id
        }

        val payload = entry.copy(
            id = targetId,
            userId = userId
        )
        awaitWrite(
            moodsCollection(userId)
                .document(targetId.toString())
                .set(payload)
        )
        return targetId
    }

    override suspend fun upsertBatch(entries: List<MoodEntryDto>, userId: String) {
        if (entries.isEmpty()) return
        val collection = moodsCollection(userId)
        entries.chunked(FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { entry ->
                val payload = entry.copy(userId = userId)
                batch.set(collection.document(payload.id.toString()), payload)
            }
            try {
                withTimeout(MIGRATION_ACK_TIMEOUT_MS) {
                    batch.commit().await()
                }
            } catch (timeout: TimeoutCancellationException) {
                throw IllegalStateException("Firestore migration timed out awaiting server acknowledgment", timeout)
            }
        }
    }

    override suspend fun update(
        entryId: Long,
        userId: String,
        moodType: String,
        note: String,
        updatedAt: Long
    ): Boolean {
        if (entryId <= 0L) return false
        val docRef = moodsCollection(userId).document(entryId.toString())
        if (!getDocumentSnapshot(docRef).exists()) return false
        awaitWrite(
            docRef.update(
                mapOf(
                    FIELD_MOOD_TYPE to moodType,
                    FIELD_NOTE to note,
                    FIELD_UPDATED_AT to updatedAt
                )
            )
        )
        return true
    }

    override suspend fun delete(entryId: Long, userId: String): Boolean {
        if (entryId <= 0L) return false
        val docRef = moodsCollection(userId).document(entryId.toString())
        if (!getDocumentSnapshot(docRef).exists()) return false
        awaitWrite(docRef.delete())
        return true
    }

    private fun generateUniqueEntryId(): Long =
        (System.currentTimeMillis() * 1_000L + Random.nextLong(0L, 1_000L)).coerceAtLeast(1L)

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val SUBCOLLECTION_MOODS = "moods"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_MOOD_TYPE = "moodType"
        private const val FIELD_NOTE = "note"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val READ_TIMEOUT_MS = 2_500L
        private const val WRITE_ACK_TIMEOUT_MS = 2_500L
        private const val MIGRATION_ACK_TIMEOUT_MS = 8_000L
        private const val FIRESTORE_BATCH_LIMIT = 500
    }
}
