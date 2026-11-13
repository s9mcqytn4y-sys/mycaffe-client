package com.azuredevid.cassy.core.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

interface AeadCipher {
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray
}

class AndroidTinkAeadCipher(context: Context) : AeadCipher {

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context.applicationContext, TINK_KEYSET_NAME, TINK_PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        aead.encrypt(plaintext, associatedData)

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        aead.decrypt(ciphertext, associatedData)

    companion object {
        private const val TINK_KEYSET_NAME = "cassy_session_keyset"
        private const val TINK_PREF_FILE = "cassy_tink_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://cassy_master_key"
    }
}
