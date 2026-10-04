@file:Suppress("DEPRECATION")

package com.kushal.mealapp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.coroutines.launch


@Composable
fun GoogleSignInScreenContent(
    account: GoogleSignInAccount? = null,
    signIn: () -> Unit,
    signOut: () -> Unit,
    authLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("meal_prefs", Context.MODE_PRIVATE) }

    var restoring by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var autoSyncEnabled by remember { mutableStateOf(DriveBackupManager.isAutoSyncEnabled(context)) }

    val activeAccount = account ?: GoogleSignIn.getLastSignedInAccount(context)
    val userName = activeAccount?.displayName ?: "Guest"

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .padding(24.dp),
        color = surfaceColor
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (activeAccount != null) Icons.Default.AccountCircle else Icons.Default.Person,
                        contentDescription = "User Icon",
                        tint = primary,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = "Welcome, $userName!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (activeAccount != null) "Your Google account is connected" else "Please sign in to sync your data",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Crossfade(targetState = activeAccount != null) { isSignedIn ->
                if (!isSignedIn) {
                    // SIGN IN BUTTON
                    Button(
                        onClick = signIn,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in with Google", color = onPrimary)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        // --- Auto-Sync Toggle ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Sync to Drive",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (autoSyncEnabled) "Automatic sync on saving entries is ON" else "Auto-sync is turned OFF",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoSyncEnabled,
                                    onCheckedChange = { enabled ->
                                        autoSyncEnabled = enabled
                                        DriveBackupManager.setAutoSyncEnabled(context, enabled)
                                        Toast.makeText(
                                            context,
                                            if (enabled) "Auto-Sync Enabled ⚡" else "Auto-Sync Disabled ⏸️",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }

                        // --- Sign Out ---
                        OutlinedButton(
                            onClick = {
                                prefs.edit {
                                    putLong("last_synced_time", 0L)
                                    putLong("last_drive_sync_time", 0L)
                                }
                                signOut()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign Out", color = Color.Red)
                        }

                        // --- Manual Backup ---
                        Button(
                            onClick = {
                                scope.launch {
                                    isBackingUp = true
                                    try {
                                        DriveBackupManager.uploadDatabaseToDrive(activeAccount!!, context)
                                        val current = System.currentTimeMillis()
                                        prefs.edit {
                                            putLong("last_synced_time", current)
                                            putLong("last_drive_sync_time", current)
                                        }
                                        Toast.makeText(context, "Backup successful ✅", Toast.LENGTH_SHORT).show()
                                    } catch (e: UserRecoverableAuthIOException) {
                                        Log.e("DriveBackup", "Auth required for Backup", e)
                                        authLauncher?.launch(e.intent)
                                    } catch (e: Exception) {
                                        Log.e("DriveBackup", "Backup failed", e)
                                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isBackingUp = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            enabled = !isBackingUp
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Backing Up...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Backup to Drive")
                            }
                        }

                        // --- Manual Restore ---
                        Button(
                            onClick = {
                                scope.launch {
                                    restoring = true
                                    try {
                                        val restored = DriveBackupManager.restoreDatabaseFromDrive(activeAccount!!, context)
                                        if (restored) {
                                            Toast.makeText(context, "Restore completed 🔄 Restarting...", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(context, HomeActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "Restore failed or backup not found", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: UserRecoverableAuthIOException) {
                                        Log.e("DriveBackup", "Auth required for Restore", e)
                                        authLauncher?.launch(e.intent)
                                    } catch (e: Exception) {
                                        Log.e("DriveBackup", "Restore failed", e)
                                        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        restoring = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                            enabled = !restoring
                        ) {
                            if (restoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Restoring...")
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Restore from Drive")
                            }
                        }
                    }
                }
            }
        }
    }
}
