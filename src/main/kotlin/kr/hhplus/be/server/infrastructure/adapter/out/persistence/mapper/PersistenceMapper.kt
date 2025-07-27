package kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper

import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertDateJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatGradeJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.entity.PaymentJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.entity.QueueTokenJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity.UserJpaJpaEntity

object PersistenceMapper {

    fun toConcertDomain(entity: ConcertJpaEntity): Concert {
        return Concert(
            concertId = entity.concertId,
            concertName = entity.concertName,
            location = entity.location,
            description = entity.description ?: "",
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toConcertEntity(domain: Concert): ConcertJpaEntity {
        return ConcertJpaEntity(
            concertId = domain.concertId,
            concertName = domain.concertName,
            location = domain.location,
            description = domain.description
        )
    }

    fun toConcertDateDomain(entity: ConcertDateJpaEntity?): ConcertDate {
        return ConcertDate(
            concertDateId = entity.concertDateId,
            concertSession = entity.concertSession,
            concertId = entity.concertId,
            date = entity.date,
            isSoldOut = entity.isSoldOut,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toConcertDateEntity(domain: ConcertDate): ConcertDateJpaEntity {
        return ConcertDateJpaEntity(
            concertDateId = domain.concertDateId,
            concertSession = domain.concertSession,
            concertId = domain.concertId,
            date = domain.date,
            isSoldOut = domain.isSoldOut
        )
    }

    fun toConcertSeatDomain(entity: ConcertSeatJpaEntity): ConcertSeat {
        return ConcertSeat(
            concertSeatId = entity.concertSeatId,
            concertDateId = entity.concertDateId,
            seatNumber = entity.seatNumber,
            seatGrade = entity.seatGrade,
            seatStatus = entity.seatStatus,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toConcertSeatEntity(domain: ConcertSeat): ConcertSeatJpaEntity {
        return ConcertSeatJpaEntity(
            concertSeatId = domain.concertSeatId,
            concertDateId = domain.concertDateId,
            seatNumber = domain.seatNumber,
            seatGrade = domain.seatGrade,
            seatStatus = domain.seatStatus
        )
    }

    fun toConcertSeatGradeDomain(entity: ConcertSeatGradeJpaEntity): ConcertSeatGrade {
        return ConcertSeatGrade(
            concertSeatGradeId = entity.concertSeatGradeId,
            concertId = entity.concertId,
            seatGrade = entity.seatGrade,
            price = entity.price
        )
    }

    fun toConcertSeatGradeEntity(domain: ConcertSeatGrade): ConcertSeatGradeJpaEntity {
        return ConcertSeatGradeJpaEntity(
            concertSeatGradeId = domain.concertSeatGradeId,
            concertId = domain.concertId,
            seatGrade = domain.seatGrade,
            price = domain.price
        )
    }

    fun toUserDomain(entity: UserJpaJpaEntity): User {
        return User(
            userId = entity.userId,
            userName = entity.userName,
            totalPoint = entity.totalPoint,
            availablePoint = entity.availablePoint,
            usedPoint = entity.usedPoint,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toUserEntity(domain: User): UserJpaJpaEntity {
        return UserJpaJpaEntity(
            userId = domain.userId,
            userName = domain.userName,
            totalPoint = domain.totalPoint,
            availablePoint = domain.availablePoint,
            usedPoint = domain.usedPoint
        )
    }

    fun toQueueTokenDomain(entity: QueueTokenJpaEntity): QueueToken {
        return QueueToken(
            queueTokenId = entity.queueTokenId,
            userId = entity.userId,
            concertId = entity.concertId,
            tokenStatus = entity.tokenStatus,
            enteredAt = entity.enteredAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toQueueTokenEntity(domain: QueueToken): QueueTokenJpaEntity {
        return QueueTokenJpaEntity(
            queueTokenId = domain.queueTokenId,
            userId = domain.userId,
            concertId = domain.concertId,
            tokenStatus = domain.tokenStatus,
            enteredAt = domain.enteredAt
        )
    }

    fun toPaymentEntity(domain: Payment) : PaymentJpaEntity {
        return PaymentJpaEntity(
            paymentId = domain.paymentId,
            reservationId = domain.reservationId,
            userId = domain.userId,
            totalAmount = domain.totalAmount,
            discountAmount = domain.discountAmount,
            actualAmount = domain.actualAmount,
            paymentAt = domain.paymentAt,
            isCancel = domain.isCancel,
            isRefund = domain.isRefund,
            cancelAt = domain.cancelAt
        )
    }
}