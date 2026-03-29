package com.ares.ewe_man.di

import com.ares.ewe_man.BuildConfig
import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.data.remote.TokenRefreshInterceptor
import com.ares.ewe_man.data.remote.api.DobbyGoApi
import com.ares.ewe_man.data.remote.api.GoogleDirectionsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    @DobbyGoNoAuthClient
    fun provideNoAuthOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        tokenRefreshInterceptor: TokenRefreshInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            if (req.header("Authorization") != null) {
                chain.proceed(req)
            } else {
                val token = runBlocking { sessionManager.authToken.first() }
                val request = if (!token.isNullOrBlank()) {
                    req.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else req
                chain.proceed(request)
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDobbyGoApi(retrofit: Retrofit): DobbyGoApi {
        return retrofit.create(DobbyGoApi::class.java)
    }

    @Provides
    @Singleton
    @Named("GoogleRetrofit")
    fun provideGoogleRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleDirectionsApi(@Named("GoogleRetrofit") retrofit: Retrofit): GoogleDirectionsApi {
        return retrofit.create(GoogleDirectionsApi::class.java)
    }
}
