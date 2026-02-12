package com.app.idisplaynew.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeviceCommandsResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("result") val result: CommandsResult? = null,
    @SerialName("errors") val errors: Map<String, List<String>> = emptyMap()
) {
    @Serializable
    data class CommandsResult(
        @SerialName("commands") val commands: List<Command> = emptyList()
    )

    @Serializable
    data class Command(
        @SerialName("commandId") val commandId: Int,
        @SerialName("commandType") val commandType: String = "",
        @SerialName("payload") val payload: String? = null,
        @SerialName("priority") val priority: Int = 0,
        @SerialName("createdAt") val createdAt: String? = null,
        @SerialName("expiresAt") val expiresAt: String? = null
    )
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AckCommandPayload(
    @SerialName("commandId") val commandId: Int,
    @SerialName("status") val status: String = "success",
    @SerialName("result") val result: String = ""
)
