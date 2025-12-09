package com.nugget.tracking.model

import java.time.Instant

data class TrackingLog(
    val serviceName: String,
    val endpoint: String,
    val method: String,
    val requestSize: Long,
    val responseSize: Long,
    val statusCode: Int,
    val timestamp: Instant,
    val latencyMs: Long,
    val rateLimitHit: Boolean,
    val meta: Map<String, Any?>? = null
)
