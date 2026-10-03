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
                    val dbFile = context.getDatabasePath("app_database")
                    Log.d(TAG, "Initializing DB from file: ${dbFile.absolutePath}, size: ${if (dbFile.exists()) dbFile.length() else 0} bytes")

                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    )
                        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                        .fallbackToDestructiveMigration(true)
                        .build()

                    dbInstance = instance
                    instance
                }
            }
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
    }
}
