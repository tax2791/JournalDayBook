package com.kushal.mealapp.database

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase

class MyApp : Application() {

    companion object {
        private var dbInstance: AppDatabase? = null
        private const val TAG = "MyApp"

        fun getDatabase(context: Context): AppDatabase {
            return dbInstance ?: synchronized(this) {
                val dbFile = context.getDatabasePath("app_database")
                Log.d(TAG, "Initializing DB from file: ${dbFile.absolutePath}, size: ${dbFile.length()} bytes")

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .fallbackToDestructiveMigration(false)
                    .build()

                dbInstance = instance
                instance
            }
        }

        fun clearDatabaseInstance() {
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

        fun isDatabaseOpen(): Boolean = dbInstance?.isOpen ?: false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App launched")
    }
}
