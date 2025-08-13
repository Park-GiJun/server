package kr.hhplus.be.server.infrastructure.config.domain

import kr.hhplus.be.server.domain.reservation.ReservationDomainService
import kr.hhplus.be.server.domain.queue.QueueTokenDomainService
import kr.hhplus.be.server.domain.payment.PaymentDomainService
import kr.hhplus.be.server.domain.concert.ConcertDomainService
import kr.hhplus.be.server.domain.users.UserDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfig {

    @Bean
    fun reservationDomainService(): ReservationDomainService {
        return ReservationDomainService()
    }

    @Bean
    fun queueTokenDomainService(): QueueTokenDomainService {
        return QueueTokenDomainService()
    }

    @Bean
    fun paymentDomainService(): PaymentDomainService {
        return PaymentDomainService()
    }

    @Bean
    fun concertDomainService(): ConcertDomainService {
        return ConcertDomainService()
    }

    @Bean
    fun userDomainService(): UserDomainService {
        return UserDomainService()
    }
}