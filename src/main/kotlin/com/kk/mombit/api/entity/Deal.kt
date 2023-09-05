package com.kk.mombit.api.entity

import java.math.BigDecimal

data class CreateDealRequest(
    val token: String,
    val amount: BigDecimal,
    val isCrypto: Boolean,
    val address: String
)

data class CreateDealResponse(
    val id: Long,
    val amountFiat: BigDecimal,
    val amountCrypto: BigDecimal,
    val requisite: String
)

data class DealResponse(
    val id: Long,
    val amountFiat: BigDecimal,
    val amountCrypto: BigDecimal,
    val fee: BigDecimal,
    val status: Int,
    val requisite: String,
    val active: Boolean,
    val address: String,
    val token: String,
    val txid: String
)





