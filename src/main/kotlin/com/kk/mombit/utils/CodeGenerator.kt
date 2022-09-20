package com.kk.mombit.utils

import java.util.*

object CodeGenerator {
    fun generateRandomCode(): String {
        return UUID.randomUUID().toString()
    }
}