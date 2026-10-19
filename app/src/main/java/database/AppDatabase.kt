package database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Meal::class, Deposit::class, Meal1::class, Member::class, LoanRecord::class, LoanRepayment::class, SmartNote::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE `member` ADD COLUMN `accountName` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `member` ADD COLUMN `accountType` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `loan_record` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `partyName` TEXT NOT NULL,
                `loanAmount` REAL NOT NULL,
                `interestRate` REAL NOT NULL DEFAULT 0.0,
                `loanDate` TEXT NOT NULL,
                `remarks` TEXT NOT NULL DEFAULT '',
                `phone` TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `loan_repayment` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `loanId` INTEGER NOT NULL DEFAULT 0,
                `partyName` TEXT NOT NULL,
                `amountReceived` REAL NOT NULL,
                `paymentDate` TEXT NOT NULL,
                `paymentMode` TEXT NOT NULL DEFAULT 'Cash',
                `remarks` TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `smart_note` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `tag` TEXT NOT NULL DEFAULT 'General',
                `date` INTEGER NOT NULL,
                `createdTime` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)
    }
}
