package com.aifactory.apigateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class RateLimitConfig {

    @Bean
    fun redisRateLimiter(): RedisRateLimiter {
        return RedisRateLimiter(50, 100)
    }

    @Bean
    fun keyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            Mono.just(
                exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            )
        }
    }
}