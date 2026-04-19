package com.colamusic.core.common

sealed class Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>()
    data class Failure(val error: Throwable, val message: String? = null) : Outcome<Nothing>()

    inline fun <R> map(transform: (T) -> R): Outcome<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun valueOrNull(): T? = (this as? Success)?.value
}

inline fun <T> outcome(block: () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: Throwable) {
    Outcome.Failure(e, e.message)
}
