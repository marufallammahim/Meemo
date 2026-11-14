package com.marufallammahim.meemo.core.result

import com.marufallammahim.meemo.core.error.MeemoError

sealed interface ResultWrapper<out T> {
    data class Success<T>(val data: T) : ResultWrapper<T>
    data class Failure(val error: MeemoError) : ResultWrapper<Nothing>

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): MeemoError? = (this as? Failure)?.error
}
