package org.sosnetwork.app.mesh

import android.content.Context
import java.io.File

object VoiceNoteStorage {
    fun save(context: Context, alertId: String, bytes: ByteArray): String {
        val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
        val file = File(dir, "$alertId.m4a")
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
