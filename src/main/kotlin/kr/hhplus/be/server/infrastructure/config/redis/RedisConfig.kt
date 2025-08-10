package kr.hhplus.be.server.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis 설정 클래스
 */
@Configuration
class RedisConfig {

    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var host: String

    @Value("\${spring.data.redis.port:6379}")
    private var port: Int = 6379

    @Value("\${spring.data.redis.password:redis123}")
    private lateinit var password: String

    /**
     * Redis 연결 팩토리 Bean 생성
     * - Lettuce 클라이언트를 사용한 Redis 연결 설정
     */
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisConfiguration = RedisStandaloneConfiguration().apply {
            hostName = host
            port = this@RedisConfig.port
            setPassword(password)
        }
        return LettuceConnectionFactory(redisConfiguration)
    }

    /**
     * RedisTemplate Bean 생성
     * - 문자열 키와 JSON 직렬화 설정
     */
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            setConnectionFactory(connectionFactory)

            // Key Serializer: String으로 설정
            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()

            // Value Serializer: JSON으로 설정
            valueSerializer = GenericJackson2JsonRedisSerializer()
            hashValueSerializer = GenericJackson2JsonRedisSerializer()

            // 트랜잭션 지원 활성화
            setEnableTransactionSupport(true)

            afterPropertiesSet()
        }
    }
}