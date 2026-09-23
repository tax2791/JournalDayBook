@file:Suppress("DEPRECATION")

package com.kushal.mealapp

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.kushal.mealapp.database.MyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.google.api.services.drive.model.File as DriveFile

object DriveBackupManager {

    private const val BACKUP_FILE_NAME = "MealAppBackup.db"
    private const val MIME_TYPE = "application/x-sqlite3"
    private const val TAG = "DriveBackupManager"
    private const val MIN_VALID_DB_SIZE = 8192L

    private fun getDriveService(account: GoogleSignInAccount, context: Context): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            .apply { selectedAccount = account.account }

        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("MealApp")
            .build()
    }

    /** Upload local database to Drive */
    suspend fun uploadDatabaseToDrive(account: GoogleSignInAccount, context: Context) = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("app_database")
            if (!dbFile.exists() || dbFile.length() < MIN_VALID_DB_SIZE) {
                Log.e(TAG, "Invalid local DB, upload skipped")
                return@withContext
            }

            MyApp.clearDatabaseInstance()

            val driveService = getDriveService(account, context)
            findExistingBackup(driveService)?.let {
                driveService.files().delete(it).execute()
                Log.d(TAG, "Old backup deleted")
            }

            val metadata = DriveFile().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }

            val content = ByteArrayContent(MIME_TYPE, dbFile.readBytes())
            driveService.files().create(metadata, content).setFields("id,modifiedTime").execute()
            Log.d(TAG, "Database uploaded successfully (${dbFile.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
        }
    }

    /** Restore database from Drive */
    suspend fun restoreDatabaseFromDrive(account: GoogleSignInAccount, context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account, context)
            val fileId = findExistingBackup(driveService) ?: return@withContext false

            val tempFile = File(context.cacheDir, "temp_restore.db")
            driveService.files().get(fileId).executeMediaAndDownloadTo(FileOutputStream(tempFile))

            if (tempFile.length() < MIN_VALID_DB_SIZE) {
                tempFile.delete()
                return@withContext false
            }

            val dbFile = context.getDatabasePath("app_database")
            MyApp.clearDatabaseInstance()
            listOf(dbFile, File(dbFile.parent, "app_database-shm"), File(dbFile.parent, "app_database-wal")).forEach { if (it.exists()) it.delete() }

            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()
            Log.d(TAG, "Database restored successfully")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            return@withContext false
        }
    }

    /** Find existing backup in appDataFolder */
    private fun findExistingBackup(driveService: Drive): String? {
        return try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .setFields("files(id,name,modifiedTime)")
                .execute()
            result.files.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup", e)
            null
        }
    }
}
