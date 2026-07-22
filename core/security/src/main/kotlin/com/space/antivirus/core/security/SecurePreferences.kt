package com.space.antivirus.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted key-value store for anything sensitive enough to not belong in
 * plain DataStore (e.g. subscription entitlement cache, trusted-list
 * tokens). Routine settings still use core:data's DataStore wrapper — this
 * is deliberately not the default, only the escape hatch for genuinely
 * sensitive values. See docs/adr/0008-secure-preferences-scope.md.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)
    fun remove(key: String) = prefs.edit().remove(key).apply()
    fun clear() = prefs.edit().clear().apply()
}
