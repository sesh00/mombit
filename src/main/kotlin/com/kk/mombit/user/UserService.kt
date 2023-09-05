package com.kk.mombit.user

import com.kk.mombit.utils.CodeGenerator.generateRandomCode
import com.kk.mombit.utils.unwrap
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UserService(
    val userRepository: UserRepository
    ) {
    fun saveUser(user: User) = userRepository.save(user)

    fun removeUser(user: User) = userRepository.delete(user)

    fun getAllUsers(): ArrayList<User> {
        return userRepository.findAll() as ArrayList<User>
    }

    fun getUserById(id: Long): User? {
        return userRepository.findById(id).unwrap()
    }

    fun getUserByToken(token: String): User? {
        return userRepository.findByToken(token)
    }
    fun cleanUser(user: User){
        user.dealAmount = BigDecimal.ZERO
        user.dealAddress = ""
        user.dealStage = 0
        user.inDeal = false

        userRepository.save(user)
    }

}