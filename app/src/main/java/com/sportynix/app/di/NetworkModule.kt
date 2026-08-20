package com.sportynix.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.network.AuthInterceptor
import com.sportynix.app.core.network.TokenAuthenticator
import com.sportynix.app.data.remote.api.AuthApiService
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.api.ChatApiService
import com.sportynix.app.data.remote.api.ChallengeApiService
import com.sportynix.app.data.remote.api.LocationApiService
import com.sportynix.app.data.remote.api.NotificationApiService
import com.sportynix.app.data.remote.api.SportsApiService
import com.sportynix.app.data.remote.api.TeamApiService
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.data.remote.api.VenueApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVenueApiService(retrofit: Retrofit): VenueApiService {
        return retrofit.create(VenueApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBookingApiService(retrofit: Retrofit): BookingApiService {
        return retrofit.create(BookingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSportsApiService(retrofit: Retrofit): SportsApiService {
        return retrofit.create(SportsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTeamApiService(retrofit: Retrofit): TeamApiService {
        return retrofit.create(TeamApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService {
        return retrofit.create(NotificationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChallengeApiService(retrofit: Retrofit): ChallengeApiService {
        return retrofit.create(ChallengeApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLeagueApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.LeagueApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.LeagueApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTournamentApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.TournamentApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.TournamentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCricketScoringApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.CricketScoringApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.CricketScoringApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuctionApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.AuctionApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.AuctionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnnouncementApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.AnnouncementApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.AnnouncementApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.SearchApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.SearchApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePlayerStatsApiService(retrofit: Retrofit): com.sportynix.app.data.remote.api.PlayerStatsApiService {
        return retrofit.create(com.sportynix.app.data.remote.api.PlayerStatsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLocationApiService(retrofit: Retrofit): LocationApiService {
        return retrofit.create(LocationApiService::class.java)
    }
}
