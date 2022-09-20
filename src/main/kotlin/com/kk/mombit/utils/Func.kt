package com.kk.mombit.utils

import java.io.File
import java.nio.file.Files.createDirectory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

@Suppress("UNCHECKED_CAST")
fun <T : Exception> kassert(b: Boolean, exception: T = Exception() as T) {
    if (!b) {
        throw exception
    }
}

fun <T> Optional<T>.unwrap(): T? = orElse(null)