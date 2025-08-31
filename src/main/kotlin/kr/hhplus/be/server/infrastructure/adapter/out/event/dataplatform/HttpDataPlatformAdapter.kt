package kr.hhplus.be.server.infrastructure.adapter.out.event.dataplatform

import kr.hhplus.be.server.application.dto.event.ReservationEventDto
import kr.hhplus.be.server.application.port.out.dataplatform.DataPlatformPort
import kr.hhplus.be.server.infrastructure.adapter.`in`.web.mock.dto.MockApiResponse
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class HttpDataPlatformAdapter(
    private val restTemplate: RestTemplate
) : DataPlatformPort {

    @Async
    override fun sendEvent(event: ReservationEventDto): Boolean {
        val url = "http://localhost:8080/mock/data-platform/events"
        val response = restTemplate.postForObject(url, event, MockApiResponse::class.java)
        return response?.success == true
    }
}