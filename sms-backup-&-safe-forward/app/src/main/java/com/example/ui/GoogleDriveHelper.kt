package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.ByteArrayContent
import java.io.ByteArrayOutputStream
import com.google.api.services.drive.model.File

class GoogleDriveHelper(private val context: Context) {

    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("Safe Backup Forward")
        .build()
    }

    suspend fun uploadBackup(account: GoogleSignInAccount, jsonData: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            
            // Search if file already exists to overwrite it
            val result = driveService.files().list()
                .setSpaces("drive")
                .setQ("name='safeforward_backup.json' and trashed=false")
                .execute()
                
            val fileList = result.files
            
            val fileMetadata = File()
            fileMetadata.name = "safeforward_backup.json"
            
            val mediaContent = ByteArrayContent.fromString("application/json", jsonData)
            
            if (fileList != null && fileList.isNotEmpty()) {
                val fileId = fileList[0].id
                driveService.files().update(fileId, fileMetadata, mediaContent).execute()
            } else {
                driveService.files().create(fileMetadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadBackup(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            
            val result = driveService.files().list()
                .setSpaces("drive")
                .setQ("name='safeforward_backup.json' and trashed=false")
                .execute()
                
            val fileList = result.files
            if (fileList == null || fileList.isEmpty()) {
                return@withContext null // File not found
            }
            
            val fileId = fileList[0].id
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            String(outputStream.toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
