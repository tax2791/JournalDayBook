package database

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase

class MyApp : Application() {

    companion object {
        @Volatile
        private var dbInstance: AppDatabase? = null
        private const val TAG = "MyApp"

        fun getDatabase(context: Context): AppDatabase {
            val current = dbInstance
            if (current != null && current.isOpen) {
                return current
            }
            return synchronized(this) {
                val existing = dbInstance
                if (existing != null && existing.isOpen) {
                    existing
                } else {
                    try {
                        createRoomDatabase(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error opening DB on launch, resetting stale instance...", e)
                        clearDatabaseInstance()
                        createRoomDatabase(context)
                    }
                }
            }
        }

        private fun createRoomDatabase(context: Context): AppDatabase {
            val dbFile = context.getDatabasePath("app_database")
            Log.d(TAG, "Initializing DB from file: ${dbFile.absolutePath}, size: ${if (dbFile.exists()) dbFile.length() else 0} bytes")

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()

            dbInstance = instance
            return instance
        }

        fun clearDatabaseInstance() {
            synchronized(this) {
                try {
                    dbInstance?.let {
                        if (it.isOpen) {
                            it.close()
                            Log.d(TAG, "Room DB closed safely")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing DB instance: ${e.message}")
                } finally {
                    dbInstance = null
                    Log.d(TAG, "Database instance cleared")
                }
            }
        }

        fun isDatabaseOpen(): Boolean = dbInstance?.isOpen ?: false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App launched")

        // 🧹 Automatically clear stale cache directory upon app update
        try {
            val prefs = getSharedPreferences("app_version_prefs", MODE_PRIVATE)
            val lastVersionCode = prefs.getInt("last_app_version_code", 0)
            val currentVersionCode = try {
                packageManager.getPackageInfo(packageName, 0).versionCode
            } catch (_: Exception) {
                35
            }

            if (lastVersionCode != 0 && lastVersionCode < currentVersionCode) {
                Log.d(TAG, "⚡ App updated ($lastVersionCode -> $currentVersionCode). Clearing stale cache...")
                clearDatabaseInstance()
                try {
                    cacheDir.deleteRecursively()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear stale cacheDir on app update", e)
                }
            }
            prefs.edit().putInt("last_app_version_code", currentVersionCode).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking version upgrade in MyApp", e)
        }
    }
}
