package com.encasaxo.hq.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Requests expected by Flask backend per provided spec
@JsonClass(generateAdapter = true)
data class CreatePackingListBody(
    val form: PackingForm,
    val items: List<CreateItem> = emptyList(),
    @Json(name = "boxDetails") val boxDetails: List<CartonDetailInput> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PackingForm(
    @Json(name = "shipment_id") val shipmentId: String,
    @Json(name = "dispatch_date") val dispatchDate: String?, // YYYY-MM-DD
    @Json(name = "channel_abb") val channelAbb: String?,
    val repository: String?,
    val production: String?,
    val mode: String?,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateItem(
    val sku: String,
    val product: String?,
    val line: String?,
    val design: String?,
    val size: String?,
    @Json(name = "pack_of") val packOf: Int = 0,
    @Json(name = "total_qty") val totalQty: Int = 0,
    @Json(name = "box_quantities") val boxQuantities: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CartonDetailInput(
    @Json(name = "carton_gross_weight") val cartonGrossWeight: String?,
    @Json(name = "carton_length") val cartonLength: String?,
    @Json(name = "carton_width") val cartonWidth: String?,
    @Json(name = "carton_height") val cartonHeight: String?
)

@JsonClass(generateAdapter = true)
data class UpdatePackingListBody(
    val items: List<UpdateItem> = emptyList(),
    @Json(name = "carton_details") val cartonDetails: List<CartonDetailUpdate> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateItem(
    val id: String?, // "new-..." or null indicates insert
    val sku: String?,
    val product: String?,
    val line: String?,
    val design: String?,
    val size: String?,
    @Json(name = "pack_of") val packOf: Int?,
    @Json(name = "total_qty") val totalQty: Int?,
    @Json(name = "box_details") val boxDetails: List<BoxItemUpdate> = emptyList(),
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class BoxItemUpdate(
    @Json(name = "box_no") val boxNo: Int,
    val quantity: Int
)

@JsonClass(generateAdapter = true)
data class CartonDetailUpdate(
    @Json(name = "box_no") val boxNo: Int,
    @Json(name = "carton_gross_weight") val cartonGrossWeight: String?,
    @Json(name = "carton_length") val cartonLength: String?,
    @Json(name = "carton_width") val cartonWidth: String?,
    @Json(name = "carton_height") val cartonHeight: String?
)

// Generic response wrapper
@JsonClass(generateAdapter = true)
data class GenericResponse(
    val success: Boolean,
    val message: String? = null
)

// List response from Flask: { success, packing_lists: [...] }
@JsonClass(generateAdapter = true)
data class PackingListListResponse(
    val success: Boolean,
    @Json(name = "packing_lists") val packingLists: List<PackingListHeader> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PackingListHeader(
    val id: String,
    @Json(name = "shipment_id") val shipmentId: String?,
    @Json(name = "dispatch_date") val dispatchDate: String?,
    @Json(name = "channel_abb") val channelAbb: String?,
    val repository: String?,
    val production: String?,
    val mode: String?,
    val status: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

// View response: { success, packing_list, items, carton_details }
@JsonClass(generateAdapter = true)
data class PackingListViewResponse(
    val success: Boolean,
    @Json(name = "packing_list") val packingList: PackingListHeader?,
    val items: List<PackingItem> = emptyList(),
    @Json(name = "carton_details") val cartonDetails: List<CartonDetail> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PackingItem(
    val id: String,
    @Json(name = "parent_id") val parentId: String,
    val sku: String?,
    val product: String?,
    val line: String?,
    val design: String?,
    val size: String?,
    @Json(name = "pack_of") val packOf: Int?,
    @Json(name = "total_qty") val totalQty: Int?,
    val status: String?,
    @Json(name = "box_details") val boxDetails: List<BoxItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BoxItem(
    val id: String?,
    @Json(name = "item_id") val itemId: String?,
    @Json(name = "box_no") val boxNo: Int?,
    val quantity: Int?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class CartonDetail(
    val id: String,
    @Json(name = "packing_list_id") val packingListId: String,
    @Json(name = "box_no") val boxNo: Int,
    @Json(name = "carton_gross_weight") val cartonGrossWeight: String?,
    @Json(name = "carton_length") val cartonLength: String?,
    @Json(name = "carton_width") val cartonWidth: String?,
    @Json(name = "carton_height") val cartonHeight: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

// Barcode mapping
@JsonClass(generateAdapter = true)
data class BarcodeMappingRequest(
    val barcode: String
)

@JsonClass(generateAdapter = true)
data class BarcodeMappingResponse(
    val success: Boolean? = null,
    val fnsku: String? = null,
    val sku: String? = null,
    val product: String? = null,
    val line: String? = null,
    val color: String? = null,
    val size: String? = null,
    @Json(name = "pcs_pack") val pcsPack: Int? = null
)

@JsonClass(generateAdapter = true)
data class BulkUpdateBody(
    val ids: List<String>,
    val field: String,
    val value: String
)
