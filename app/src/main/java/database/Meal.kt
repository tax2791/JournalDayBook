package com.kushal.mealapp.database

//noinspection SuspiciousImport
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
    val createdDate: Long,
    val joinDate: Date,
    val exitDate: Date?
)

