package com.app.idisplaynew.data.repository

import com.app.idisplaynew.data.model.LoginPayload
import com.app.idisplaynew.data.model.LoginResponse
import com.app.idisplaynew.data.remote.ApiEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
            requestTimeoutMillis = 30_000L
            connectTimeoutMillis = 15_000L
            socketTimeoutMillis = 30_000L
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
}