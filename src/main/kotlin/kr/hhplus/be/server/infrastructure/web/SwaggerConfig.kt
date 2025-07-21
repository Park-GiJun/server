package kr.hhplus.be.server.infrastructure.web

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "콘서트 예약 서비스 API",
        version = "1.0.0",
        description = "콘서트 예약 서비스의 Mock API 문서입니다."
    ),
    servers = [Server(url = "http://localhost:8080", description = "Local server")]
)
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("대기열 토큰을 Bearer 형식으로 입력")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }
}