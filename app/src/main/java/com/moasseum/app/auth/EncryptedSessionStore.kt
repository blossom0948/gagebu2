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

class EncryptedSessionStore(context: Context) : SessionStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "auth-session.enc"))
    private val alias = "moasseum-session-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    @Synchronized override fun read(): AuthSession? {
        if (!file.baseFile.exists()) return null
        val bytes = file.openRead().use { it.readBytes() }
        require(bytes.size in 30..65536)
        val buffer = ByteBuffer.wrap(bytes); val ivSize = buffer.get().toInt()
        require(ivSize == 12)
        val iv = ByteArray(ivSize).also { buffer.get(it) }; val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        val data = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        return AuthSession(SignedInUser(data.getString("id"), data.getString("email")), data.getString("access"), data.getString("refresh"), data.getLong("expires"))
    }
    @Synchronized override fun write(session: AuthSession) {
        val text = JSONObject().put("id", session.user.id).put("email", session.user.email).put("access", session.accessToken).put("refresh", session.refreshToken).put("expires", session.expiresAt).toString()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        val bytes = ByteBuffer.allocate(1 + cipher.iv.size + encrypted.size).put(cipher.iv.size.toByte()).put(cipher.iv).put(encrypted).array()
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) } catch (error: Throwable) { file.failWrite(output); throw error }
    }
    @Synchronized override fun clear() { file.delete() }
}
