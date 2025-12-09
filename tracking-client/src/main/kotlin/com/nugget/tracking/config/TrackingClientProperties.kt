package com.nugget.tracking.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties(prefix = "tracking")
class TrackingClientProperties(
    @DefaultValue("unknown-service")
    var serviceName: String = "unknown-service",
    var collector: CollectorProperties = CollectorProperties(),
    var rateLimiter: RateLimiterProperties = RateLimiterProperties()
) {
    class CollectorProperties(
        @DefaultValue("http://localhost:8080")
        var baseUrl: String = "http://localhost:8080",
        @DefaultValue("/logs")
        var endpoint: String = "/logs",
        @DefaultValue("PT2S")
        var connectTimeout: Duration = Duration.ofSeconds(2),
        @DefaultValue("PT5S")
        var writeTimeout: Duration = Duration.ofSeconds(5)
    )

    class RateLimiterProperties(
        @DefaultValue("true")
        var enabled: Boolean = true,
        @DefaultValue("100")
        var requestsPerSecond: Long = 100
    )
}
