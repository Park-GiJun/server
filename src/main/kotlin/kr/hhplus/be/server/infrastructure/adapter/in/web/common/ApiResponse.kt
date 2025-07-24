package kr.hhplus.be.server.infrastructure.adapter.`in`.web.common

data class ApiResponse<T>(
    val success: Boolean,
    val status: Int,
    val data: T? = null,
    val message: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String = "Success"): ApiResponse<T> {
            return ApiResponse(true, 200, data, message)
        }
    }
}