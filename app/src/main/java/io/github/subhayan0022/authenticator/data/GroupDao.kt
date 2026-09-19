package io.github.subhayan0022.authenticator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT name FROM account_groups ORDER BY name ASC")
    fun observeNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: GroupEntity)

    @Query("DELETE FROM account_groups WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("UPDATE accounts SET group_name = :new WHERE group_name = :old")
    suspend fun moveAccounts(old: String, new: String?)

    @Transaction
    suspend fun rename(old: String, new: String) {
        insert(GroupEntity(new))
        moveAccounts(old, new)
        deleteByName(old)
    }

    @Transaction
    suspend fun remove(name: String) {
        moveAccounts(name, null)
        deleteByName(name)
    }
}
