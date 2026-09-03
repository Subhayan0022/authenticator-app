package io.github.subhayan0022.authenticator.data

data class Account(
    val id: Long,
    val issuer: String,
    val label: String,
    val groupName: String?,
    val type: OtpType,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
    val counter: Long,
    val sortOrder: Int,
)

internal fun AccountEntity.toAccount(): Account = Account(
    id = id,
    issuer = issuer,
    label = label,
    groupName = groupName,
    type = type,
    algorithm = algorithm,
    digits = digits,
    periodSeconds = periodSeconds,
    counter = counter,
    sortOrder = sortOrder,
)
