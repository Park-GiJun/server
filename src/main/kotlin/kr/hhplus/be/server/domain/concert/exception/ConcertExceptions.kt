package kr.hhplus.be.server.domain.concert.exception

import kr.hhplus.be.server.domain.common.exception.EntityNotFoundException
import kr.hhplus.be.server.domain.common.exception.BusinessRuleViolationException
import kr.hhplus.be.server.domain.common.exception.EntityStateException

class ConcertNotFoundException(concertId: Long) : EntityNotFoundException("Concert", concertId.toString())

class ConcertDateNotFoundException(dateId: Long) : EntityNotFoundException("ConcertDate", dateId.toString())

class ConcertSeatNotFoundException(seatId: Long) : EntityNotFoundException("ConcertSeat", seatId.toString())

class SeatAlreadyBookedException(seatNumber: String) :
    BusinessRuleViolationException("Seat $seatNumber is already booked")

class ConcertSoldOutException(concertName: String) :
    BusinessRuleViolationException("Concert '$concertName' is sold out")

class ConcertDateExpiredException(date: String) :
    EntityStateException("Concert date $date has expired")