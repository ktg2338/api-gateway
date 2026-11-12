package com.aifactory.apigateway

import com.aifactory.apigateway.config.JwtProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import reactor.core.publisher.Mono

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class)
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
