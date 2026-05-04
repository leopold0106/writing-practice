package com.example.writingpractice.data.remote

import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope
) : Interceptor {

    @Volatile private var cachedKey: String = ""

    init {
        scope.launch {
            settingsRepository.apiKey.collect { key ->
                cachedKey = key
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("x-api-key", cachedKey)
            .build()
        return chain.proceed(request)
    }
}
