package io.github.subhayan0022.authenticator.crypto

interface SecretCipher {

    fun encrypt(plaintext: ByteArray): EncryptedSecret

    fun decrypt(encrypted: EncryptedSecret): ByteArray
}
