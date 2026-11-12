package com.aifactory.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*

@Component
class StatelessCsrfFilter : WebFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(StatelessCsrfFilter::class.java)

        private const val CSRF_COOKIE_NAME = "XSRF-TOKEN"
        private const val CSRF_HEADER_NAME = "X-XSRF-TOKEN"

        private val EXCLUDED_PATHS = listOf(
            "/auth/login",
            "/auth/signup",
            "/oauth2",
            "/login/oauth2/code",
            "/swagger-ui",
            "/actuator",
            "/v3/api-docs",
            "/auth/api-docs",
            "/competition/api-docs",
            "/csrf-token",
            "/fallback",
            "/competition/test"
        )

        private val SAFE_METHODS = listOf(
            HttpMethod.GET,
            HttpMethod.HEAD,
            HttpMethod.OPTIONS
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return try {
            val request = exchange.request
            val path = request.uri.path

            // CSRF 제외 경로 체크
            if (EXCLUDED_PATHS.any { path.startsWith(it) }) {
                return chain.filter(exchange)
            }

            // Safe 메소드는 CSRF 검증 스킵
            if (request.method in SAFE_METHODS) {
                return ensureCsrfCookie(exchange, chain)
            }

            // CSRF 토큰 검증
            val cookieToken = request.cookies[CSRF_COOKIE_NAME]?.firstOrNull()?.value
            val headerToken = request.headers[CSRF_HEADER_NAME]?.firstOrNull()

            // Double Submit Cookie 패턴: 쿠키와 헤더의 토큰이 일치해야 함
            if (cookieToken == null || headerToken == null || cookieToken != headerToken) {
                logger.warn("CSRF token validation failed for path: $path (method: ${request.method})")
                exchange.response.statusCode = HttpStatus.FORBIDDEN
                return exchange.response.setComplete()
            }

            chain.filter(exchange)
        } catch (e: Exception) {
            logger.error("CSRF filter error for path: ${exchange.request.uri.path}", e)
            exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            exchange.response.setComplete()
        }
    }
    
    private fun ensureCsrfCookie(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return try {
            val request = exchange.request
            val existingToken = request.cookies[CSRF_COOKIE_NAME]?.firstOrNull()?.value

            if (existingToken == null) {
                // 새 CSRF 토큰 생성
                val newToken = UUID.randomUUID().toString()
                val cookie = ResponseCookie.from(CSRF_COOKIE_NAME, newToken)
                    .httpOnly(false)  // JavaScript에서 읽을 수 있어야 함
                    .secure(false)    // HTTPS에서만 사용하려면 true
                    .path("/")
                    .sameSite("None")
                    .build()

                exchange.response.headers.add("Set-Cookie", cookie.toString())
                logger.debug("Generated new CSRF token for path: ${request.uri.path}")
            }

            chain.filter(exchange)
        } catch (e: Exception) {
            logger.error("Error ensuring CSRF cookie for path: ${exchange.request.uri.path}", e)
            exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            exchange.response.setComplete()
        }
    }
}