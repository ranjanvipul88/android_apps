package com.ranjanvipul.relayguard.di

import android.content.Context
import android.telephony.SmsManager
import androidx.room.Room
import com.google.gson.Gson
import com.ranjanvipul.relayguard.data.local.EntityMappers
import com.ranjanvipul.relayguard.data.local.RelayGuardDatabase
import com.ranjanvipul.relayguard.data.network.RelayApi
import com.ranjanvipul.relayguard.data.repository.RoomFilterRepository
import com.ranjanvipul.relayguard.data.repository.RoomMessageLogRepository
import com.ranjanvipul.relayguard.data.security.AndroidSecretStore
import com.ranjanvipul.relayguard.data.security.SecretStore
import com.ranjanvipul.relayguard.data.transport.ChatWebhookRelayTransport
import com.ranjanvipul.relayguard.data.transport.CompositeRelayTransport
import com.ranjanvipul.relayguard.data.transport.SmsRelayTransport
import com.ranjanvipul.relayguard.data.transport.StubRelayTransport
import com.ranjanvipul.relayguard.data.transport.WebhookRelayTransport
import com.ranjanvipul.relayguard.domain.repository.FilterRepository
import com.ranjanvipul.relayguard.domain.repository.MessageLogRepository
import com.ranjanvipul.relayguard.domain.repository.RelayTransport
import com.ranjanvipul.relayguard.domain.usecase.EvaluateMessageUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RelayGuardDatabase =
        Room.databaseBuilder(context, RelayGuardDatabase::class.java, "relayguard.db").build()

    @Provides
    fun provideMappers(): EntityMappers = EntityMappers(Gson())

    @Provides
    @Singleton
    fun provideFilterRepository(db: RelayGuardDatabase, mappers: EntityMappers): FilterRepository =
        RoomFilterRepository(db.filterDao(), mappers)

    @Provides
    @Singleton
    fun provideMessageRepository(db: RelayGuardDatabase, mappers: EntityMappers): MessageLogRepository =
        RoomMessageLogRepository(db.messageLogDao(), mappers)

    @Provides
    @Singleton
    fun provideSecretStore(@ApplicationContext context: Context): SecretStore = AndroidSecretStore(context)

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRelayApi(client: OkHttpClient): RelayApi = Retrofit.Builder()
        .baseUrl("https://example.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RelayApi::class.java)

    @Provides
    @Singleton
    fun provideRelayTransport(@ApplicationContext context: Context, api: RelayApi, secrets: SecretStore): RelayTransport =
        CompositeRelayTransport(
            sms = SmsRelayTransport { context.getSystemService(SmsManager::class.java) },
            webhook = WebhookRelayTransport(api),
            email = StubRelayTransport("Email delivery"),
            chat = ChatWebhookRelayTransport(api, secrets)
        )

    @Provides
    fun provideEvaluateMessageUseCase(): EvaluateMessageUseCase = EvaluateMessageUseCase()
}
