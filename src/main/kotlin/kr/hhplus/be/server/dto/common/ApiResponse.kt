package kr.hhplus.be.server.dto.common

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

        fun <T> error(status: Int, message: String): ApiResponse<T> {
            return ApiResponse(false, status, null, message)
        }
    }
}
