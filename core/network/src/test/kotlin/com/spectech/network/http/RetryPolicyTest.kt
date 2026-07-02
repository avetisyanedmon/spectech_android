package com.spectech.network.http

import com.spectech.domain.error.ApiError
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Pins the retry policy of [buildHttpClient]: transient gateway failures are
 * retried for GET only. A POST must never be re-sent — the server may have
 * already processed the write (duplicate order/bid), so the failure has to
 * surface to the caller instead.
 */
class RetryPolicyTest {

    private lateinit var server: MockWebServer
    private lateinit var client: HttpClient

    private object NoSession : SessionProvider {
        override suspend fun authToken(): String? = null
        override suspend fun clearSession() = Unit
    }

    @BeforeEach fun setUp() {
        server = MockWebServer().apply { start() }
        client = buildHttpClient(
            clientId = "test-client",
            clientSecret = "test-secret",
            sessionProvider = NoSession,
        )
    }

    @AfterEach fun tearDown() {
        client.close()
        server.shutdown()
    }

    @Test fun `GET is retried after a 503 and succeeds on the second attempt`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(jsonOk())

        val response = client.get(server.url("/orders").toString())

        response.status.value shouldBe 200
        response.bodyAsText() shouldBe OK_BODY
        server.requestCount shouldBe 2
    }

    @Test fun `POST is not retried after a 503`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(jsonOk()) // Must never be consumed.

        val error = assertThrows<ApiError> {
            runBlocking {
                client.post(server.url("/orders").toString()) {
                    setBody(TextContent("""{"title":"dig"}""", ContentType.Application.Json))
                }
            }
        }

        error.statusCode shouldBe 503
        server.requestCount shouldBe 1
    }

    private fun jsonOk(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(OK_BODY)

    companion object {
        private const val OK_BODY = """{"success":true,"data":[]}"""
    }
}
