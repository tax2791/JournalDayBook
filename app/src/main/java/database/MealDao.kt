package database

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: Deposit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal1(meal1: Meal1)

    @Update
    suspend fun updateMeal1(meal1: Meal1)

    @Delete
    suspend fun deleteMeal1(meal1: Meal1)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    // 🚀 NEW: Update and Delete operations for Members/Accounts
    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("DELETE FROM member WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Long)

    @Query("DELETE FROM meal1 WHERE name = :memberName")
    suspend fun deleteMeals1ByMemberName(memberName: String)

    @Query("DELETE FROM meal WHERE name = :memberName")
    suspend fun deleteMealsByMemberName(memberName: String)

    @Query("DELETE FROM deposit WHERE name = :memberName")
    suspend fun deleteDepositsByMemberName(memberName: String)

    @Query("UPDATE meal1 SET name = :newName WHERE name = :oldName")
    suspend fun updateMemberNameInMeal1(oldName: String, newName: String)

    @Query("UPDATE meal SET name = :newName WHERE name = :oldName")
    suspend fun updateMemberNameInMeal(oldName: String, newName: String)

    @Query("UPDATE deposit SET name = :newName WHERE name = :oldName")
    suspend fun updateMemberNameInDeposit(oldName: String, newName: String)

    @Query("SELECT * FROM meal ORDER BY date DESC")
    fun getAllMeals(): Flow<List<Meal>>

    @Query("SELECT * FROM deposit ORDER BY date DESC")
    fun getAllDeposits(): Flow<List<Deposit>>

    @Query("SELECT * FROM meal1 WHERE name != ''")
    fun getAllMeals1(): LiveData<List<Meal1>>

    @Query("SELECT COUNT(DISTINCT name) FROM meal1")
    fun getNumUniqueNames(): LiveData<Int>

    @Query("SELECT DISTINCT name FROM meal1")
    fun distinctName(): LiveData<String>

    @Query("SELECT SUM(CASE WHEN item = 'Admin' THEN price ELSE 0 END) FROM meal1")
    fun getTotalAdminCost(): LiveData<Double>

    @Query("SELECT SUM(CASE WHEN item NOT IN ('Deposit', 'Admin') THEN price ELSE 0 END) FROM meal1")
    fun getTotalMealCost(): LiveData<Double>

    @Query("SELECT SUM(CASE WHEN item = 'Meal' THEN meal ELSE 0 END) FROM meal1")
    fun getTotalMeals(): LiveData<Int>

    // ─── MEMBER / ACCOUNT QUERIES ───────────────────────────────────────────

    @Query("SELECT * FROM member")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE type = 'Group Member'")
    fun getGroupMembers(): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE type = 'Personal Account'")
    fun getPersonalAccounts(): Flow<List<Member>>

    // 🚀 NEW: Fetch single member by ID
    @Query("SELECT * FROM member WHERE id = :memberId")
    suspend fun getMemberById(memberId: Long): Member?

    // ────────────────────────────────────────────────────────────────────────

    // ✅ Meal Cost Per Person = Total Meal Cost ÷ Unique Members
    @Query("SELECT CASE WHEN COUNT(DISTINCT name) > 0 THEN " +
            "(SUM(CASE WHEN item NOT IN ('Deposit', 'Admin') THEN price ELSE 0 END) * 1.0 / COUNT(DISTINCT name)) " +
            "ELSE 0 END FROM meal1")
    fun getMealCostPerPerson(): LiveData<Double>

    // ✅ Admin Cost Per Person = Total Admin Cost ÷ Unique Members
    @Query("SELECT CASE WHEN COUNT(DISTINCT name) > 0 THEN " +
            "(SUM(CASE WHEN item = 'Admin' THEN price ELSE 0 END) * 1.0 / COUNT(DISTINCT name)) " +
            "ELSE 0 END FROM meal1")
    fun getAdminCostPerPerson(): LiveData<Double>

    // ✅ Total Cost Per Person = Meal Cost Per Person + Admin Cost Per Person
    @Query("SELECT " +
            "(CASE WHEN COUNT(DISTINCT name) > 0 THEN " +
            "(SUM(CASE WHEN item NOT IN ('Deposit', 'Admin') THEN price ELSE 0 END) * 1.0 / COUNT(DISTINCT name)) " +
            "ELSE 0 END) + " +
            "(CASE WHEN COUNT(DISTINCT name) > 0 THEN " +
            "(SUM(CASE WHEN item = 'Admin' THEN price ELSE 0 END) * 1.0 / COUNT(DISTINCT name)) " +
            "ELSE 0 END) " +
            "FROM meal1")
    fun getTotalCostPerPerson(): LiveData<Double>

    // ─── LOAN ACCOUNT QUERIES ───────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanRecord): Long

    @Update
    suspend fun updateLoan(loan: LoanRecord)

    @Delete
    suspend fun deleteLoan(loan: LoanRecord)

    @Query("DELETE FROM loan_record WHERE id = :loanId")
    suspend fun deleteLoanById(loanId: Long)

    @Query("SELECT * FROM loan_record ORDER BY id DESC")
    fun getAllLoans(): Flow<List<LoanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanRepayment(repayment: LoanRepayment): Long

    @Update
    suspend fun updateLoanRepayment(repayment: LoanRepayment)

    @Delete
    suspend fun deleteLoanRepayment(repayment: LoanRepayment)

    @Query("SELECT * FROM loan_repayment ORDER BY id DESC")
    fun getAllLoanRepayments(): Flow<List<LoanRepayment>>

    @Query("DELETE FROM loan_record WHERE partyName = :partyName")
    suspend fun deleteLoansByPartyName(partyName: String)

    @Query("DELETE FROM loan_repayment WHERE partyName = :partyName")
    suspend fun deleteRepaymentsByPartyName(partyName: String)

    @Query("UPDATE loan_record SET partyName = :newName, phone = CASE WHEN :newPhone != '' THEN :newPhone ELSE phone END WHERE partyName = :oldName")
    suspend fun updatePartyNameInLoans(oldName: String, newName: String, newPhone: String)

    @Query("UPDATE loan_repayment SET partyName = :newName WHERE partyName = :oldName")
    suspend fun updatePartyNameInRepayments(oldName: String, newName: String)
}