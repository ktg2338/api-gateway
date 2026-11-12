package com.aifactory.apigateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig {

    @Value("\${services.auth.uri}")
    lateinit var authServiceUri: String
    @Value("\${services.competition.uri}")
    lateinit var competitionServiceUri: String

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
//            정확한 경로 + 와일드 카드 섞어서 사용하면 동작 안됨

//            auth-service
            .route("auth-service-public") { r ->
                r.path("/auth/login", "/auth/signup")
                    .filters { f ->
                        f.circuitBreaker { config ->
                            config
                                .setName("authCircuitBreaker")
                                .setFallbackUri("forward:/fallback/auth")
                        }
                    }
                    .uri(authServiceUri)
            }
            .route("auth-swagger") { r ->
                r.path("/auth/swagger-ui.html", "/auth/api-docs")
                    .filters { f ->
                        f.stripPrefix(1)
                        f.circuitBreaker { config ->
                            config
                                .setName("authCircuitBreaker")
                                .setFallbackUri("forward:/fallback/auth")
                        }
                    }
                    .uri(authServiceUri)
            }
            .route("auth-protected") { r ->
                r.path("/auth/**", "/oauth2/**", "/login/oauth2/**")
                    .filters { f ->
                        f.circuitBreaker { config ->
                            config
                                .setName("authCircuitBreaker")
                                .setFallbackUri("forward:/fallback/auth")
                        }
                    }
                    .uri(authServiceUri)
            }

//          경진대회
            .route("competition-swagger") { r ->
                r.path("/competition/swagger-ui.html", "/competition/api-docs")
                    .filters { f ->
                        f.stripPrefix(1)
                        f.circuitBreaker { config ->
                            config
                                .setName("competitionCircuitBreaker")
                                .setFallbackUri("forward:/fallback/competition")
                        }
                    }
                    .uri(competitionServiceUri)
            }
            .route("competition-protected") { r ->
                r.path("/competition/**")
                    .filters { f ->
                        f.circuitBreaker { config ->
                            config
                                .setName("competitionCircuitBreaker")
                                .setFallbackUri("forward:/fallback/competition")
                        }
                    }
                    .uri(competitionServiceUri)
            }
            .build()
    }

}