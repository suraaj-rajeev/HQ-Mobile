package com.encasaxo.hq.data

import com.encasaxo.hq.network.ApiModule
import com.encasaxo.hq.network.PackingListApi
import com.encasaxo.hq.network.dto.CreatePackingListBody
import com.encasaxo.hq.network.dto.UpdatePackingListBody
import com.encasaxo.hq.network.dto.BulkUpdateBody
import com.encasaxo.hq.network.dto.GenericResponse
import com.encasaxo.hq.network.dto.BarcodeMappingRequest
import com.encasaxo.hq.network.dto.BarcodeMappingResponse
import retrofit2.Response

/**
 * Simple repository wrapper around the Retrofit API.
 * Keep this small — business logic and mapping belongs in ViewModel or a separate mapper class.
 */
class PackingListRepository(
    private val api: PackingListApi = ApiModule.packingListApi
) {
    // Simple in-memory cache for barcode mappings
    private val mappingCache: MutableMap<String, com.encasaxo.hq.network.dto.BarcodeMappingResponse> = mutableMapOf()
    suspend fun list(): Response<com.encasaxo.hq.network.dto.PackingListListResponse> = api.list()
    suspend fun view(id: String): Response<com.encasaxo.hq.network.dto.PackingListViewResponse> = api.view(id)
    suspend fun create(body: CreatePackingListBody): Response<GenericResponse> = api.create(body)
    suspend fun update(id: String, body: UpdatePackingListBody): Response<GenericResponse> = api.update(id, body)
    suspend fun delete(id: String): Response<GenericResponse> = api.delete(id)
    suspend fun bulkUpdate(body: BulkUpdateBody): Response<GenericResponse> = api.bulkUpdate(body)

    suspend fun barcodeMapping(barcode: String): Response<BarcodeMappingResponse> {
        // Cache hit
        mappingCache[barcode]?.let { return Response.success(it) }

        return try {
            // Server provides a list on GET with no params. Fetch and filter client-side.
            val listResp = api.barcodeMappingList()
            if (listResp.isSuccessful) {
                val list = listResp.body().orEmpty()
                val match = list.firstOrNull { it.fnsku?.equals(barcode, ignoreCase = true) == true || it.sku?.equals(barcode, ignoreCase = true) == true }
                if (match != null) {
                    mappingCache[barcode] = match
                    Response.success(match)
                } else {
                    Response.success(null)
                }
            } else {
                // Treat as not found; UI will show "Not found" or error body if needed
                Response.success(null)
            }
        } catch (_: Exception) {
            // As a safe fallback just return null success; avoids 405 from unsupported methods
            Response.success(null)
        }
    }

    suspend fun barcodeMappingClearCache(): Boolean {
        try {
            mappingCache.clear()
            val resp = api.barcodeMappingClearCache()
            return resp.isSuccessful
        } catch (_: Exception) {
            // still clear local
            return false
        }
    }

    suspend fun barcodeMappingLastCleared(): String? = try {
        val resp = api.barcodeMappingLastCleared()
        if (resp.isSuccessful) resp.body()?.values?.firstOrNull() else null
    } catch (_: Exception) { null }
}
