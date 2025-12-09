package com.nugget.tracking.http

import com.nugget.tracking.model.TrackingLog
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Sends tracking logs to the collector service asynchronously.
 */
open class TrackingLogSender(
    private val webClient: WebClient,
    private val endpointPath: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    open fun send(log: TrackingLog) {
        webClient.post()
            .uri(endpointPath)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(log))
            .retrieve()
            .bodyToMono(Void::class.java)
            .onErrorResume { error ->
                logger.warn("Failed to send tracking log", error)
                Mono.empty()
            }
            .subscribe()
    }
}
