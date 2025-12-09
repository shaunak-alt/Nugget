package com.nugget.tracking.config

import com.nugget.tracking.http.TrackingLogSender
import com.nugget.tracking.rate.FixedWindowRateLimiter
import com.nugget.tracking.web.TrackingFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import io.netty.channel.ChannelOption

@AutoConfiguration
@ConditionalOnClass(WebClient::class)
@EnableConfigurationProperties(TrackingClientProperties::class)
class TrackingClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun trackingWebClient(properties: TrackingClientProperties): WebClient {
        val collector = properties.collector
        val tcpClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, collector.connectTimeout.toMillis().toInt())
        val httpClient = HttpClient.from(tcpClient)
            .responseTimeout(collector.writeTimeout)
        return WebClient.builder()
            .baseUrl(collector.baseUrl)
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { config ->
                        config.defaultCodecs().maxInMemorySize(512 * 1024)
                    }
                    .build()
            )
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun trackingLogSender(webClient: WebClient, properties: TrackingClientProperties): TrackingLogSender {
        return TrackingLogSender(webClient, properties.collector.endpoint)
    }

    @Bean
    @ConditionalOnMissingBean
    fun trackingRateLimiter(properties: TrackingClientProperties): FixedWindowRateLimiter? {
        val rateLimiterConfig = properties.rateLimiter
        return if (rateLimiterConfig.enabled) {
            FixedWindowRateLimiter(rateLimiterConfig.requestsPerSecond)
        } else {
            null
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun trackingFilter(
        properties: TrackingClientProperties,
        rateLimiter: FixedWindowRateLimiter?,
        logSender: TrackingLogSender
    ): TrackingFilter {
        return TrackingFilter(
            serviceName = properties.serviceName,
            rateLimiter = rateLimiter,
            logSender = logSender
        )
    }

    @Bean
    @ConditionalOnMissingBean(name = ["trackingFilterRegistration"])
    fun trackingFilterRegistration(filter: TrackingFilter): FilterRegistrationBean<TrackingFilter> {
        return FilterRegistrationBean<TrackingFilter>(filter).apply {
            setName("trackingFilter")
            order = Ordered.HIGHEST_PRECEDENCE + 50
        }
    }
}
