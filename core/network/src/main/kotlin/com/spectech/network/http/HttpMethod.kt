package com.spectech.network.http

import io.ktor.http.HttpMethod as KtorMethod

enum class HttpMethod(val wire: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE");

    fun toKtor(): KtorMethod = when (this) {
        GET -> KtorMethod.Get
        POST -> KtorMethod.Post
        PUT -> KtorMethod.Put
        PATCH -> KtorMethod.Patch
        DELETE -> KtorMethod.Delete
    }
}
