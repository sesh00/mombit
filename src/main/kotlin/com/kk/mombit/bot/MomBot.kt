package com.kk.mombit.bot

import com.kk.mombit.user.User
import com.kk.mombit.user.UserService
import com.kk.mombit.utils.Constants
import com.kk.mombit.utils.startSuspended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class UserBot(
    @Value("\${telegram.token}")
    val token: String,
    val userService: UserService,
    val constants: Constants
): TelegramLongPollingBot(), Bot {
    override fun getBotToken(): String = token

    override fun getBotUsername(): String = "Mombit bot"

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val userName =
                if (update.hasCallbackQuery()) update.callbackQuery.from.userName else update.message.from.userName
            val userChatId =
                if (update.hasCallbackQuery()) update.callbackQuery.message.chatId else update.message.chatId.toLong()

            val user = userService.getAllUsers().firstOrNull { it.chatId == userChatId }

            if (user == null) {
                startSuspended { processCandidateUpdate(update) }
            } else {
                startSuspended { processUserUpdate(user, update) }
            }
        }
    }

    suspend fun processUserUpdate(user: User, update: Update) {
        val splitted = update.message.text.split(" ")

        when (splitted[0].lowercase().split("@")[0]) {
            "/enable" -> {

                sendMessage("Копитрейдинг включен", user.chatId)
            }

            "/disable" -> {
                userService.saveUser(user)

                sendMessage("Копитрейдинг выключен", user.chatId)
            }


            "/setkeys" -> {

            }

            "/help" -> sendMessage("""
                /stats --- информация об аккаунте
                /enable --- включить копитрейдинг
                /disable --- выключить копитрейдинг
                /balance --- баланс аккаунта в USDT
                /setKeys api-key api-secret --- установить ключи от ByBit
            """.trimIndent(), user.chatId)
        }
    }

    suspend fun processCandidateUpdate(update: Update) {
        GlobalScope.launch (Dispatchers.Default) {
            if (update.hasMessage()) {

                    val newUser = User(
                        tgId = update.message.from.id,
                        chatId = update.message.chatId,
                        tgName = update.message.from.userName,
                    )

                    userService.saveUser(newUser)

                    sendMessage("Mombit приветствует вас!", update.message.chatId)

            }
        }
    }

}