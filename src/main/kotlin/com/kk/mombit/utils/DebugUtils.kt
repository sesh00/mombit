package com.kk.mombit.utils

object DebugUtils {
    private val debug = true
    private const val header = "--------DEBUG--------"

    fun print(
        message: String,
        vararg args: Any?
    ) {
        if (debug) {
            val methodCalledFrom = try {
                Thread.currentThread().stackTrace[12].methodName + " " + Thread.currentThread().stackTrace[13].methodName
            } catch (e: Exception) {
                ""
            }

            System.err.println(header)
            System.err.println("Method: $methodCalledFrom")
            System.err.println("Args: ${args.joinToString(",")}")
            System.err.println("Result: $message")
            System.err.println(System.lineSeparator())
        }
    }
}