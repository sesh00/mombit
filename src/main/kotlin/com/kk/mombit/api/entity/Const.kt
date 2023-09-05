package com.kk.mombit.api.entity

import java.math.BigDecimal

data class BalanceResponse(
    val balance: BigDecimal
)

data class RateResponse(
    val rate: BigDecimal
)

data class MinMaxResponse(
    val minCrypto: BigDecimal,
    val maxCrypto: BigDecimal,
    val minFiat: BigDecimal,
    val maxFiat: BigDecimal
)