package com.nugget.tracking.web

import com.nugget.tracking.MutableClock
import com.nugget.tracking.http.TrackingLogSender
import com.nugget.tracking.model.TrackingLog
import com.nugget.tracking.rate.FixedWindowRateLimiter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Instant
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse

class TrackingFilterTest {

    private lateinit var clock: MutableClock
    private lateinit var limiter: FixedWindowRateLimiter
    private lateinit var capturedLogs: MutableList<TrackingLog>
    private lateinit var filter: TrackingFilter

    @BeforeEach
    fun setup() {
        clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))
        limiter = FixedWindowRateLimiter(1, clock)
        capturedLogs = mutableListOf()
        val webClient = WebClient.builder()
            .exchangeFunction(ExchangeFunction {
                Mono.just(ClientResponse.create(HttpStatus.ACCEPTED).build())
            })
            .build()
        val sender = object : TrackingLogSender(webClient, "") {
            override fun send(log: TrackingLog) {
                capturedLogs.add(log)
            }
        }
        filter = TrackingFilter(
            serviceName = "sample-service",
            rateLimiter = limiter,
            logSender = sender,
            clock = clock
        )
    }

    @Test
    fun `captures request and response metrics`() {
        val request = MockHttpServletRequest("POST", "/widgets")
        request.setContent("payload".toByteArray(StandardCharsets.UTF_8))
        request.contentType = "application/json"

        val response = MockHttpServletResponse()
        val chain = FilterChain { _, resp ->
            val httpResp = resp as HttpServletResponse
            httpResp.status = HttpServletResponse.SC_CREATED
            httpResp.contentType = "application/json"
            httpResp.writer.write("{\"ok\":true}")
        }

        filter.doFilter(request, response, chain)

        assertEquals(1, capturedLogs.size)
        val log = capturedLogs.first()
        assertEquals("sample-service", log.serviceName)
        assertEquals("/widgets", log.endpoint)
        assertEquals("POST", log.method)
        assertEquals(7, log.requestSize)
        assertEquals(11, log.responseSize)
        assertEquals(201, log.statusCode)
        assertFalse(log.rateLimitHit)
    }

    @Test
    fun `marks rate limit exceed when threshold hit`() {
        val request = MockHttpServletRequest("GET", "/ping")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, resp ->
            (resp as HttpServletResponse).status = HttpServletResponse.SC_OK
        }

        filter.doFilter(request, response, chain)
        assertFalse(capturedLogs.last().rateLimitHit)

        val secondResponse = MockHttpServletResponse()
        filter.doFilter(request, secondResponse, chain)
        assertTrue(capturedLogs.last().rateLimitHit)
    }
}
