package io.github.subhayan0022.authenticator.data

import androidx.room.TypeConverter

object Converters {

    @TypeConverter
    fun fromOtpType(type: OtpType): String = type.name

    @TypeConverter
    fun toOtpType(value: String): OtpType = OtpType.valueOf(value)
}
