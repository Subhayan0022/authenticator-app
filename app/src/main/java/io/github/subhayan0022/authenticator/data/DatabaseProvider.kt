package io.github.subhayan0022.authenticator.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    private const val DATABASE_NAME = "authenticator.db"

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `account_groups` " +
                    "(`name` TEXT NOT NULL, PRIMARY KEY(`name`))",
            )
        }
    }

    @Volatile
    private var instance: AuthenticatorDatabase? = null

    fun get(context: Context): AuthenticatorDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AuthenticatorDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
}
