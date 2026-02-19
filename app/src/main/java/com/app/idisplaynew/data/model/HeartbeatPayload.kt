package com.app.idisplaynew.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class HeartbeatPayload(
    @SerialName("status") val status: String,
    @SerialName("ipAddress") val ipAddress: String,
    @SerialName("temperature") val temperature: Double,
    @SerialName("cpuUsage") val cpuUsage: Double,
    @SerialName("memoryUsage") val memoryUsage: Double,
    @SerialName("storageUsage") val storageUsage: Double,
    @SerialName("storageAvailableMb") val storageAvailableMb: Double,
    @SerialName("currentLayoutId") val currentLayoutId: String,
    @SerialName("currentMediaId") val currentMediaId: String,
    @SerialName("appVersion") val appVersion: String,
    @SerialName("osVersion") val osVersion: String
)
