package com.kk.mombit.utils

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class BitcoinBigDecimal(s: String) : BigDecimal(s) {
    constructor(double: Double) : this(double.toString())
    constructor(long: Long) : this(long.toString())
    constructor(bigDecimal: BigDecimal) : this(bigDecimal.toPlainString())

    companion object {
        val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
        val ZERO = BitcoinBigDecimal(0L)
        val ONE = BitcoinBigDecimal(1L)
        val TEN = BitcoinBigDecimal(10L)
    }

    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun toChar(): Char {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }

    override fun divide(divisor: BigDecimal?): BitcoinBigDecimal {
        return BitcoinBigDecimal(super.divide(divisor, mathContext))
    }

    override fun multiply(multiplicand: BigDecimal?): BitcoinBigDecimal {
        return BitcoinBigDecimal(super.multiply(multiplicand, mathContext))
    }

    override fun subtract(subtrahend: BigDecimal?): BitcoinBigDecimal {
        return BitcoinBigDecimal(super.subtract(subtrahend, mathContext))
    }

    override fun add(augend: BigDecimal?): BitcoinBigDecimal {
        return BitcoinBigDecimal(super.add(augend, mathContext))
    }

    override fun equals(other: Any?): Boolean {
        return super.compareTo(other as BigDecimal?) == 0
    }
}