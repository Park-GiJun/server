package kr.hhplus.be.server.dto.common

data class ApiResponse<T>(
    val success: Boolean,
    val status: Int,
    val data: T? = null,
    val message: String? = null
)