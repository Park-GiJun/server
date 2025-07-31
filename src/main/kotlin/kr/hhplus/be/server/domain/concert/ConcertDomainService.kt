package kr.hhplus.be.server.domain.concert

import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.domain.concert.exception.ConcertSoldOutException
import kr.hhplus.be.server.domain.concert.exception.ConcertDateExpiredException
import java.time.LocalDateTime

class ConcertDomainService {

    fun validateConcertExists(concert: Concert?) {
        if (concert == null) {
            throw ConcertNotFoundException(0L)
        }
    }

    fun validateConcertAvailability(concertDate: ConcertDate, concertName: String) {
        if (concertDate.isSoldOut) {
            throw ConcertSoldOutException(concertName)
        }

        val now = LocalDateTime.now()
        if (concertDate.date.isBefore(now)) {
            throw ConcertDateExpiredException(concertDate.date)
        }
    }

    fun calculateSeatStatistics(seats: List<ConcertSeat>): Pair<Int, Int> {
        val totalSeats = seats.size
        val availableSeats = seats.count { it.seatStatus == SeatStatus.AVAILABLE }
        return Pair(totalSeats, availableSeats)
    }

    fun buildSeatPriceMap(seatGrades: List<ConcertSeatGrade>): Map<String, Int> {
        return seatGrades.associateBy({ it.seatGrade }, { it.price })
    }

    fun validateConcertDateExists(concertDate: ConcertDate?, concertDateId: Long) {
        if (concertDate == null) {
            throw ConcertNotFoundException(concertDateId)
        }
    }

    fun validateSeatsExist(seats: List<ConcertSeat>, concertDateId: Long) {
        if (seats.isEmpty()) {
            throw ConcertNotFoundException(concertDateId)
        }
    }
}