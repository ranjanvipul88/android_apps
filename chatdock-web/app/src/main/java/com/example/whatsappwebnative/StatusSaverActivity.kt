package com.example.whatsappwebnative

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class StatusSaverActivity : AppCompatActivity() {

    private lateinit var savedInfoView: TextView
    private var lastSavedUri: Uri? = null

    private val mediaPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        var savedCount = 0
        uris.forEach { uri ->
            if (saveStatusMedia(uri) != null) savedCount++
        }
        savedInfoView.text = resources.getQuantityString(R.plurals.saved_status_count, savedCount, savedCount)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_saver)

        savedInfoView = findViewById(R.id.statusSaverInfo)
        findViewById<Button>(R.id.pickStatusMediaButton).setOnClickListener {
            mediaPicker.launch(arrayOf("image/*", "video/*"))
        }
        findViewById<Button>(R.id.repostStatusMediaButton).setOnClickListener {
            repostLastSaved()
        }
    }

    private fun saveStatusMedia(source: Uri): Uri? {
        val mimeType = contentResolver.getType(source) ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val displayName = "chatdock_status_${System.currentTimeMillis()}.$extension"

        val destination = createDestinationUri(mimeType, displayName) ?: return null

        runCatching {
            contentResolver.openInputStream(source).use { input ->
                contentResolver.openOutputStream(destination).use { output ->
                    if (input == null || output == null) error("Unable to open media stream")
                    input.copyTo(output)
                }
            }
        }.onFailure {
            Toast.makeText(this, R.string.status_save_failed, Toast.LENGTH_SHORT).show()
            return null
        }

        lastSavedUri = destination
        Toast.makeText(this, R.string.status_saved, Toast.LENGTH_SHORT).show()
        return destination
    }

    private fun createDestinationUri(mimeType: String, displayName: String): Uri? {
        val collection = when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ChatDock Status Saver")
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ChatDock Status Saver")
                directory.mkdirs()
                put(MediaStore.MediaColumns.DATA, File(directory, displayName).absolutePath)
            }
        }

        return contentResolver.insert(collection, values)
    }

    private fun repostLastSaved() {
        val uri = lastSavedUri
        if (uri == null) {
            Toast.makeText(this, R.string.no_saved_status_to_repost, Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.repost_status)))
    }
}
