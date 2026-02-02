package com.app.idisplaynew.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginPayload(
    val deviceId: String="",
    val name: String="",
    val ipAddress: String="",
    val resolution: String="",
    val orientation: String="",
    val appVersion: String="",
    val osVersion: String="",
    val registrationCode: String=""
)