package com.sportynix.app.core.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int? = null, val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
    object Unauthorized : ApiResult<Nothing>()
    object NoInternet : ApiResult<Nothing>()
    object Timeout : ApiResult<Nothing>()
    data class ServerError(val code: Int, val message: String) : ApiResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error || this is Unauthorized || this is NoInternet || this is Timeout || this is ServerError

    fun getOrNull(): T? = (this as? Success)?.data
}
