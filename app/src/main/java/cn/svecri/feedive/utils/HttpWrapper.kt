package cn.svecri.feedive.utils

import android.util.Log
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import java.io.IOException
import java.time.Duration

class HttpWrapper {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(Duration.ofMillis(2000))
        .readTimeout(Duration.ofMillis(5000))
        .build()

    fun fetchAsFlow(
        url: String,
        request: Request.Builder = Request.Builder(),
    ) = callbackFlow<Response> {
        val req = request.url(url).build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    trySend(response)
                        .onFailure {
                            Log.e("InfoFlow", "Send Response Failed to Flow", it)
                            channel.close()
                        }
                        .onSuccess {
                            channel.close()
                        }
                } else {
                    cancel("bad http code")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                cancel("okhttp error", e)
            }
        })
        awaitClose { }
    }
}