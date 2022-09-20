package com.kk.mombit.user

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Table

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByTgId(tgId: Long): User?

}

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "users")
data class User(
    var tgName: String?,
    val chatId: Long,
    val tgId: Long,
    var lastMessageId: Long = -1L,
    var isBan: Boolean = false,
    var isRegistered: Boolean = false,
    var isActive: Boolean = false,
    val balance: BigDecimal = BigDecimal.ZERO,
) : BaseEntity<Long>() {

}


