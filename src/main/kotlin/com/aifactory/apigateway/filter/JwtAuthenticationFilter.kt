package com.aifactory.apigateway.filter

import com.aifactory.apigateway.util.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : GlobalFilter, Ordered {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request

        if (!isAuthRequired(request)) {
            return chain.filter(exchange)
        }

        val token = getTokenFromRequest(request)

        if (token == null) {
            val response = exchange.response
            response.statusCode = HttpStatus.UNAUTHORIZED
            return response.setComplete()
        }

        return Mono.fromCallable { jwtUtil.isTokenValid(token) }
            .flatMap { isValid ->
                if (!isValid) {
                    logger.warn("Invalid JWT token for path: ${request.uri.path}")
                    val response = exchange.response
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    response.setComplete()
                } else {
                    processValidToken(exchange, chain, token)
                }
            }
            .onErrorResume { throwable ->
                // JWT 처리 중 오류 발생
                logger.error("JWT validation error for path: ${request.uri.path}", throwable)
                val response = exchange.response
                response.statusCode = HttpStatus.UNAUTHORIZED
                response.setComplete()
            }
    }

    private fun processValidToken(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
        token: String
    ): Mono<Void> {
        return try {
            val username = jwtUtil.getUsernameFromToken(token)
            val roles = jwtUtil.getRolesFromToken(token)

            // Spring Security Authentication 생성
            val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
            val authentication = UsernamePasswordAuthenticationToken(username, null, authorities)


            // 다운스트림 서비스에 사용자 정보 전달
            val modifiedRequest = exchange.request.mutate()
                .headers { headers ->
                    if (exchange.request.uri.path.startsWith("/auth")) {
                        headers.set("X-User-Id", username)
                        headers.set("X-User-Roles", roles.joinToString(","))
                    } else {
                        headers.remove("Cookie")
                    }
                    // CSRF 관련 헤더 제거 (downstream 서비스에서 불필요)
                    headers.remove("X-XSRF-TOKEN")
                    headers.remove("x-xsrf-token")

                }
                .build()

            val modifiedExchange = exchange.mutate().request(modifiedRequest).build()

            // SecurityContext에 Authentication 설정하고 요청 처리
            chain.filter(modifiedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } catch (e: Exception) {
            logger.error("Error processing valid token for path: ${exchange.request.uri.path}", e)
            val response = exchange.response
            response.statusCode = HttpStatus.UNAUTHORIZED
            response.setComplete()
        }
    }

    private fun isAuthRequired(request: ServerHttpRequest): Boolean {
        val path = request.uri.path
        // 공개 경로는 인증 제외
        val publicPaths = listOf(
            "/auth/login",
            "/auth/signup",
            "/oauth2",
            "/login/oauth2/code",
            "/public",
            "/actuator",
            "/docs",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/csrf-token",
            "/auth/api-docs",
            "/competition/api-docs",
            "/fallback",
            "/competition/test"
        )
        return !publicPaths.any { path.startsWith(it) }
    }

    private fun getTokenFromRequest(request: ServerHttpRequest): String? {
        // HTTP-only 쿠키에서 JWT 토큰 읽기
        return try {
            request.cookies["accessToken"]?.firstOrNull()?.value
        } catch (e: Exception) {
            logger.error("Error extracting token from request cookies", e)
            null
        }
    }

    override fun getOrder(): Int {
        return -100
    }
}