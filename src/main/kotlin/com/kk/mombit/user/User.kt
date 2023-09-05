package com.kk.mombit.user

import com.kk.mombit.api.entity.DealResponse
import com.kk.mombit.utils.CodeGenerator.generateRandomCode
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByToken(token: String): User?
}

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "users")
data class User(
    val chatId: Long,
    val phoneNumber: String,
    var tgName: String?,
    var token: String,
    var lastMessageId: Long = -1L,
    var dealId: Long = 0L,

    @Column(precision = 40, scale = 8)
    var dealAmount: BigDecimal = BigDecimal.ZERO,
    var dealCurrency: Currency = Currency.BTC,
    var dealAddress: String = "",
    var dealStage: Int = 0,
    var inDeal: Boolean = false,

    var isBlocked: Boolean = false

) : BaseEntity<Long>()

enum class Currency(val currency: String, val isCrypto: Boolean) {
    BTC("BTC", true),
    RUB("RUB", false)
}



