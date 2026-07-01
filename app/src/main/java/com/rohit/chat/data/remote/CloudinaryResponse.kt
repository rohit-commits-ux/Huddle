package com.rohit.chat.data.remote

import com.google.gson.annotations.SerializedName

data class CloudinaryResponse(
    @SerializedName("secure_url")
    val secureUrl: String,
    @SerializedName("public_id")
    val publicId: String,
    @SerializedName("version")
    val version: String,
    @SerializedName("format")
    val format: String,
    @SerializedName("resource_type")
    val resourceType: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("bytes")
    val bytes: Int,
    @SerializedName("width")
    val width: Int,
    @SerializedName("height")
    val height: Int
)
