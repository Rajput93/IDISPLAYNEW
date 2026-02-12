package com.app.idisplaynew.data.repository

import com.app.idisplaynew.data.model.AckCommandPayload
import com.app.idisplaynew.data.model.DeviceCommandsResponse
import com.app.idisplaynew.data.model.LoginPayload
import com.app.idisplaynew.data.model.LoginResponse
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import com.app.idisplaynew.data.remote.ApiEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object Repository {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpRedirect)
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000L
            connectTimeoutMillis = 60_000L
            socketTimeoutMillis = 60_000L
        }
    }

    private fun buildUrl(baseUrl: String, endpoint: String, vararg queryParams: Pair<String, String>): String {
        val normalizedBase = baseUrl.trimEnd('/') + "/"
        val fullUrl = "${normalizedBase}$endpoint"
        val query = queryParams.joinToString("&") { "${it.first}=${it.second}" }
        return if (query.isNotEmpty()) "$fullUrl?$query" else fullUrl
    }

    suspend fun register(baseUrl: String, request: LoginPayload): LoginResponse {
        val url = buildUrl(baseUrl, ApiEndpoints.LOGIN)
        return httpClient.post(url) {
            contentType(ContentType.Application.Json) // Set content type to JSON
            setBody(request) // Serialize LoginRequest into JSON
        }.body()
    }

    suspend fun getScheduleCurrent(baseUrl: String, token: String): ScheduleCurrentResponse {
        val url = buildUrl(baseUrl, ApiEndpoints.SCHEDULE_CURRENT)
        return httpClient.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            timeout {
                connectTimeoutMillis = 60_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }.body()
    }

    suspend fun getDeviceCommands(baseUrl: String, token: String): DeviceCommandsResponse {
        val url = buildUrl(baseUrl, ApiEndpoints.DEVICE_COMMANDS)
        return httpClient.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            timeout {
                connectTimeoutMillis = 60_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }.body()
    }

    suspend fun ackCommand(baseUrl: String, token: String, payload: AckCommandPayload): Unit {
        val url = buildUrl(baseUrl, ApiEndpoints.DEVICE_COMMANDS_ACK)
        httpClient.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
            timeout {
                connectTimeoutMillis = 60_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}