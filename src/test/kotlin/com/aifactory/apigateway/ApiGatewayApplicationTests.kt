package com.aifactory.apigateway

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["jwt.secret=test-only-secret-key-not-for-production-use"])
class ApiGatewayApplicationTests {

    @Test
    fun contextLoads() {
    }

}
