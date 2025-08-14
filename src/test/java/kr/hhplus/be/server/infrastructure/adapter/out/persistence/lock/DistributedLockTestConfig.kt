package kr.hhplus.be.server.infrastructure.adapter.out.persistence.lock

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class DistributedLockTestConfig {

    companion object {
        private const val REDIS_PORT = 6379
    }

    @Bean
    fun redisContainer(): GenericContainer<*> {
        val redis = GenericContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server")
        redis.start()
        return redis
    }

    @Bean
    @Primary
    fun testRedissonClient(redisContainer: GenericContainer<*>): RedissonClient {
        val config = Config()
        val redisUrl = "redis://${redisContainer.host}:${redisContainer.getMappedPort(REDIS_PORT)}"

        config.useSingleServer()
            .setAddress(redisUrl)
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5)
            .setConnectTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500)

        return Redisson.create(config)
    }
}