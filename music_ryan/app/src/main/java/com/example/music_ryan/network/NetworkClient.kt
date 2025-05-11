package com.example.music_ryan.network

import android.content.Context
import com.example.music_ryan.MusicApplication
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https://hotfix-service-prod.g.mi.com/"
    private lateinit var applicationContext: Context
    
    // 初始化方法，应在应用启动时调用
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    // 使用MusicApplication提供的全局OkHttpClient
    private val okHttpClient by lazy {
        // 添加日志拦截器到全局OkHttpClient
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // 在全局OkHttpClient的基础上添加特定的网络拦截器
        MusicApplication.getOkHttpClient(applicationContext as android.app.Application)
            .newBuilder()
            .addInterceptor(loggingInterceptor)
            // 添加超时设置，确保请求不会无限等待
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    // 取消所有网络请求
    fun cancelAllRequests() {
        try {
            okHttpClient.dispatcher.cancelAll()
            println("NetworkClient: 已取消所有进行中的网络请求")
        } catch (e: Exception) {
            println("NetworkClient: 取消网络请求时发生异常: ${e.message}")
        }
    }
} 