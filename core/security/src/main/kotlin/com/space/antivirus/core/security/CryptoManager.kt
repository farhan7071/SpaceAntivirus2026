package com.space.antivirus.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin abstraction point for any future field-level encryption need
 * beyond SecurePreferences (e.g. encrypting a cached scan-history export).
 * Deliberately not implemented beyond the interface in Sprint 003 — no
 * business logic depends on it yet, per the sprint's "no malware engine
 * implementation" rule.
 */
interface CryptoManager {
    fun encrypt(plainText: ByteArray): ByteArray
    fun decrypt(cipherText: ByteArray): ByteArray
}

@Singleton
class AndroidKeystoreCryptoManager @Inject constructor() : CryptoManager {
    override fun encrypt(plainText: ByteArray): ByteArray {
        TODO("Implement Android Keystore-backed AES-GCM in the feature module that first needs it")
    }

    override fun decrypt(cipherText: ByteArray): ByteArray {
        TODO("Implement Android Keystore-backed AES-GCM in the feature module that first needs it")
    }
}
