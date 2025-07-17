package kr.hhplus.be.server.domain.concert


enum class SeatStatus {
    AVAILABLE,
    RESERVED,
    SOLD;

    fun isReservable(): Boolean = this == AVAILABLE
    fun isOccupied(): Boolean = this == RESERVED || this == SOLD
}
