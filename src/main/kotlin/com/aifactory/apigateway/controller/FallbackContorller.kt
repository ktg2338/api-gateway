package com.aifactory.apigateway.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/fallback")
@Tag(name = "Fallback", description = "서비스 장애 시 대체 응답 API")
class FallbackController {

    @GetMapping("/auth")
    fun authFallback(): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf(
                "message" to "인증/인가 서비스를 일시적으로 사용할 수 없습니다",
                "status" to "Circuit Breaker가 활성화되었습니다"
            ))
    }

    @GetMapping("/competition")
    fun competitionFallback(): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf(
                "message" to "경진대회 서비스를 일시적으로 사용할 수 없습니다",
                "status" to "Circuit Breaker가 활성화되었습니다"
            ))
    }
}