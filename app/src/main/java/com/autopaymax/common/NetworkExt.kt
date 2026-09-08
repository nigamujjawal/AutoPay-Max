package com.autopaymax.common

import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Resource<T> = try {
    Resource.Success(apiCall())
} catch (e: HttpException) {
    Resource.Error(e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: e.message(), code = e.code())
} catch (e: IOException) {
    Resource.Error("Network error, check your connection")
} catch (e: Exception) {
    Resource.Error(e.message ?: "Unexpected error")
}
