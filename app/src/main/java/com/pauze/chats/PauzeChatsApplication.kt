package com.pauze.chats

import android.app.Application
import com.pauze.chats.account.NetworkAuthRepository

class PauzeChatsApplication : Application() {
    lateinit var authRepository: NetworkAuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = NetworkAuthRepository(this)
    }
}
