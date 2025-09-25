package com.encasaxo.hq.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.encasaxo.hq.network.ApiModule
import com.encasaxo.hq.network.dto.UpdatePackingListBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class UpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val queue = QueueManager(appContext, moshi)
    private val repository = PackingListRepository(ApiModule.packingListApi)

    override suspend fun doWork(): Result {
        val next = queue.peek() ?: return Result.success()
        return try {
            val adapter = moshi.adapter(UpdatePackingListBody::class.java)
            val body = adapter.fromJson(next.payloadJson) ?: return Result.success().also { queue.pop() }
            val resp = repository.update(next.packingListId, body)
            if (resp.isSuccessful) {
                queue.pop()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}


