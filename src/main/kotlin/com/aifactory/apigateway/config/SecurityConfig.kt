package com.aifactory.apigateway.config

import com.aifactory.apigateway.filter.StatelessCsrfFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    //spring security 기본값이 세션 로그인!!
    @Bean
    fun securityFilterChain(
        http: ServerHttpSecurity,
        statelessCsrfFilter: StatelessCsrfFilter
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() } // Spring Security의 기본 CSRF 비활성화, 커스텀 필터 사용
            .httpBasic { it.disable() } // HTTP Basic 인증 비활성화
            .formLogin { it.disable() } // 폼 로그인 비활성화
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()) // 세션 비활성화
            .addFilterBefore(statelessCsrfFilter, SecurityWebFiltersOrder.CSRF) // 커스텀 CSRF 필터
            .build()
    }
}
