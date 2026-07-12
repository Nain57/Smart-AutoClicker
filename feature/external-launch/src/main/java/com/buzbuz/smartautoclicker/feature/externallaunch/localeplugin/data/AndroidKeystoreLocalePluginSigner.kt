/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfigurationSigner
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidKeystoreLocalePluginSigner @Inject constructor() : LocalePluginConfigurationSigner {

    override fun sign(payload: String): String = hmac(payload).toHex()

    override fun verify(payload: String, signature: String): Boolean {
        val signatureBytes = signature.hexToByteArrayOrNull() ?: return false
        return MessageDigest.isEqual(hmac(payload), signatureBytes)
    }

    private fun hmac(payload: String): ByteArray =
        Mac.getInstance(HMAC_ALGORITHM).run {
            init(getOrCreateKey())
            doFinal(payload.toByteArray(Charsets.UTF_8))
        }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(HMAC_ALGORITHM, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                ).setDigests(KeyProperties.DIGEST_SHA256).build()
            )
            generateKey()
        }
    }
}

private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0 || any { it.digitToIntOrNull(16) == null }) return null
    return ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
}

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val HMAC_ALGORITHM = "HmacSHA256"
private const val KEY_ALIAS = "klickr_locale_plugin_hmac_v1"
