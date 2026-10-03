@file:Suppress("DEPRECATION")

package com.kushal.mealapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import database.MyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.google.api.services.drive.model.File as DriveFile

object DriveBackupManager {

    private const val BACKUP_FILE_NAME = "MealAppBackup.db"
    private const val MIME_TYPE = "application/x-sqlite3"
    private const val TAG = "DriveBackupManager"
    private const val MIN_VALID_DB_SIZE = 8192L

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private fun getDriveService(account: GoogleSignInAccount, context: Context): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            .apply { selectedAccount = account.account }

        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("MealApp")
            .build()
    }

    private const val PREF_AUTO_SYNC_ENABLED = "auto_sync_enabled"

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("meal_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("meal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_AUTO_SYNC_ENABLED, enabled).apply()
    }

    /** 🌐 Network Availability Check */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /** ⚡ Automatic Background Sync
     * Checks if Auto-Sync is enabled in settings, internet is connected, and a Google account is signed in.
     * If enabled, uploads/syncs the database in the background without interrupting the user.
     */
    fun syncAutomatically(context: Context) {
        if (!isAutoSyncEnabled(context)) {
            Log.d(TAG, "⚡ Auto-sync skipped: Disabled by user setting")
            return
        }

        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "⚡ Auto-sync skipped: No active internet connection")
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.d(TAG, "⚡ Auto-sync skipped: No Google account signed in")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            try {
                uploadDatabaseToDriveInternal(account, context)
                Log.d(TAG, "⚡ Auto-sync: Database backup synced to Google Drive successfully")
            } catch (e: Exception) {
                Log.e(TAG, "⚡ Auto-sync failed", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /** 👆 Manual Upload: Upload local database to Drive */
    suspend fun uploadDatabaseToDrive(account: GoogleSignInAccount, context: Context) = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            uploadDatabaseToDriveInternal(account, context)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun uploadDatabaseToDriveInternal(account: GoogleSignInAccount, context: Context) {
        try {
            val dbFile = context.getDatabasePath("app_database")
            if (!dbFile.exists() || dbFile.length() < MIN_VALID_DB_SIZE) {
                Log.e(TAG, "Invalid local DB, upload skipped")
                return
            }

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
        } catch (e: UserRecoverableAuthIOException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
        }
    }

    /** 👆 Manual Restore: Restore database from Drive */
    suspend fun restoreDatabaseFromDrive(account: GoogleSignInAccount, context: Context): Boolean = withContext(Dispatchers.IO) {
        _isSyncing.value = true
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
        } catch (e: UserRecoverableAuthIOException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            return@withContext false
        } finally {
            _isSyncing.value = false
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
        } catch (e: UserRecoverableAuthIOException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup", e)
            null
        }
    }
}

/** ⚡ Composable Overlay that displays a subtle top indicator when Auto Sync is in progress.
 * Runs smoothly in the background without blocking touch inputs or interrupting user workflow.
 */
@Composable
fun AutoSyncFlashOverlay() {
    val syncing by DriveBackupManager.isSyncing.collectAsState()

    if (syncing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Syncing in background...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}