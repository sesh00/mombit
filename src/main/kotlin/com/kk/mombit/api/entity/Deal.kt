package com.hitbit.hitbit.api.entity

import java.math.BigDecimal

data class CreateDealRequest(
    val token: String,
    val amount: BigDecimal
)

data class CreateDealResponse(
    val id: Long,
    val amountFiat: BigDecimal,
    val amountCrypto: BigDecimal,
    val requisite: String
)

data class CancelDealRequest(
    val id: Long
)