package com.kk.mombit.bot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import com.kk.mombit.utils.nonMarkdownShielded
import com.kk.mombit.utils.telegramShielded
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.io.File

interface Bot {
    fun <T : java.io.Serializable, Method : BotApiMethod<T>> execute(method: Method): T

    fun execute(sendSticker: SendSticker): Message
    fun execute(sendDocument: SendDocument): Message
    fun execute(sendPhoto: SendPhoto): Message

    suspend fun sendSticker(
        sticker: String,
        chatId: Long
    ) {
        // Сносит при отправке replyKeyboard
        val sendSticker = SendSticker()
        val str = InputFile()
        str.setMedia(sticker)
        sendSticker.chatId = chatId.toString()
        sendSticker.sticker = str
        val tmp = ReplyKeyboardRemove()
        tmp.removeKeyboard = true
        sendSticker.replyMarkup = tmp
        try {
            execute(sendSticker)
        } catch (e: Exception) {
            println("#")
            println("Ошибка при отправке стикера.")
            println(e.message)
            println("#")
        }
    }

    suspend fun editMessage(
        text: String,
        messageId: Long,
        chatId: Long,
        inlineButtons: List<Pair<String, String>>? = null,
        shielded: Boolean = false
    ) {
        val editMessage = EditMessageText()
        editMessage.messageId = messageId.toInt()
        editMessage.chatId = chatId.toString()
        editMessage.text = if (shielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()
        editMessage.parseMode = "MarkdownV2"
        if (inlineButtons != null) editMessage.replyMarkup = getReplyInlineKeyboard(inlineButtons)

        try {
            execute(editMessage)
        } catch (e: Exception) {
            println("#")
            println("Ошибка при редактировании сообщения.")
            println(e.message)
            println("#")
        }
    }

    suspend fun sendMessage(
        text: String,
        chatId: Long,
        markButtons: List<List<String>>? = null,
        inlineButtons: List<Pair<String, String>>? = null,
        shielded: Boolean = false,
        oneTime: Boolean = false
    ): Int? {
        // Приоритет обработки buttons: 1 -> mark, 2 -> inline

        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = if (shielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()
        sendMessage.parseMode = "MarkdownV2"
        if (markButtons != null) sendMessage.replyMarkup = getReplyMarkup(markButtons, oneTime)
        if (inlineButtons != null) sendMessage.replyMarkup = getReplyInlineKeyboard(inlineButtons)
        return GlobalScope.async {
            val id = try {
                execute(sendMessage).messageId
            } catch (e: Exception) {
                println("#")
                println("Ошибка при отправке сообщения.")
                println(e.message)
                println("#")
                -100
            }

            return@async id
        }.await()
    }

    fun getReplyMarkup(allButtons: List<List<String>>, oneTime: Boolean = false): ReplyKeyboardMarkup {
        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = oneTime
        markup.keyboard = allButtons.map { rowButtons ->
            val row = KeyboardRow()
            rowButtons.forEach { rowButton -> row.add(rowButton) }
            row
        }
        return markup
    }

    private fun getReplyInlineKeyboard(buttonsInfo: List<Pair<String, String>>): InlineKeyboardMarkup {
        val keyboard = arrayListOf<List<InlineKeyboardButton>>()
        var twoList = arrayListOf<InlineKeyboardButton>()
        for ((name, code) in buttonsInfo) {
            val button = InlineKeyboardButton()
            button.text = name

            if (code.length >= 8 && code.substring(0, 8) == "https://") {
                button.url = code
            } else {
                button.callbackData = code
            }

            twoList.add(button)

            if (twoList.size == 2) {
                keyboard.add(twoList)
                twoList = arrayListOf()
            }
        }

        if (twoList.isNotEmpty()) {
            keyboard.add(twoList)
        }
        val inlineKeyboard = InlineKeyboardMarkup()
        inlineKeyboard.keyboard = keyboard
        return inlineKeyboard
    }

    suspend fun sendPhoto(photo: String, chatId: Long) {
        // Закидывайте все отправляемые фото и документы в папку "photoAndDocs"
        val sendPhoto = SendPhoto()
        val t = InputFile()
        t.setMedia(File("photoAndDocs\\$photo"))
        sendPhoto.photo = t
        sendPhoto.chatId = chatId.toString()
        try {
            execute(sendPhoto)
        } catch (e: Exception) {
            println("#")
            println("Ошибка при отправке фото из папки.")
            println(e.message)
            println("#")
        }
    }

    suspend fun sendPhoto(photo: File, chatId: Long) {
        val sendPhoto = SendPhoto()
        val t = InputFile()
        t.setMedia(photo)
        sendPhoto.caption
        sendPhoto.photo = t
        sendPhoto.chatId = chatId.toString()
        try {
            execute(sendPhoto)
        } catch (e: Exception) {
            println("#")
            println("Ошибка при отправке фото как файла.")
            println(e.message)
            println("#")
        }
    }

    suspend fun sendDocument(document: File, text: String, chatId: Long, markdownShielded: Boolean = true) {
        val sendDocument = SendDocument()
        sendDocument.chatId = chatId.toString()
        sendDocument.document = InputFile(document, document.name)
        sendDocument.caption =
            if (markdownShielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()

        sendDocument.parseMode = "MarkdownV2"
        try {
            execute(sendDocument)
        } catch (e: Exception) {
            println("#")
            println("Ошибка при отправке документа.")
            println(e.message)
            println("#")
        }
    }

    suspend fun answerCallbackQuery(id: String) {
        val answerCallbackQuery = AnswerCallbackQuery()
        answerCallbackQuery.callbackQueryId = id

        execute(answerCallbackQuery)
    }
}