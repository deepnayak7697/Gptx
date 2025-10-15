package com.gptx.app
import android.app.Application
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class GPTXApp : Application() {
    companion object {
        lateinit var instance: GPTXApp
    }
    val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
