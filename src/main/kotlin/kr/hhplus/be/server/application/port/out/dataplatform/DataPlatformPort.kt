package kr.hhplus.be.server.application.port.out.dataplatform

import kr.hhplus.be.server.application.dto.event.ReservationEventDto

interface DataPlatformPort {
    fun sendEvent(event: ReservationEventDto): Boolean
}