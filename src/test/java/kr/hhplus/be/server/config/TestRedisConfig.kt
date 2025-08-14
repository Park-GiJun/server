package kr.hhplus.be.server.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@TestConfiguration
@Profile("test")
class TestRedisConfig {

    @Bean
    @Primary
    fun testRedisConnectionFactory(): RedisConnectionFactory {
        // TestContainers에서 제공하는 Redis 정보 사용
        val factory = LettuceConnectionFactory(
            System.getProperty("spring.data.redis.host", "localhost"),
            System.getProperty("spring.data.redis.port", "6379").toInt()
        )
        factory.afterPropertiesSet()
        return factory
    }

    @Bean
    @Primary
    fun testRedisTemplate(testRedisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = testRedisConnectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean
    @Primary
    fun testStringRedisTemplate(testRedisConnectionFactory: RedisConnectionFactory): StringRedisTemplate {
        val template = StringRedisTemplate()
        template.connectionFactory = testRedisConnectionFactory
        template.afterPropertiesSet()
        return template
    }

    @Bean
    @Primary
    fun testRedissonClient(): RedissonClient {
        val config = Config()
        val redisHost = System.getProperty("spring.data.redis.host", "localhost")
        val redisPort = System.getProperty("spring.data.redis.port", "6379")

        config.useSingleServer()
            .setAddress("redis://$redisHost:$redisPort")
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5)
            .setConnectTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500)
            .setDatabase(0)
            .setDnsMonitoringInterval(-1) // DNS 모니터링 비활성화

        return Redisson.create(config)
    }
}