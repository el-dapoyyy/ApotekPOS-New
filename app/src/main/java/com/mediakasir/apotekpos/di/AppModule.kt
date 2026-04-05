package com.mediakasir.apotekpos.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mediakasir.apotekpos.BuildConfig
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.data.network.AuthInterceptor
import dagger.Module as DaggerModule
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@DaggerModule
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val vhost = BuildConfig.DEV_API_HOST_HEADER.trim()
                val url = original.url
                val host = url.host.lowercase()
                val effectivePort = when {
                    url.port != -1 -> url.port
                    url.scheme == "https" -> 443
                    else -> 80
                }
                // php artisan serve / Sail, dll. — jangan timpa Host dengan nama vhost Laragon
                val isArtisanLikePort = effectivePort == 8000 || effectivePort == 8001
                val isLoopback =
                    host == "127.0.0.1" || host == "localhost" || host == "::1" ||
                        host == "0:0:0:0:0:0:0:1"
                val isLanOrEmulatorHost =
                    host == "10.0.2.2" ||
                        host.matches(Regex("^192\\.168\\.\\d{1,3}\\.\\d{1,3}$")) ||
                        host.matches(Regex("^10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
                val needsVhost = vhost.isNotEmpty() &&
                    !isLoopback &&
                    !isArtisanLikePort &&
                    isLanOrEmulatorHost
                val request = if (needsVhost) {
                    original.newBuilder().header("Host", vhost).build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
