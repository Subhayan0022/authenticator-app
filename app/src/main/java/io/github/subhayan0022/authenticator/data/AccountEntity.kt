package io.github.subhayan0022.authenticator.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.subhayan0022.authenticator.crypto.EncryptedSecret

enum class OtpType { TOTP, HOTP }

@Entity(
    tableName = "accounts",
    indices = [Index("group_name")],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val issuer: String,
    val label: String,

    @ColumnInfo(name = "group_name")
    val groupName: String? = null,

    @Embedded(prefix = "secret_")
    val secret: EncryptedSecret,

    val type: OtpType = OtpType.TOTP,
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,

    @ColumnInfo(name = "period_seconds")
    val periodSeconds: Int = 30,

    val counter: Long = 0,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)