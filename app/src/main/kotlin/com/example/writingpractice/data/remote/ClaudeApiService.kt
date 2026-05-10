package com.example.writingpractice.data.remote

import com.example.writingpractice.data.remote.dto.ClaudeRequest
import com.example.writingpractice.data.remote.dto.ClaudeResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApiService {
    @POST("v1/messages")
    @Headers("anthropic-version: 2023-06-01")
    suspend fun complete(@Body request: ClaudeRequest): ClaudeResponse
}
