package com.app.idisplaynew.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("result") val result: HeartbeatResult? = null,
    @SerialName("errors") val errors: Map<String, List<String>>? = null
) {
    @Serializable
    data class HeartbeatResult(
        @SerialName("success") val success: Boolean = false,
        @SerialName("hasPendingCommands") val hasPendingCommands: Boolean = false,
        @SerialName("hasScheduleUpdate") val hasScheduleUpdate: Boolean = false,
        @SerialName("hasEmergencyOverride") val hasEmergencyOverride: Boolean = false,
        @SerialName("serverTime") val serverTime: String? = null
    )
}
