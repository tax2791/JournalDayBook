package database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "meal")
//currently this table is not used
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int =0,
    val date: String,
    val name: String,
    val amount: Double
)

@Entity(tableName = "deposit")
data class Deposit(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val name: String,
    val date: String,
    val amount: Double
)

@Entity(tableName = "meal1")
data class Meal1(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val item: String,
    val expenditure: String,
    val price: Double,
    val meal: Int,
    val date: Date
)

@Entity(tableName = "member")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String,                       // "Group Member" or "Personal Account"
    val accountName: String? = null,        // Specific account name (e.g., HDFC Savings)
    val accountType: String? = null,        // "Savings", "Deposit", "Credit Card", "Cash Wallet", etc.
    val openingBalance: Double = 0.0,       // Opening balance in ₹
    val createdDate: Long,
    val joinDate: Date? = null,             // Applicable mainly for Group Members
    val exitDate: Date? = null              // Applicable mainly for Group Members
)