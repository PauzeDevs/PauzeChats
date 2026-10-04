package com.pauze.chats

import android.app.Application
import com.pauze.chats.account.NetworkAuthRepository
import com.pauze.chats.account.NetworkSocialRepository

class PauzeChatsApplication : Application() {
    lateinit var authRepository: NetworkAuthRepository
        private set

    lateinit var socialRepository: NetworkSocialRepository
        private set

    override fun onCreate() {
        super.onCreate()

        authRepository = NetworkAuthRepository(this)
        socialRepository = NetworkSocialRepository(authRepository)
    }
}
