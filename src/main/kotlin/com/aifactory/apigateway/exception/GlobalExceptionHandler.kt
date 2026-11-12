package com.aifactory.apigateway.exception

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Component
@Order(-1)
class GlobalExceptionHandler : ErrorWebExceptionHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        val path = exchange.request.uri.path

        return when (ex) {
            is SecurityException -> {
                logger.warn("Security exception for path: $path - ${ex.message}")
                response.statusCode = HttpStatus.UNAUTHORIZED
                val errorResponse = """{"error": "Unauthorized", "message": "${escapeJson(ex.message)}"}"""
                writeResponse(response, errorResponse)
            }
            is ResponseStatusException -> {
                logger.warn("Response status exception for path: $path - ${ex.statusCode} - ${ex.reason}")
                response.statusCode = ex.statusCode
                val errorResponse = """{"error": "${ex.statusCode}", "message": "${escapeJson(ex.reason)}"}"""
                writeResponse(response, errorResponse)
            }
            is IllegalArgumentException -> {
                logger.warn("Illegal argument exception for path: $path - ${ex.message}")
                response.statusCode = HttpStatus.BAD_REQUEST
                val errorResponse = """{"error": "Bad Request", "message": "${escapeJson(ex.message)}"}"""
                writeResponse(response, errorResponse)
            }
            else -> {
                logger.error("Unhandled exception for path: $path", ex)
                response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                val errorResponse = """{"error": "Internal Server Error", "message": "An unexpected error occurred"}"""
                writeResponse(response, errorResponse)
            }
        }
    }

    private fun writeResponse(response: ServerHttpResponse, body: String): Mono<Void> {
        return try {
            response.headers.contentType = MediaType.APPLICATION_JSON
            val buffer: DataBuffer = response.bufferFactory().wrap(body.toByteArray(StandardCharsets.UTF_8))
            response.writeWith(Mono.just(buffer))
        } catch (e: Exception) {
            logger.error("Error writing error response", e)
            Mono.empty()
        }
    }

    private fun escapeJson(message: String?): String {
        return message?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: "Unknown error"
    }
}