package com.moasseum.app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedPendingAuthStore(context: Context) : PendingAuthStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "auth-pending.enc"))
    private val alias = "moasseum-pkce-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    @Synchronized override fun read(): PendingAuthFlow? {
        if (!file.baseFile.exists()) return null
        require(file.baseFile.length() in 30..65536)
        val bytes = file.openRead().use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes)
        require(buffer.get().toInt() == 12)
        val iv = ByteArray(12).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        val data = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        return PendingAuthFlow(data.getString("email"), data.getString("verifier"), data.getString("flowId"), data.getBoolean("recovery"), data.getLong("createdAt"), data.getString("redirectUri"), data.optString("provider").takeIf { it.isNotBlank() && it != "null" })
    }
    @Synchronized override fun write(flow: PendingAuthFlow) {
        val text = JSONObject().put("email", flow.email).put("verifier", flow.verifier).put("flowId", flow.flowId).put("recovery", flow.recovery).put("createdAt", flow.createdAt).put("redirectUri", flow.redirectUri).put("provider", flow.provider ?: JSONObject.NULL).toString()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        val bytes = ByteBuffer.allocate(1 + cipher.iv.size + encrypted.size).put(cipher.iv.size.toByte()).put(cipher.iv).put(encrypted).array()
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) } catch (error: Throwable) { file.failWrite(output); throw error }
    }
    @Synchronized override fun clear() { file.delete() }
}
