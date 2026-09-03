package io.github.subhayan0022.authenticator.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private const val DATABASE_NAME = "authenticator.db"

    @Volatile
    private var instance: AuthenticatorDatabase? = null

    fun get(context: Context): AuthenticatorDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AuthenticatorDatabase::class.java,
                DATABASE_NAME,
            ).build().also { instance = it }
        }
}
