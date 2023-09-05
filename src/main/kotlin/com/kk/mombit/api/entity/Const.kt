package com.hitbit.hitbit.api.entity

import java.math.BigDecimal

data class BalanceResponse(
    val balance: Long
)

data class RateResponse(
    val rate: BigDecimal
)