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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

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

    // 🚀 NEW: Fetch only Group Members for meal cost calculations
    @Query("SELECT * FROM member WHERE type = 'Group Member'")
    fun getGroupMembers(): Flow<List<Member>>

    // 🚀 NEW: Fetch only Personal Accounts (Savings, Credit Card, Cash Wallet, etc.)
    @Query("SELECT * FROM member WHERE type = 'Personal Account'")
    fun getPersonalAccounts(): Flow<List<Member>>

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
}