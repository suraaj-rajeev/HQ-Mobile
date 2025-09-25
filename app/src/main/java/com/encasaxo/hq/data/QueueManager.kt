package com.encasaxo.hq.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.queueDataStore by preferencesDataStore("encasa_queue")

data class QueuedUpdate(
    val packingListId: String,
    val payloadJson: String,
    val payloadType: String, // "full" or "delta"
    val createdAt: Long = System.currentTimeMillis()
)

class QueueManager(private val appContext: Context, private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()) {
    private val KEY = stringPreferencesKey("queued_updates")
    private val listType = Types.newParameterizedType(List::class.java, QueuedUpdate::class.java)
    private val adapter = moshi.adapter<List<QueuedUpdate>>(listType)

    val queueFlow: Flow<List<QueuedUpdate>> = appContext.queueDataStore.data.map { prefs ->
        prefs[KEY]?.let { json -> adapter.fromJson(json) } ?: emptyList()
    }

    suspend fun enqueue(item: QueuedUpdate) {
        appContext.queueDataStore.edit { prefs ->
            val current = prefs[KEY]?.let { adapter.fromJson(it) } ?: emptyList()
            val updated = current + item
            prefs[KEY] = adapter.toJson(updated)
        }
    }

    suspend fun peek(): QueuedUpdate? {
        val prefs = appContext.queueDataStore.data.first()
        val list = prefs[KEY]?.let { adapter.fromJson(it) } ?: emptyList()
        return list.firstOrNull()
    }

    suspend fun pop(): QueuedUpdate? {
        var removed: QueuedUpdate? = null
        appContext.queueDataStore.edit { prefs ->
            val current = prefs[KEY]?.let { adapter.fromJson(it) } ?: emptyList()
            if (current.isNotEmpty()) {
                removed = current.first()
                prefs[KEY] = adapter.toJson(current.drop(1))
            }
        }
        return removed
    }

    suspend fun clear() {
        appContext.queueDataStore.edit { prefs -> prefs.remove(KEY) }
    }
}


