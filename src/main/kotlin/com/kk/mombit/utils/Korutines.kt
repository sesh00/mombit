package com.kk.mombit.utils

import kotlinx.coroutines.*

suspend fun <T> retry(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 5000, // 0.1 second
    maxDelay: Long = 5000,    // 1 second
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        delay(currentDelay)
        //currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

fun startSuspended(unit: suspend () -> Any?) = GlobalScope.launch { try { unit.invoke() } catch (e: Exception) { e.printStackTrace() } }

fun <T> asynced(unit: suspend () -> T): Deferred<T> = GlobalScope.async { unit.invoke() }

fun <T, R> wrapTry(func: suspend (T) -> R, default: R): suspend (T) -> R = {
    try {
        func.invoke(it)
    } catch (e: Exception) {
        default
    }
}