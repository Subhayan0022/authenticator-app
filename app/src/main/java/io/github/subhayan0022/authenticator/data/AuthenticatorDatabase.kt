package io.github.subhayan0022.authenticator.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AccountEntity::class, GroupEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AuthenticatorDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun groupDao(): GroupDao
}
