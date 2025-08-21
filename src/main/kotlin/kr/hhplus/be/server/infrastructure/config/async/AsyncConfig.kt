package kr.hhplus.be.server.infrastructure.config.async

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["queueTaskExecutor"])
    fun queueTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("Queue-")
        executor.initialize()
        return executor
    }

    @Bean(name = ["websocketTaskExecutor"])
    fun websocketTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 3
        executor.maxPoolSize = 6
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("WebSocket-")
        executor.initialize()
        return executor
    }

    @Bean(name = ["viewCountTaskExecutor"])
    fun viewCountTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("ViewCount-")
        executor.initialize()
        return executor
    }
}