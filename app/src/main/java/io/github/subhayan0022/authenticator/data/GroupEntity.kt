package io.github.subhayan0022.authenticator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_groups")
data class GroupEntity(
    @PrimaryKey
    val name: String,
)
