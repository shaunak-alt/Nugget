package com.nugget.tracking.web

import com.nugget.tracking.TrackingMetaContext
import com.nugget.tracking.http.TrackingLogSender
import com.nugget.tracking.model.TrackingLog
import com.nugget.tracking.rate.FixedWindowRateLimiter
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.time.Instant
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

class TrackingFilter(
    private val serviceName: String,
    private val rateLimiter: FixedWindowRateLimiter?,
    private val logSender: TrackingLogSender,
    private val clock: Clock = Clock.systemUTC()
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = wrapRequest(request)
        val wrappedResponse = wrapResponse(response)
        val startTime = clock.instant()
        val startNanos = System.nanoTime()

        var rateLimitExceeded = false
        if (rateLimiter != null) {
            rateLimitExceeded = rateLimiter.hitAndCheckLimit()
        }

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val latencyMs = ((System.nanoTime() - startNanos) / 1_000_000)
            val log = buildLog(wrappedRequest, wrappedResponse, startTime, latencyMs, rateLimitExceeded)
            logSender.send(log)
            TrackingMetaContext.clear()
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun buildLog(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        timestamp: Instant,
        latencyMs: Long,
        rateLimitExceeded: Boolean
    ): TrackingLog {
        val endpoint = request.requestURI ?: "unknown"
        val method = request.method ?: "UNKNOWN"
        val requestSize = resolveRequestSize(request)
        val responseSize = resolveResponseSize(response)
        val status = response.status
        val meta = TrackingMetaContext.snapshot()

        if (log.isTraceEnabled) {
            log.trace(
                "tracking-log endpoint={} method={} status={} latencyMs={} rlExceeded={}",
                endpoint,
                method,
                status,
                latencyMs,
                rateLimitExceeded
            )
        }

        return TrackingLog(
            serviceName = serviceName,
            endpoint = endpoint,
            method = method,
            requestSize = requestSize,
            responseSize = responseSize,
            statusCode = status,
            timestamp = timestamp,
            latencyMs = latencyMs,
            rateLimitHit = rateLimitExceeded,
            meta = meta
        )
    }

    private fun resolveRequestSize(request: ContentCachingRequestWrapper): Long {
        val cached = request.contentAsByteArray
        if (cached.isNotEmpty()) {
            return cached.size.toLong()
        }
        val contentLength = request.contentLengthLong
        return if (contentLength >= 0) contentLength else 0
    }

    private fun resolveResponseSize(response: ContentCachingResponseWrapper): Long {
        val buffered = response.contentAsByteArray
        if (buffered.isNotEmpty()) {
            return buffered.size.toLong()
        }
        val headerSize = response.getHeader("Content-Length")?.toLongOrNull()
        return headerSize ?: 0
    }

    private fun wrapRequest(request: HttpServletRequest): ContentCachingRequestWrapper {
        return if (request is ContentCachingRequestWrapper) {
            request
        } else {
            ContentCachingRequestWrapper(request)
        }
    }

    private fun wrapResponse(response: HttpServletResponse): ContentCachingResponseWrapper {
        return if (response is ContentCachingResponseWrapper) {
            response
        } else {
            ContentCachingResponseWrapper(response)
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI ?: ""
        return INTERNAL_ENDPOINTS.any { path.startsWith(it) }
    }

    companion object {
        private val INTERNAL_ENDPOINTS = setOf("/actuator", "/favicon", "/error")
    }
}
