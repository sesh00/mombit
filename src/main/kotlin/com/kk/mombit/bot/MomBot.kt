package com.kk.mombit.bot

import com.google.gson.Gson
import com.kk.mombit.api.MombitAPI
import com.kk.mombit.api.MombitAPIException
import com.kk.mombit.api.MombitWSAPI
import com.kk.mombit.api.entity.DealResponse
import com.kk.mombit.api.entity.MinMaxResponse
import com.kk.mombit.api.entity.UserNotify
import com.kk.mombit.user.Currency
import com.kk.mombit.user.User
import com.kk.mombit.user.UserService
import com.kk.mombit.utils.CodeGenerator
import com.kk.mombit.utils.startSuspended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.math.BigDecimal
import javax.annotation.PostConstruct

class InvalidDealAmountException(s: String) : Exception(s)
class InvalidDealAddressException(s: String) : Exception(s)

@Component
class UserBot(
    @Value("\${telegram.token}") val token: String,
    val userService: UserService,
    val mombitAPI: MombitAPI,
    val mombitWSAPI: MombitWSAPI

) : TelegramLongPollingBot(), Bot {
    private val gson = Gson()

    val startUserMark = listOf(
        listOf("Купить криптовалюту", "Мои транзакции"), listOf("Поддержка", "Как это работает?")
    )

    suspend fun gotDealUpdate(message: String) {


        val deal = gson.fromJson(message, DealResponse::class.java)
        val user = userService.getUserByToken(deal.token) ?: return

        when (deal?.status) {

            3 -> {
                deleteMessage(user.lastMessageId, user.chatId)
                deleteMessage(user.lastMessageId + 1, user.chatId)

                sendMessage(
                    "Ваша сделка находится на рассмотрении. Ожидайте ответ от поддержки",
                    user.chatId,
                    inlineButtons = listOf(
                        "Поддержка" to "https://t.me/mombit_support"
                    ),
                    markButtons = listOf(listOf("Меню")),
                    oneTime = false
                )

                userService.cleanUser(user)

            }
            7 -> {
                deleteMessage(user.lastMessageId, user.chatId)
                deleteMessage(user.lastMessageId + 1, user.chatId)
                sendMessage(
                    "Ваша сделка отменена", user.chatId, markButtons = listOf(listOf("Меню")), oneTime = false
                )

                userService.cleanUser(user)

            }
            0 -> {
                deleteMessage(user.lastMessageId, user.chatId)
                deleteMessage(user.lastMessageId + 1, user.chatId)

                sendMessage(
                    "Успешно! Ваша транзакция отправлена, отслеживать ее Вы можете по ссылке:",
                    user.chatId,
                    inlineButtons = listOf(
                        "Проверить" to "https://blockchair.com/bitcoin/transaction/${deal.txid}", "Поддержка" to "https://t.me/mombit_support"
                    ),
                    markButtons = listOf(listOf("Меню")),
                    oneTime = false
                )
                userService.cleanUser(user)
            }
            10 -> {
                deleteMessage(user.lastMessageId, user.chatId)
                deleteMessage(user.lastMessageId + 1, user.chatId)

                sendMessage(
                    "Не удается найти Вашу оплату. Если Вы оплатили на указанные реквизиты, нажмите кнопку «Открыть спор» и отправьте чек об оплате",
                    user.chatId,
                    inlineButtons = listOf(
                        "Открыть спор" to "dispute", "Поддержка" to "https://t.me/mombit_support"
                    ),
                    markButtons = listOf(listOf("Меню")),
                    oneTime = false
                )
                //userService.cleanUser(user)
            }

        }

    }

    suspend fun gotNotifyUpdate(message: String) {
        val notify = gson.fromJson(message, UserNotify::class.java)

        var users: List<User> = if(notify.to == "all"){
            userService.getAllUsers()
        } else {
            listOf(userService.getAllUsers().firstOrNull{ it.token == notify.to }!!)
        }

        when(notify.type){
            "notify" -> {
                users.forEach{
                    sendMessage(notify.message, it.chatId)
                }

            }
            "ban" -> {
                users.forEach{
                    it.isBlocked = true
                    userService.saveUser(it)
                }

            }
            "unban" -> {
                users.forEach{
                    it.isBlocked = false
                    userService.saveUser(it)
                }

            }
        }


    }

    @PostConstruct
    fun iinit() {
        mombitWSAPI.gotDealUpdate = this::gotDealUpdate
        mombitWSAPI.gotNotifyUpdate = this::gotNotifyUpdate
    }

    override fun getBotToken(): String = token

    override fun getBotUsername(): String = "Mombit bot"

    override fun onUpdateReceived(update: Update) {
        if ((update.hasCallbackQuery() || update.message.hasText() || update.message.hasContact())) {
            val userName =
                if (update.hasCallbackQuery()) update.callbackQuery.from.userName else update.message.from.userName
            val userChatId =
                if (update.hasCallbackQuery()) update.callbackQuery.message.chatId else update.message.chatId.toLong()

            val user = userService.getAllUsers().firstOrNull { it.chatId == userChatId }

            if (user == null) {
                startSuspended { processCandidateUpdate(update) }
            } else {
                if(!user.isBlocked){
                    startSuspended { processUserUpdate(user, update) }
                }
            }
        }
    }

    suspend fun processUserUpdate(user: User, update: Update) {

        if (update.hasMessage()) {
            when (update.message.text) {


                "Меню", "/start" -> {
                    try {
                        deleteMessage(user.lastMessageId, user.chatId)
                    } catch (_: Exception) {
                    }

                    sendMessage(
                        "@${user.tgName}! Выберите действие:", user.chatId, startUserMark, oneTime = false
                    )
                    userService.cleanUser(user)
                }

                "Купить криптовалюту" -> {
                    try {
                        val minMaxBalance = mombitAPI.getMinMaxBalance()
                        sendMessage(
                            "Пожалуйста, введите сумму от ${minMaxBalance?.minFiat} до ${minMaxBalance?.maxFiat} RUB или" + " от ${minMaxBalance?.minCrypto} до ${minMaxBalance?.maxCrypto} BTC\n\nЕсли вы хотите указать сумму в BTC," + " то добавьте тикер криптовалюты BTC \\(например: 0.035 BTC\\)\n",
                            user.chatId,
                            listOf(listOf("Меню")),
                            oneTime = false
                        )

                        user.dealStage = 1
                        userService.saveUser(user)
                    } catch (e: MombitAPIException) {
                        println(e)

                        sendMessage(
                            e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                            inlineButtons = listOf(
                                "Поддержка" to "https://t.me/mombit_support"
                            ),
                        )
                    }
                }

                "Мои транзакции" -> {
                    sendMessage(
                        "пока не работает", user.chatId, listOf(listOf("Меню")), oneTime = false
                    )
                    userService.cleanUser(user)
                }

                "Как это работает?" -> {
                    sendMessage(
                        "\uD83D\uDD25 С помощью этого бота Вы можете без регистрации и верификации, быстро и выгодно купить биткоин \\(BTC\\) и получить его сразу на Ваш адрес кошелька. \n\n" + "ℹ️ Совершать такие покупки намного проще и дешевле, чем покупать криптовалюту на разных сайтах. Здесь намного выгоднее цены, значительно ниже комиссии, а купить криптовалюту можно в любое время суток! \n\n" + "Это очень удобно, смотрите как это работает: \n\n" + "1️⃣ Вводите сумму покупки в рублях или в BTC \n\n" + "2️⃣ Вводите адрес Вашего биткоин-кошелька \\(это может быть любой Ваш кошелек или адрес пополнения, будь то личный «холодный» кошелек или, например Ваш BTC-счет на сторонних сайтах\\)\n\n" + "❗️Очень внимательно сверяете адрес биткоин-кошелька, ошибка в адресе приведет к потере денег! \n\n" + "3️⃣ Оплачиваем покупку\n\n" + "ℹ️ Вам бот предоставит номер карты для оплаты. Вы можете оплатить покупку переводом по номеру карты с любого российского банка и нажать в боте кнопку «Я оплатил». На оплату у вас есть 30 минут, иначе сделка будет отменена \n\n" + "❗️ Важно чтобы сумма перевода была в точности такая, какая указана в боте, тогда покупка пройдет быстро и легко, а если ошибиться в сумме перевода, то придется писать в поддержку и ждать когда Ваш платеж обработается в ручном режиме \n\n" + "4️⃣ После успешной оплаты, Вам придет ссылка для отслеживания транзакции. Обычно, транзакция выполняется за 10-15 минут, но, если сеть BTC сильно загружена, то время ожидания транзакции может увеличиться\n\n" + "ℹ️ Мы всегда отправляем Вам биткоины с оптимальной комиссией чтобы Ваша транзакция обрабатывалась в первую очередь и деньги проходили на Ваш кошелек как можно скорее\n\n" + "Вот и все, весь процесс покупки занимает не больше 3 минут, а если у Вас возникнут какие-то вопросы, поддержка всегда будет рада Вам помочь - @mombit_support",
                        user.chatId,
                        startUserMark,
                        oneTime = false
                    )
                    userService.cleanUser(user)
                }
                "Поддержка" -> {
                    sendMessage(
                        "У Вас есть вопрос или Вам нужна помощь? Мы всегда рады Вам помочь, напишите @mombit_support",
                        user.chatId,
                        startUserMark,
                        oneTime = false
                    )
                    userService.cleanUser(user)
                }

                else -> {
                    when (user.dealStage) {
                        0 -> {
                            sendMessage(
                                "Не понимаю вас", user.chatId, startUserMark, oneTime = false
                            )
                        }
                        1 -> {
                            var minMaxBalance: MinMaxResponse?

                            try {
                                val messageText = update.message.text
                                val splitted = messageText.split(" ")

                                var dealAmount: BigDecimal
                                var dealCurrency: Currency

                                if (splitted.size == 1) {
                                    dealAmount = BigDecimal(messageText)
                                    dealCurrency = Currency.RUB
                                } else {
                                    dealAmount = BigDecimal(splitted[0])
                                    dealCurrency = if (splitted[1].uppercase() == "RUB") {
                                        Currency.RUB
                                    } else {
                                        Currency.BTC
                                    }
                                }

                                user.dealAmount = dealAmount
                                user.dealCurrency = dealCurrency

                                minMaxBalance = mombitAPI.getMinMaxBalance()!!

                                when (dealCurrency) {
                                    Currency.RUB -> {
                                        if (minMaxBalance.minFiat > dealAmount || dealAmount > minMaxBalance.maxFiat) {
                                            throw InvalidDealAmountException("Invalid dealAmount")
                                        }
                                    }
                                    Currency.BTC -> {
                                        if (minMaxBalance.minCrypto > dealAmount || dealAmount > minMaxBalance.maxCrypto) {
                                            throw InvalidDealAmountException("Invalid dealAmount")
                                        }
                                    }

                                }

                                if (!user.inDeal) {
                                    sendMessage(
                                        "Пожалуйста, укажите BTC адрес, на который надо отправить криптовалюту",
                                        user.chatId,
                                        listOf(listOf("Меню")),
                                        oneTime = false
                                    )
                                }

                                user.dealStage = 2

                            } catch (e: Exception) {

                                when (e) {
                                    is MombitAPIException -> {
                                        sendMessage(
                                            e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                            inlineButtons = listOf(
                                                "Поддержка" to "https://t.me/mombit_support"
                                            ),
                                        )
                                        println(e)

                                    }
                                    is InvalidDealAmountException -> {
                                        minMaxBalance = mombitAPI.getMinMaxBalance()!!

                                        sendMessage(
                                            "Пожалуйста, введите сумму от ${minMaxBalance?.minFiat} до ${minMaxBalance?.maxFiat} RUB или" + " от ${minMaxBalance?.minCrypto} до ${minMaxBalance?.maxCrypto} BTC\n\nЕсли вы хотите указать сумму в BTC," + " то добавьте тикер криптовалюты BTC \\(например: 0.035 BTC\\)\n",
                                            user.chatId,
                                            listOf(listOf("Меню")),
                                            oneTime = false
                                        )

                                    }

                                }

                                user.dealStage = 1
                            }
                            userService.saveUser(user)
                        }

                        2 -> {
                            var minMaxBalance: MinMaxResponse?

                            try {
                                val messageText = update.message.text

                                if (!user.inDeal) {

                                    if (mombitAPI.checkAddress(messageText) == true) {
                                        user.dealAddress = messageText
                                    } else {
                                        throw InvalidDealAddressException("Invalid dealAddress")
                                    }


                                } else {
                                    val splitted = messageText.split(" ")

                                    var dealAmount: BigDecimal
                                    var dealCurrency: Currency

                                    if (splitted.size == 1) {
                                        dealAmount = BigDecimal(messageText)
                                        dealCurrency = Currency.RUB
                                    } else {
                                        dealAmount = BigDecimal(splitted[0])
                                        dealCurrency = if (splitted[1].uppercase() == "RUB") {
                                            Currency.RUB
                                        } else {
                                            Currency.BTC
                                        }
                                    }

                                    user.dealAmount = dealAmount
                                    user.dealCurrency = dealCurrency

                                    minMaxBalance = mombitAPI.getMinMaxBalance()!!

                                    when (dealCurrency) {
                                        Currency.RUB -> {
                                            if (minMaxBalance.minFiat > dealAmount || dealAmount > minMaxBalance.maxFiat) {
                                                throw InvalidDealAmountException("Invalid dealAmount")
                                            }
                                        }
                                        Currency.BTC -> {
                                            if (minMaxBalance.minCrypto > dealAmount || dealAmount > minMaxBalance.maxCrypto) {
                                                throw InvalidDealAmountException("Invalid dealAmount")
                                            }
                                        }

                                    }
                                }


                                val rates = mombitAPI.calculateDeal(user)

                                sendMessage(
                                    "К оплате ${rates?.amountFiat} RUB," + " с учетом комиссии за транзакцию, вы получите ${rates?.amountCrypto} BTC на адрес ${user.dealAddress}",
                                    user.chatId,
                                    listOf(listOf("Меню")),
                                    oneTime = false
                                )

                                val messageId = sendMessage(
                                    "⚠️ Внимательно проверьте адрес получения, если в адресе есть ошибка - нажмите кнопку «Исправить адрес» \n\n" + "ℹ️ Если Вы хотите исправить сумму покупки, нажмите кнопку «Изменить сумму» ",
                                    user.chatId,
                                    listOf(listOf("Меню")),
                                    listOf(
                                        "Исправить адрес" to "changeAddress",
                                        "Изменить сумму" to "changeAmount",
                                        "Подтвердить" to "acceptDeal"
                                    ),
                                    oneTime = false,
                                    shielded = true
                                )

                                user.lastMessageId = messageId!!.toLong()


                            } catch (e: Exception) {

                                when (e) {
                                    is MombitAPIException -> {
                                        println(e)
                                        sendMessage(
                                            e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                            inlineButtons = listOf(
                                                "Поддержка" to "https://t.me/mombit_support"
                                            ),
                                        )
                                    }
                                    is InvalidDealAmountException -> {
                                        minMaxBalance = mombitAPI.getMinMaxBalance()!!
                                        sendMessage(
                                            "Пожалуйста, введите сумму от ${minMaxBalance?.minFiat} до ${minMaxBalance?.maxFiat} RUB или" + " от ${minMaxBalance?.minCrypto} до ${minMaxBalance?.maxCrypto} BTC\n\nЕсли вы хотите указать сумму в BTC," + " то добавьте тикер криптовалюты BTC \\(например: 0.035 BTC\\)\n",
                                            user.chatId,
                                            listOf(listOf("Меню")),
                                            oneTime = false
                                        )
                                    }

                                    is InvalidDealAddressException -> {
                                        sendMessage(
                                            "Пожалуйста, введите валидный адресс сети",
                                            user.chatId,
                                            listOf(listOf("Меню")),
                                            oneTime = false
                                        )
                                    }


                                }

                            }

                            userService.saveUser(user)

                        }
                    }
                }

            }
        }
        if (update.hasCallbackQuery()) {
           // if (user.dealStage == 2) {
            if (true) {
                answerCallbackQuery(update.callbackQuery.id)

                when (update.callbackQuery.data) {
                    "changeAddress" -> {
                        deleteMessage(user.lastMessageId, user.chatId)

                        sendMessage(
                            "Пожалуйста, укажите BTC адрес, на который надо отправить криптовалюту",
                            user.chatId,
                            startUserMark,
                            oneTime = false
                        )

                        user.inDeal = false
                        userService.saveUser(user)

                    }

                    "changeAmount" -> {
                        try {
                            deleteMessage(user.lastMessageId, user.chatId)
                            val minMaxBalance = mombitAPI.getMinMaxBalance()
                            sendMessage(
                                "Пожалуйста, введите сумму от ${minMaxBalance?.minFiat} до ${minMaxBalance?.maxFiat} RUB или " + "от ${minMaxBalance?.minCrypto} до ${minMaxBalance?.maxCrypto} BTC\n\nЕсли вы хотите указать сумму в BTC," + " то добавьте тикер криптовалюты BTC \\(например: 0.035 BTC\\)\n",
                                user.chatId,
                                listOf(listOf("Меню")),
                                oneTime = false
                            )

                        } catch (e: MombitAPIException) {
                            println(e)
                            sendMessage(
                                e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                inlineButtons = listOf(
                                    "Поддержка" to "https://t.me/mombit_support"
                                ),
                            )
                        }

                        user.inDeal = true
                        userService.saveUser(user)

                    }

                    "acceptDeal" -> {
                        try {
                            deleteMessage(user.lastMessageId, user.chatId)
                            val deal = mombitAPI.createDeal(user)
                            user.dealId = deal!!.id

                            sendMessage(
                                "Произведите оплату:\n" + "Сумма: ${deal.amountFiat} RUB\n" + "Реквизиты для оплаты: ${deal.requisite}",
                                user.chatId,
                                startUserMark,
                                oneTime = false
                            )
                            val messageId = sendMessage(
                                "ℹ️ Оплатить покупку необходимо в течение 30 минут \n\n" + "ℹ️ Чтобы покупка прошла успешно, оплачивайте не больше и не меньше, чем указано в заявке \n" + "При возникновении вопросов обращайтесь в поддержку",
                                user.chatId,
                                markButtons = listOf(listOf("Меню")),
                                inlineButtons = listOf(
                                    "Я оплатил" to "paidDeal", "Отмена сделки" to "cancelDeal", "Поддержка" to "https://t.me/mombit_support"
                                ),
                                oneTime = false,
                                shielded = true
                            )

                            user.lastMessageId = messageId!!.toLong()
                            userService.saveUser(user)

                        } catch (e: MombitAPIException) {
                            println(e)
                            sendMessage(
                                e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                inlineButtons = listOf(
                                    "Поддержка" to "https://t.me/mombit_support"
                                ),
                            )
                        }

                    }

                    "paidDeal" -> {
                        editMessage(
                            "ℹ️ Оплатить покупку необходимо в течение 30 минут \n\n" + "ℹ️ Чтобы покупка прошла успешно, оплачивайте не больше и не меньше, чем указано в заявке \n" + "При возникновении вопросов обращайтесь в поддержку",
                            chatId = user.chatId,
                            messageId = user.lastMessageId,
                            inlineButtons = listOf(
                                "Отмена сделки" to "cancelDeal", "Поддержка" to "https://t.me/mombit_support"
                            ),
                            shielded = true
                        )

                        sendMessage(
                            "Проверяем оплату, это может занять около 5 минут",
                            user.chatId,
                            markButtons = listOf(listOf("Меню")),
                            oneTime = false
                        )


                        try {
                            mombitAPI.payDeal(user.dealId)
                        } catch (e: MombitAPIException) {
                            println(e)
                            sendMessage(
                                e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                inlineButtons = listOf(
                                    "Поддержка" to "https://t.me/mombit_support"
                                ),
                            )
                        }

                    }

                    "cancelDeal" -> {
                        try {
                            mombitAPI.cancelDeal(user.dealId)
                        } catch (e: MombitAPIException) {
                            println(e)
                            sendMessage(
                                e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                inlineButtons = listOf(
                                    "Поддержка" to "https://t.me/mombit_support"
                                ),
                            )
                        }

                    }

                    "dispute" -> {
                        try {
                            mombitAPI.disputeDeal(user.dealId)
                        } catch (e: MombitAPIException) {
                            println(e)
                            sendMessage(
                                e.message.toString(), user.chatId, listOf(listOf("Меню")), oneTime = false,
                                inlineButtons = listOf(
                                    "Поддержка" to "https://t.me/mombit_support"
                                ),
                            )
                        }

                        userService.cleanUser(user)

                    }
                }
            }
        }
    }

    suspend fun processCandidateUpdate(update: Update) {
        GlobalScope.launch(Dispatchers.Default) {
            if (update.hasMessage()) {
                if (update.message.hasContact()) {

                    val user = User(
                        chatId = update.message.chatId,
                        tgName = update.message.from.userName,
                        token = CodeGenerator.generateRandomCode(),
                        phoneNumber = update.message.contact.phoneNumber
                    )


                    userService.saveUser(user)
                    mombitAPI.signUpUser(user)

                    sendMessage(
                        "@${update.message.from.userName}!\nВыберите действие:",
                        update.message.chatId,
                        startUserMark,
                        oneTime = false,
                    )

                } else {

                    sendMessage(
                        "Mombit приветствует вас, @${update.message.from.userName}!\n" + "Пожалуйста, ознакомьтесь с пользовательским соглашением, и, если вы согласны, поделитесь своим номером:",
                        update.message.chatId,
                        listOf(listOf()),
                        oneTime = false,
                        sendContact = true
                    )

                    sendDocument(
                        text = "",
                        chatId = update.message.chatId,
                        document = File("/root/mombit/static/Mombit.pdf")
                    )
                }

            }
        }
    }

}