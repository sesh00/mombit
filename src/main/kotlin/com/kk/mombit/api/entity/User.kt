package com.hitbit.hitbit.api.entity

data class SignUpRequest(
    val token: String
)

data class WithdrawRequest(
    val address: String,
    val token: String,
    val amount: Long
)