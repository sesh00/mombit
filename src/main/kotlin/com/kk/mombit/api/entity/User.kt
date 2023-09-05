package com.kk.mombit.api.entity

import java.math.BigDecimal


data class SignUpRequest(
    val token: String,
    val tgName: String,
    val phone: String
)

data class WithdrawRequest(
    val address: String,
    val token: String,
    val amount: BigDecimal
)

data class WSHandshake( val token: String )

data class UserNotify(
    val to: String,
    val type: String,
    val message: String
)