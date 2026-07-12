/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalePluginConfigurationCodecTest {

    private val codec = LocalePluginConfigurationCodec(TestSigner("current-key"))

    @Test
    fun `smart launch round trips`() {
        val configuration = LocalePluginConfiguration(
            operation = LocalePluginOperation.LAUNCH,
            scenarioId = 42L,
            isSmart = true,
        )

        assertEquals(configuration, codec.decode(codec.encode(configuration)))
    }

    @Test
    fun `dumb launch round trips`() {
        val configuration = LocalePluginConfiguration(
            operation = LocalePluginOperation.LAUNCH,
            scenarioId = 7L,
            isSmart = false,
        )

        assertEquals(configuration, codec.decode(codec.encode(configuration)))
    }

    @Test
    fun `stop round trips without scenario fields`() {
        val configuration = LocalePluginConfiguration(operation = LocalePluginOperation.STOP)

        assertEquals(configuration, codec.decode(codec.encode(configuration)))
    }

    @Test
    fun `tampered payload is rejected`() {
        val encoded = codec.encode(
            LocalePluginConfiguration(
                operation = LocalePluginOperation.LAUNCH,
                scenarioId = 42L,
                isSmart = true,
            )
        )

        assertNull(codec.decode(encoded.replace("\"scenarioId\":42", "\"scenarioId\":43")))
    }

    @Test
    fun `configuration signed by an old install key is rejected`() {
        val oldCodec = LocalePluginConfigurationCodec(TestSigner("old-key"))
        val encoded = oldCodec.encode(LocalePluginConfiguration(operation = LocalePluginOperation.STOP))

        assertNull(codec.decode(encoded))
    }

    @Test
    fun `malformed and missing fields are rejected`() {
        assertNull(codec.decode(null))
        assertNull(codec.decode("not-json"))
        assertNull(codec.decode("""{"version":1,"operation":"LAUNCH","signature":"00"}"""))
    }

    @Test
    fun `invalid configuration cannot be encoded`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode(LocalePluginConfiguration(operation = LocalePluginOperation.LAUNCH))
        }
    }
}

private class TestSigner(secret: String) : LocalePluginConfigurationSigner {
    private val key = SecretKeySpec(secret.toByteArray(), "HmacSHA256")

    override fun sign(payload: String): String = hmac(payload).toHex()

    override fun verify(payload: String, signature: String): Boolean =
        MessageDigest.isEqual(hmac(payload), signature.hexToBytes())

    private fun hmac(payload: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(key)
        doFinal(payload.toByteArray())
    }
}

private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
