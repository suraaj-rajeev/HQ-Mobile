package com.encasaxo.hq.network

import com.encasaxo.hq.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface PackingListApi {
    @GET("api/erp_packing_list/list")
    suspend fun list(): Response<PackingListListResponse>

    @GET("api/erp_packing_list/view/{id}")
    suspend fun view(@Path("id") id: String): Response<PackingListViewResponse>

    @POST("api/erp_packing_list/create")
    suspend fun create(@Body body: CreatePackingListBody): Response<GenericResponse>

    @PUT("api/erp_packing_list/update/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdatePackingListBody): Response<GenericResponse>

    @DELETE("api/erp_packing_list/delete/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>

    @POST("api/erp_packing_list/bulk_update")
    suspend fun bulkUpdate(@Body body: BulkUpdateBody): Response<GenericResponse>

    // Barcode mapping endpoint provided
    @POST("api/erp/barcode-mapping")
    suspend fun barcodeMapping(@Body body: BarcodeMappingRequest): Response<BarcodeMappingResponse>

    // Some servers expose a GET variant with a query param
    @GET("api/erp/barcode-mapping")
    suspend fun barcodeMappingGet(@Query("barcode") barcode: String): Response<BarcodeMappingResponse>

    // Some servers return a list without a filter param
    @GET("api/erp/barcode-mapping")
    suspend fun barcodeMappingList(): Response<List<BarcodeMappingResponse>>

    // Module-level cache helpers
    @GET("api/erp/barcode-mapping/last-cleared")
    suspend fun barcodeMappingLastCleared(): Response<Map<String, String>>

    @POST("api/erp/barcode-mapping/clear-cache")
    suspend fun barcodeMappingClearCache(): Response<GenericResponse>
}
