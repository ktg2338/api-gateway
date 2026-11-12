package com.aifactory.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class GlobalRateLimitFilter(
    private val redisRateLimiter: RateLimiter<*>,
    private val keyResolver: KeyResolver
) : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(GlobalRateLimitFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return keyResolver.resolve(exchange)
            .flatMap { key ->
                redisRateLimiter.isAllowed(key, null)
                    .flatMap { response ->
                        if (response.isAllowed) {
                            logger.debug("Request allowed for key: $key")
                            response.headers["X-RateLimit-Remaining-RequestRateLimiter"]?.firstOrNull()?.let { remaining ->
                                exchange.response.headers.add("X-RateLimit-Remaining", remaining.toString())
                            }
                            chain.filter(exchange)
                        } else {
                            logger.warn("Rate limit exceeded for key: $key")
                            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                            val reset = response.headers["X-RateLimit-Reset"]?.firstOrNull()?.toString() ?: "1"
                            exchange.response.headers.add("X-RateLimit-Retry-After-Seconds", reset)
                            exchange.response.setComplete()
                        }
                    }
                    .onErrorResume { error ->
                        logger.error("Rate limiter error for path: ${exchange.request.uri.path}", error)
                        // Rate limiter 오류 시 요청 허용 (Fail-open 방식)
                        chain.filter(exchange)
                    }
            }
            .onErrorResume { error ->
                logger.error("Key resolver error for path: ${exchange.request.uri.path}", error)
                // Key resolver 오류 시 요청 허용 (Fail-open 방식)
                chain.filter(exchange)
            }
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}