package com.example.music_ryan.network

import com.example.music_ryan.data.model.HomePageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("music/homePage")
    suspend fun getHomePage(
        @Query("current") current: Int = 1,
        @Query("size") size: Int = 4
    ): HomePageResponse
} 