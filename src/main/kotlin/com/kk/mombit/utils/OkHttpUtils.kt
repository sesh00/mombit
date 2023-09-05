package com.kk.mombit.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await

internal inline fun <reified T> Gson.fromJson(json: String) =
    fromJson<T>(json, object : TypeToken<T>() {}.type)

object OkHttpUtils {
    val JSON_TYPE = "application/json".toMediaTypeOrNull()!!

    suspend fun makeAsyncRequest(
        client: OkHttpClient,
        request: Request,
        s: String = ""
    ): Pair<Int, String?> {
        val call = makeAsyncRequestRaw(client, request)
        val string = withContext(Dispatchers.Default) { call.body!!.string() }
        call.body?.close()
        call.close()

        DebugUtils.print(
            message = "Code: ${call.code}${System.lineSeparator()}$string",
            args = arrayOf(request.url.encodedPath, s)
        )

        return call.code to string
    }

    suspend fun makeAsyncRequestRaw(
        client: OkHttpClient,
        request: Request
    ): Response {
        return client.newCall(request).await()
    }

    fun makeRequest(client: OkHttpClient, request: Request): JSONObject? {
        val call = client.newCall(request).execute()

        val string = call.body!!.string()
        call.body?.close()
        call.close()

        return if (call.code == 200) JSONObject(string) else null
    }
}