package com.app.idisplaynew.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("result") val result: Result? = null,
    @SerialName("errors") val errors: Map<String, List<String>> = emptyMap()
) {
    @Serializable
    data class Result(
        @SerialName("deviceId") val deviceId: Int = 0,
        @SerialName("token") val token: String = "",
        @SerialName("refreshToken") val refreshToken: String = "",
        @SerialName("tokenExpiry") val tokenExpiry: String = "",
        @SerialName("locationId") val locationId: Int = 0,
        @SerialName("locationName") val locationName: String = "",
        @SerialName("clientId") val clientId: Int = 0
    )
}
