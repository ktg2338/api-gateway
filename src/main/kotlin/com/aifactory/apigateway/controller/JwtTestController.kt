package com.aifactory.apigateway.controller

import com.aifactory.apigateway.util.JwtUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*
import kotlin.collections.firstOrNull

@Profile("!prod")
@RestController
@RequestMapping("/test")
class JwtTestController (
    private val jwtUtil: JwtUtil
){

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @PostMapping("/generate-token")
    fun generateToken(): ResponseEntity<Map<String, String>> {
        val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
        
        val token = Jwts.builder()
            .subject("qweqweqwe")
            .claim("roles", listOf("USER"))
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(secretKey)
            .compact()

        val cookie = ResponseCookie.from("accessToken", token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(3600)
            .build()

        return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(mapOf("token" to token, "message" to "Token generated successfully"))
    }

    @GetMapping("/protected")
    fun protectedEndpoint(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "This is a protected endpoint"))
    }

    @GetMapping("/info")
    fun getUserInfo(exchange: ServerWebExchange): Mono<ResponseEntity<out Map<String, Any?>?>?> {
        val token = exchange.request.cookies["accessToken"]?.firstOrNull()?.value

        return Mono.just(
            if (token != null && jwtUtil.isTokenValid(token)) {
                val username = jwtUtil.getUsernameFromToken(token)
                val roles = jwtUtil.getRolesFromToken(token)

                ResponseEntity.ok(
                    mapOf(
                        "username" to username,
                        "roles" to roles,
                        "tokenValid" to true,
                        "message" to "User information retrieved successfully"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf(
                        "error" to "Invalid or missing token",
                        "tokenValid" to false,
                        "message" to "Please login to access this resource"
                    )
                )
            }
        )
    }
}