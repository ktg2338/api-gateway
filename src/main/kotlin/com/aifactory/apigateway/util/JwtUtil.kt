package com.aifactory.apigateway.util

import com.aifactory.apigateway.config.JwtProperties
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil(
    private val jwtProperties: JwtProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtUtil::class.java)
    }

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    fun getClaimsFromToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            when (e) {
                is ExpiredJwtException -> {
                    logger.warn("JWT token is expired: ${e.message}")
                    null
                }

                is UnsupportedJwtException -> {
                    logger.warn("JWT token is unsupported: ${e.message}")
                    null
                }

                is MalformedJwtException -> {
                    logger.warn("JWT token is malformed: ${e.message}")
                    null
                }

                is IllegalArgumentException -> {
                    logger.warn("JWT token compact of handler are invalid: ${e.message}")
                    null
                }

                else -> {
                    logger.error("JWT token validation error: ${e.message}", e)
                    null
                }
            }
        }
    }

    fun getUsernameFromToken(token: String): String? {
        val claims = getClaimsFromToken(token)
        return claims?.subject
    }


    fun getRolesFromToken(token: String): List<String> {
        val claims = getClaimsFromToken(token)
        @Suppress("UNCHECKED_CAST")
        return claims?.get("roles") as? List<String> ?: emptyList()
    }


    fun isTokenValid(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            claims != null && !isTokenExpired(claims)
        } catch (_: Exception) {
            false
        }
    }

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }

}