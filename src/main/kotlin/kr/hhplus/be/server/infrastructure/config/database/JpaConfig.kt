package kr.hhplus.be.server.infrastructure.config.database

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories("kr.hhplus.be.server.infrastructure.adapter.out.persistence")
class JpaConfig {
    @Bean
    fun transactionManager(): JpaTransactionManager {
        return JpaTransactionManager()
    }
}