package kr.hhplus.be.server.infrastructure.adapter.out.persistence.mapper

import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.domain.concert.ConcertDate
import kr.hhplus.be.server.domain.concert.ConcertSeat
import kr.hhplus.be.server.domain.concert.ConcertSeatGrade
import kr.hhplus.be.server.domain.log.pointHistory.PointHistory
import kr.hhplus.be.server.domain.payment.Payment
import kr.hhplus.be.server.domain.queue.QueueToken
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.ReservationJpaEntity
import kr.hhplus.be.server.domain.reservation.TempReservation
import kr.hhplus.be.server.domain.users.User
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertDateJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.concert.entity.ConcertSeatGradeJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.log.pointHistory.entity.PointHistoryJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.payment.entity.PaymentJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.queue.entity.QueueTokenJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.reservation.entity.TempReservationJpaEntity
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.user.entity.UserJpaEntity

object PersistenceMapper {

    fun toConcertDomain(entity: ConcertJpaEntity): Concert {
        return Concert(
            concertId = entity.concertId,
            concertName = entity.concertName,
            location = entity.location,
            description = entity.description ?: "",
            createdAt = try {
                entity.createdAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
            updatedAt = try {
                entity.updatedAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
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

    fun toConcertDateDomain(entity: ConcertDateJpaEntity): ConcertDate {
        return ConcertDate(
            concertDateId = entity.concertDateId,
            concertSession = entity.concertSession,
            concertId = entity.concertId,
            date = entity.date,
            isSoldOut = entity.isSoldOut,
            createdAt = try {
                entity.createdAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
            updatedAt = try {
                entity.updatedAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
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
            createdAt = try {
                entity.createdAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
            updatedAt = try {
                entity.updatedAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
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

    fun toUserDomain(entity: UserJpaEntity): User {
        return User(
            userId = entity.userId,
            userName = entity.userName,
            totalPoint = entity.totalPoint,
            availablePoint = entity.availablePoint,
            usedPoint = entity.usedPoint,
            version = entity.version,
            createdAt = try {
                entity.createdAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
            updatedAt = try {
                entity.updatedAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toUserEntity(domain: User): UserJpaEntity {
        return UserJpaEntity(
            userId = domain.userId,
            userName = domain.userName,
            totalPoint = domain.totalPoint,
            availablePoint = domain.availablePoint,
            usedPoint = domain.usedPoint,
            version = domain.version,
        )
    }

    fun toQueueTokenDomain(entity: QueueTokenJpaEntity): QueueToken {
        return QueueToken(
            queueTokenId = entity.queueTokenId,
            userId = entity.userId,
            concertId = entity.concertId,
            tokenStatus = entity.tokenStatus,
            enteredAt = entity.enteredAt,
            createdAt = try {
                entity.createdAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
            updatedAt = try {
                entity.updatedAt
            } catch (e: UninitializedPropertyAccessException) {
                null
            },
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

    fun toPaymentEntity(domain: Payment): PaymentJpaEntity {
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

    fun toPaymentDomain(entity: PaymentJpaEntity): Payment {
        return Payment(
            paymentId = entity.paymentId,
            reservationId = entity.reservationId,
            userId = entity.userId,
            totalAmount = entity.totalAmount,
            discountAmount = entity.discountAmount,
            actualAmount = entity.actualAmount,
            paymentAt = entity.paymentAt,
            isCancel = entity.isCancel,
            isRefund = entity.isRefund,
            cancelAt = entity.cancelAt
        )
    }

    fun toReservationEntity(domain: Reservation): ReservationJpaEntity {
        return ReservationJpaEntity(
            reservationId = domain.reservationId,
            userId = domain.userId,
            concertDateId = domain.concertDateId,
            seatId = domain.seatId,
            reservationAt = domain.reservationAt,
            cancelAt = domain.cancelAt,
            reservationStatus = domain.reservationStatus,
            paymentAmount = domain.paymentAmount
        )
    }

    fun toReservationDomain(entity: ReservationJpaEntity): Reservation {
        return Reservation(
            reservationId = entity.reservationId,
            userId = entity.userId,
            concertDateId = entity.concertDateId,
            seatId = entity.seatId,
            reservationAt = entity.reservationAt,
            cancelAt = entity.cancelAt,
            reservationStatus = entity.reservationStatus,
            paymentAmount = entity.paymentAmount
        )
    }

    fun toTempReservationEntity(domain: TempReservation): TempReservationJpaEntity {
        return TempReservationJpaEntity(
            tempReservationId = domain.tempReservationId,
            userId = domain.userId,
            concertSeatId = domain.concertSeatId,
            expiredAt = domain.expiredAt,
            status = domain.status
        )
    }

    fun toTempReservationDomain(entity: TempReservationJpaEntity): TempReservation {
        return TempReservation(
            tempReservationId = entity.tempReservationId,
            userId = entity.userId,
            concertSeatId = entity.concertSeatId,
            expiredAt = entity.expiredAt,
            status = entity.status
        )
    }

    fun toPointHistoryEntity(domain: PointHistory): PointHistoryJpaEntity {
        return PointHistoryJpaEntity(
            pointHistoryId = domain.pointHistoryId,
            userId = domain.userId,
            pointHistoryType = domain.pointHistoryType,
            pointHistoryAmount = domain.pointHistoryAmount,
            description = domain.description
        )
    }

    fun toPointHistoryDomain(entity: PointHistoryJpaEntity): PointHistory {
        return PointHistory(
            pointHistoryId = entity.pointHistoryId,
            userId = entity.userId,
            pointHistoryType = entity.pointHistoryType,
            pointHistoryAmount = entity.pointHistoryAmount,
            description = entity.description
        )
    }
}