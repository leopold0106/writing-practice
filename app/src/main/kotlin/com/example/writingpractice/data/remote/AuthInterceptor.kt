package com.example.writingpractice.data.remote

import com.example.writingpractice.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository
) : Interceptor {
    // runBlocking is safe here: OkHttp runs on its own thread pool (not a coroutine dispatcher),
    // and DataStore uses a separate ProcessingScope — no deadlock possible.
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = runBlocking { settingsRepository.getApiKey() }
        val request = chain.request().newBuilder()
            .addHeader("x-api-key", key)
            .build()
        return chain.proceed(request)
    }
}
