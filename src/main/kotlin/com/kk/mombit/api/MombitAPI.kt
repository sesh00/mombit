package com.kk.mombit.api

import com.google.gson.Gson
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.and
import com.kk.mombit.api.entity.*
import com.kk.mombit.user.User
import com.kk.mombit.utils.Constants
import com.kk.mombit.utils.OkHttpUtils
import com.kk.mombit.utils.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import javax.annotation.PostConstruct


class MombitAPIException(s: String) : Exception(s)

@Component
class MombitWSAPI(
    @Value("\${mombitws.token}")
    val apiToken: String) {
    var gotDealUpdate: (suspend (String) -> Unit)? = null
    var gotNotifyUpdate: (suspend (String) -> Unit)? = null

    val okHttpClient = OkHttpClient.Builder().build()

    companion object {
        private const val BASE_URL = "snicks.online"
    }

    @PostConstruct
    fun iinit() {
        reconnectDeal()
        reconnectNotification()
    }

    private val dealWebSocketBuilder: WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            webSocket.send(Gson().toJson(WSHandshake(apiToken)))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    println(text)

                    if (text == "ping") {
                        webSocket.send("pong")
                        return@launch
                    }

                    gotDealUpdate?.invoke(text)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.printStackTrace()

            reconnectDeal()
        }
    }

    fun reconnectDeal() {
        okHttpClient.newWebSocket(Request.Builder().apply {
            url("wss://$BASE_URL/deal/")
        }.build(), dealWebSocketBuilder)
    }

    private val notificationWebSocketBuilder = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            webSocket.send(Gson().toJson(WSHandshake(apiToken)))
        }
        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    println(text)

                    if (text == "ping") {
                        webSocket.send("pong")
                        return@launch
                    }

                    gotNotifyUpdate?.invoke(text)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.printStackTrace()

            reconnectNotification()
        }
    }

    fun reconnectNotification() {
        okHttpClient.newWebSocket(Request.Builder().apply {
            url("wss://$BASE_URL/notification/")
        }.build(), notificationWebSocketBuilder)
    }
}

@Component
class MombitAPI(
    @Value("\${mombit.token}")
    val apiToken: String)
{
    private val okHttpClient = OkHttpProvider().httpClient

    private val gson = Gson()

    inner class OkHttpProvider {
        val httpClient =
            OkHttpClient.Builder()
                .addNetworkInterceptor(TokenInterceptor())
                .connectTimeout(Duration.ofMinutes(1))
                .build()


        inner class TokenInterceptor() : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val newReq = chain.request().newBuilder()
                    .addHeader("Authorization", apiToken)
                    .build()
                return chain.proceed(newReq)
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://snicks.online"
    }


    suspend fun signUpUser(user: User): String {

        val json = gson.toJson(SignUpRequest(token = user.token,
            tgName = user.tgName.toString(), phone = user.phoneNumber))

        val endpoint = "/api/mombit/signup"

        val request = Request.Builder()
            .post(json.toRequestBody(OkHttpUtils.JSON_TYPE))
            .url("$BASE_URL$endpoint")
            .build()
        val (code, body) = OkHttpUtils.makeAsyncRequest(okHttpClient, request)
        if(code == 200){
            return body!!
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))


    }

    suspend fun getBalance(user: User): BalanceResponse? {

        val endpoint = "/api/mombit/${user.token}/balance"
        val request = Request.Builder()
            .get()
            .url("$BASE_URL$endpoint")
            .build()


        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return gson.fromJson(body,
                BalanceResponse::class.java)
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))

    }
    suspend fun checkAddress(address: String): Boolean?{
        val endpoint = "/api/mombit/checkAddress/${address}"

        val request = Request.Builder()
            .get()
            .url("$BASE_URL$endpoint")
            .build()


        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return body.toBoolean()
        }

        throw MombitAPIException(JSONObject(body.toString()).getString("message"))


    }

    suspend fun getRate(): RateResponse? {
        val endpoint = "/api/mombit/rate"

        val request = Request.Builder()
            .get()
            .url("$BASE_URL$endpoint")
            .build()


        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return gson.fromJson(body,
                RateResponse::class.java)
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))

    }
    suspend fun createDeal(user: User): CreateDealResponse? {
        val json = gson.toJson(CreateDealRequest(
            token = user.token,
            amount = user.dealAmount,
            isCrypto = user.dealCurrency.isCrypto,
            address = user.dealAddress))

        val endpoint = "/api/mombit/createDeal"

        val request = Request.Builder()
            .post(json.toRequestBody(OkHttpUtils.JSON_TYPE))
            .url("$BASE_URL$endpoint")
            .build()

        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return gson.fromJson(body,
                CreateDealResponse::class.java)
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }

    suspend fun calculateDeal(user: User): CreateDealResponse? {
        val json = gson.toJson(CreateDealRequest(
            token = user.token,
            amount = user.dealAmount,
            isCrypto = user.dealCurrency.isCrypto,
            address = user.dealAddress))

        val endpoint = "/api/mombit/calculate"

        val request = Request.Builder()
            .post(json.toRequestBody(OkHttpUtils.JSON_TYPE))
            .url("$BASE_URL$endpoint")
            .build()

        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return gson.fromJson(body,
                CreateDealResponse::class.java)
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }


    suspend fun cancelDeal(id: Long): String {

        val endpoint = "/api/mombit/cancelDeal/$id"

        val request = Request.Builder()
            .post(ByteArray(0).toRequestBody(null))
            .url("$BASE_URL$endpoint")
            .build()

        val (code, body) = OkHttpUtils.makeAsyncRequest(okHttpClient, request)
        if(code == 200){
            return body!!
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }

    suspend fun disputeDeal(id: Long): String {

        val endpoint = "/api/mombit/disputeDeal/$id"

        val request = Request.Builder()
            .post(ByteArray(0).toRequestBody(null))
            .url("$BASE_URL$endpoint")
            .build()

        val (code, body) = OkHttpUtils.makeAsyncRequest(okHttpClient, request)
        if(code == 200){
            return body!!
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }


    suspend fun payDeal(id: Long): String {

        val endpoint = "/api/mombit/payDeal/$id"

        val request = Request.Builder()
            .post(ByteArray(0).toRequestBody(null))
            .url("$BASE_URL$endpoint")
            .build()

        val (code, body) = OkHttpUtils.makeAsyncRequest(okHttpClient, request)
        if(code == 200){
            return body!!
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }


    suspend fun getDeal(id: Long): DealResponse?{
        val endpoint = "/api/mombit/deal/${id}"

        val request = Request.Builder()
            .get()
            .url("$BASE_URL$endpoint")
            .build()


        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return gson.fromJson(body,
                DealResponse::class.java)
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }

    suspend fun getMinMaxBalance():MinMaxResponse? {
        val endpoint = "/api/mombit/minmax"

        val request = Request.Builder()
            .get()
            .url("$BASE_URL$endpoint")
            .build()


        var (code, body) = OkHttpUtils.makeAsyncRequest(
            okHttpClient,
            request
        )

        if(code == 200){
            return gson.fromJson(body,
                MinMaxResponse::class.java)
        }
        throw MombitAPIException(JSONObject(body.toString()).getString("message"))
    }

}