package kr.hhplus.be.server.service

import kr.hhplus.be.server.domain.concert.Concert
import kr.hhplus.be.server.domain.concert.SeatStatus
import kr.hhplus.be.server.dto.ConcertDateWithStatsResponse
import kr.hhplus.be.server.dto.ConcertSeatWithPriceResponse
import kr.hhplus.be.server.dto.QueueTokenStatusRequest
import kr.hhplus.be.server.exception.ConcertDateExpiredException
import kr.hhplus.be.server.exception.ConcertNotFoundException
import kr.hhplus.be.server.exception.ConcertSoldOutException
import kr.hhplus.be.server.exception.InvalidTokenException
import kr.hhplus.be.server.exception.InvalidTokenStatusException
import kr.hhplus.be.server.exception.QueueTokenNotFoundException
import kr.hhplus.be.server.exception.UserNotFoundException
import kr.hhplus.be.server.repository.mock.MockConcertDateRepository
import kr.hhplus.be.server.repository.mock.MockConcertRepository
import kr.hhplus.be.server.repository.mock.MockConcertSeatGradeRepository
import kr.hhplus.be.server.repository.mock.MockConcertSeatRepository
import kr.hhplus.be.server.repository.mock.MockQueueTokenRepository
import kr.hhplus.be.server.repository.mock.MockUserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ConcertService(
    private val concertRepository: MockConcertRepository,
    private val concertDateRepository: MockConcertDateRepository,
    private val concertSeatRepository: MockConcertSeatRepository,
    private val concertSeatGradeRepository: MockConcertSeatGradeRepository,
    private val queueTokenRepository: MockQueueTokenRepository,
    private val userRepository: MockUserRepository
) {
    fun getConcertList(): List<Concert> {
        return concertRepository.findConcertList()
            ?: throw ConcertNotFoundException("Not Found Concert")
    }

    fun getConcertDate(req: QueueTokenStatusRequest, concertId: Long): List<ConcertDateWithStatsResponse> {
        validateActiveToken(req)

        val concertDates = concertDateRepository.findConcertDateByConcertId(concertId)
            .ifEmpty { throw ConcertNotFoundException("Not Found Concert Date By Id") }

        return concertDates.map { concertDate ->
            val seats = concertSeatRepository.findConcertSeats(concertDate.concertDateId)
                ?: emptyList()

            val totalSeats = seats.size
            val availableSeats = seats.count { it.seatStatus == SeatStatus.AVAILABLE }

            ConcertDateWithStatsResponse(
                concertDate = concertDate,
                totalSeats = totalSeats,
                availableSeats = availableSeats
            )
        }
    }

    fun getConcertSeats(req: QueueTokenStatusRequest, concertDateId: Long): List<ConcertSeatWithPriceResponse> {
        validateActiveToken(req)

        val concertDate = concertDateRepository.findConcertDateByConcertDateId(concertDateId)
            ?: throw ConcertNotFoundException("Not Found Concert Date")

        val concert = concertRepository.findByConcertId(concertDate.concertId)
            ?: throw ConcertNotFoundException("Not Found Concert")

        if (req.concertId != concertDate.concertId) {
            throw InvalidTokenException("Token concert ID does not match requested concert")
        }

        if (concertDate.isSoldOut) {
            throw ConcertSoldOutException("Concert date is sold out")
        }

        val now = LocalDateTime.now()
        if (concertDate.date.isBefore(now)) {
            throw ConcertDateExpiredException("Concert date has passed")
        }

        val seats = concertSeatRepository.findConcertSeats(concertDateId)
            ?: throw ConcertNotFoundException("Not Found Concert Seats")

        val seatGradePriceMap = concertSeatGradeRepository.findByConcertId(concertDate.concertId)
            .associateBy { it.seatGrade }

        return seats.map { seat ->
            val price = seatGradePriceMap[seat.seatGrade]?.price ?: 0

            ConcertSeatWithPriceResponse(
                seat = seat,
                price = price
            )
        }
    }

    private fun validateUser(userId: String) {
        userRepository.findByUserId(userId)
            ?: throw UserNotFoundException("User not found with id: $userId")
    }

    private fun validateActiveToken(req: QueueTokenStatusRequest): Boolean {
        val token = queueTokenRepository.findByQueueToken(req.uuid)
            ?: throw QueueTokenNotFoundException("Queue token not found: ${req.uuid}")

        if (!token.isActive()) {
            throw InvalidTokenStatusException("Token is not active. Current status: ${token.tokenStatus}")
        }

        return true
    }
}