package com.kk.mombit.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max


fun Int.formatCode(): String {
    val codeLen = max(this.toString().length, 6)
    return "%0${codeLen}d".format(this)
}

fun String.telegramShielded(): String {
    return this.replace(".", """\.""")
        .replace(",", """\,""")
        .replace("!", """\!""")
        .replace("-", """\-""")
        .replace("_", """\_""")
        .replace("|", """\|""")
        .replace("+", """\+""")
        .replace("#", """\#""")
        .replace("=", """\=""")
        .replace("{", """\{""")
        .replace("}", """\}""")

}

fun String.nonMarkdownShielded(): String = this
    .replace("(", """\(""")
    .replace(")", """\)""")
    .replace("{", """\{""")
    .replace("}", """\}""")

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i])
        val secondIndex = HEX_CHARS.indexOf(this[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}

fun BigDecimal?.round(
    increment: BigDecimal, roundingMode: RoundingMode = RoundingMode.UP
): BigDecimal? {
    return if (increment.signum() == 0) {
        this
    } else {
        val divided = this?.divide(increment, 0, roundingMode)
        divided?.multiply(increment)
    }
}

fun BigDecimal?.round(
    scale: Int, roundingMode: RoundingMode = RoundingMode.UP
): BigDecimal? {
    return this?.setScale(scale, roundingMode)
}



