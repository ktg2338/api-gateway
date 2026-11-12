package com.aifactory.apigateway.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("API Gateway")
                    .description("Microservices API Gateway")
                    .version("1.0.0")
            )
            .servers(listOf(
                Server()
                    .url("")
                    .description("Current Server")
            ))
    }

    @Bean
    fun gatewayGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("gateway")
            .pathsToMatch("/**")
            .build()
    }
}