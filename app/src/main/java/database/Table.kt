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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: String,
    val openingBalance: Double,
    val createdDate: Long,
    val joinDate: Date?,
    val exitDate: Date?,
    val accountName: String? = null,
    val accountType: String? = null
)

@Entity(tableName = "loan_record")
data class LoanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partyName: String,
    val loanAmount: Double,
    val interestRate: Double = 0.0,
    val loanDate: String,
    val remarks: String = "",
    val phone: String = ""
)

@Entity(tableName = "loan_repayment")
data class LoanRepayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val loanId: Long = 0L,
    val partyName: String,
    val amountReceived: Double,
    val paymentDate: String,
    val paymentMode: String = "Cash",
    val remarks: String = ""
)