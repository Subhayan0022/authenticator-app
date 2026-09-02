package io.github.subhayan0022.authenticator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findById(id: Long): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("UPDATE accounts SET counter = counter + 1 WHERE id = :id")
    suspend fun incrementCounter(id: Long)

    @Query("SELECT counter FROM accounts WHERE id = :id")
    suspend fun counterOf(id: Long): Long?

    @Transaction
    suspend fun nextCounter(id: Long): Long? {
        incrementCounter(id)
        return counterOf(id)
    }
}
