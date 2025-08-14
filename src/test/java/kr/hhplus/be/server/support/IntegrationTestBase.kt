package kr.hhplus.be.server.support

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<Nothing> = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withReuse(true)
            start() // 명시적으로 시작
        }

        @Container
        @JvmStatic
        val redisContainer: GenericContainer<Nothing> = GenericContainer<Nothing>(DockerImageName.parse("redis:7.2-alpine")).apply {
            withExposedPorts(6379)
            withCommand("redis-server", "--appendonly", "no")
            withReuse(true)
            start() // 명시적으로 시작
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            // 컨테이너가 시작되었는지 확인
            if (!mysqlContainer.isRunning) {
                mysqlContainer.start()
            }
            if (!redisContainer.isRunning) {
                redisContainer.start()
            }

            // MySQL 설정
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }

            // Redis 설정
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }

            // 시스템 프로퍼티로도 설정 (Redisson에서 사용)
            System.setProperty("spring.data.redis.host", redisContainer.host)
            System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString())
        }
    }
}