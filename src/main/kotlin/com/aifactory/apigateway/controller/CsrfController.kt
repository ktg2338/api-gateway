package com.aifactory.apigateway.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
class CsrfController {

    companion object {
        private val logger = LoggerFactory.getLogger(CsrfController::class.java)
    }

    @GetMapping("/api/csrf-token")
    fun getCsrfToken(exchange: ServerWebExchange): Mono<Map<String, String>> {
        val existingToken = exchange.request.cookies["XSRF-TOKEN"]
            ?.firstOrNull()
            ?.value

        val token = if (existingToken == null) {
            // 새 토큰 생성 및 쿠키 설정
            val newToken = UUID.randomUUID().toString()
            val cookie = ResponseCookie.from("XSRF-TOKEN", newToken)
                .httpOnly(false)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(3600)
                .build()


            exchange.response.addCookie(cookie)

            newToken
        } else {
            existingToken
        }

        return Mono.just(mapOf("token" to token))
    }
}