package kr.hhplus.be.server.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.LocalDateTime
import java.util.*

@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
class TestJpaConfig {

    @Bean
    fun testDateTimeProvider(): DateTimeProvider {
        return DateTimeProvider { Optional.of(LocalDateTime.now()) }
    }
}