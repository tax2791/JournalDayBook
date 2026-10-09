package database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date

class MealViewModel(private val mealDao: MealDao) : ViewModel() {

    val allMembers: Flow<List<Member>> = mealDao.getAllMembers()

    // 🚀 Filtered Flows for specific UI screens
    val groupMembers: Flow<List<Member>> = allMembers.map { members ->
        members.filter { it.type == "Group Member" }
    }

    val personalAccounts: Flow<List<Member>> = allMembers.map { members ->
        members.filter { it.type == "Personal Account" }
    }

    // Flow for meals and deposits
    val allMeals: Flow<List<Meal>> = mealDao.getAllMeals()
    val allDeposits: Flow<List<Deposit>> = mealDao.getAllDeposits()

    // 🚀 LOAN ACCOUNT FLOWS
    val allLoans: Flow<List<LoanRecord>> = mealDao.getAllLoans()
    val allLoanRepayments: Flow<List<LoanRepayment>> = mealDao.getAllLoanRepayments()

    // LiveData for Meal1 and related stats
    val allMeals1: LiveData<List<Meal1>> = mealDao.getAllMeals1()
    private val numUniqueNames: LiveData<Int> = mealDao.getNumUniqueNames()
    val uniqueNames: LiveData<String> = mealDao.distinctName()

    val totalAdminCost: LiveData<Double> = mealDao.getTotalAdminCost()
    val totalMealCost: LiveData<Double> = mealDao.getTotalMealCost()
    val totalMeals: LiveData<Int> = mealDao.getTotalMeals()

    // Computed variables from DAO
    val mealCostPerPerson: LiveData<Double> = mealDao.getMealCostPerPerson()
    val adminCostPerPerson: LiveData<Double> = mealDao.getAdminCostPerPerson()
    val totalCostPerPerson: LiveData<Double> = mealDao.getTotalCostPerPerson()

    // MediatorLiveData to observe multiple sources and update values
    val totalMealCostPerPerson = MediatorLiveData<Double>().apply {
        addSource(totalMealCost) { updateMealCostPerPerson() }
        addSource(totalMeals) { updateMealCostPerPerson() }
    }

    val totalAdminCostPerPerson = MediatorLiveData<Double>().apply {
        addSource(totalAdminCost) { updateAdminCostPerPerson() }
        addSource(numUniqueNames) { updateAdminCostPerPerson() }
    }

    val overallTotalCostPerPerson = MediatorLiveData<Double>().apply {
        addSource(totalMealCostPerPerson) { updateTotalCostPerPerson() }
        addSource(totalAdminCostPerPerson) { updateTotalCostPerPerson() }
    }

    // Fetch filtered meals
    private val _filteredMeals = MutableLiveData<List<Meal1>>()
    val filteredMeals: LiveData<List<Meal1>> get() = _filteredMeals

    fun fetchFilteredMeals(name: String, item: String) {
        viewModelScope.launch {
            val filteredMealsList = fetchMealsFromDatabase(name, item)
            _filteredMeals.postValue(filteredMealsList)
        }
    }

    private suspend fun fetchMealsFromDatabase(name: String, item: String): List<Meal1> {
        // Replace with actual database query logic if needed
        return listOf()
    }

    // Core insertion functions
    fun addMeal(meal: Meal) {
        viewModelScope.launch { mealDao.insertMeal(meal) }
    }

    fun addDeposit(deposit: Deposit) {
        viewModelScope.launch { mealDao.insertDeposit(deposit) }
    }

    fun addMeal1(meal1: Meal1) {
        viewModelScope.launch { mealDao.insertMeal1(meal1) }
    }

    fun updateMeal1(meal1: Meal1) {
        viewModelScope.launch { mealDao.updateMeal1(meal1) }
    }

    fun deleteMeal1(meal1: Meal1) {
        viewModelScope.launch { mealDao.deleteMeal1(meal1) }
    }

    fun insertMember(member: Member) {
        viewModelScope.launch { mealDao.insertMember(member) }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch { mealDao.updateMember(member) }
    }

    fun updateMember(oldName: String, updatedMember: Member) {
        viewModelScope.launch {
            mealDao.updateMember(updatedMember)
            val newName = updatedMember.name
            if (oldName.isNotBlank() && newName.isNotBlank() && oldName != newName) {
                mealDao.updateMemberNameInMeal1(oldName, newName)
                mealDao.updateMemberNameInMeal(oldName, newName)
                mealDao.updateMemberNameInDeposit(oldName, newName)
                mealDao.updatePartyNameInLoans(oldName, newName, "")
                mealDao.updatePartyNameInRepayments(oldName, newName)
            }
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            mealDao.deleteMeals1ByMemberName(member.name)
            mealDao.deleteMealsByMemberName(member.name)
            mealDao.deleteDepositsByMemberName(member.name)
            member.accountName?.let { accName ->
                if (accName.isNotBlank() && accName != member.name) {
                    mealDao.deleteMeals1ByMemberName(accName)
                    mealDao.deleteMealsByMemberName(accName)
                    mealDao.deleteDepositsByMemberName(accName)
                }
            }
            mealDao.deleteMember(member)
        }
    }

    // 🚀 Updated Helper to quickly add a Group Member from MemberForm
    fun addGroupMember(
        name: String,
        openingBalance: Double,
        joinDate: Date,
        exitDate: Date?,
        groupName: String? = null
    ) {
        val member = Member(
            name = name,
            type = "Group Member",
            openingBalance = openingBalance,
            createdDate = System.currentTimeMillis(),
            joinDate = joinDate,
            exitDate = exitDate,
            accountName = groupName?.ifBlank { null }
        )
        insertMember(member)
    }

    // 🚀 Helper to quickly add a Personal Account from MemberForm
    fun addPersonalAccount(accountName: String, accountType: String, openingBalance: Double, joinDate: Date?, exitDate: Date?) {
        val member = Member(
            name = accountName,
            type = "Personal Account",
            accountName = accountName,
            accountType = accountType,
            openingBalance = openingBalance,
            joinDate = joinDate,
            exitDate = exitDate,
            createdDate = System.currentTimeMillis()
        )
        insertMember(member)
    }

    // 🚀 LOAN ACCOUNT CRUD OPERATIONS
    fun addLoan(loan: LoanRecord) {
        viewModelScope.launch { mealDao.insertLoan(loan) }
    }

    fun updateLoan(loan: LoanRecord) {
        viewModelScope.launch { mealDao.updateLoan(loan) }
    }

    fun deleteLoan(loan: LoanRecord) {
        viewModelScope.launch { mealDao.deleteLoan(loan) }
    }

    fun addLoanRepayment(repayment: LoanRepayment) {
        viewModelScope.launch { mealDao.insertLoanRepayment(repayment) }
    }

    fun updateLoanRepayment(repayment: LoanRepayment) {
        viewModelScope.launch { mealDao.updateLoanRepayment(repayment) }
    }

    fun deleteLoanRepayment(repayment: LoanRepayment) {
        viewModelScope.launch { mealDao.deleteLoanRepayment(repayment) }
    }

    fun deletePartyLedger(partyName: String) {
        viewModelScope.launch {
            mealDao.deleteLoansByPartyName(partyName)
            mealDao.deleteRepaymentsByPartyName(partyName)
        }
    }

    fun updatePartyLedger(oldName: String, newName: String, newPhone: String) {
        viewModelScope.launch {
            mealDao.updatePartyNameInLoans(oldName, newName, newPhone)
            mealDao.updatePartyNameInRepayments(oldName, newName)
        }
    }

    // Update methods for LiveData
    private fun updateMealCostPerPerson() {
        val mealCost = totalMealCost.value ?: 0.0
        val mealCount = totalMeals.value ?: 1 // Avoid division by zero
        totalMealCostPerPerson.value = if (mealCount > 0) mealCost / mealCount else 0.0
    }

    private fun updateAdminCostPerPerson() {
        val adminCost = totalAdminCost.value ?: 0.0
        val uniqueNamesCount = numUniqueNames.value ?: 1 // Avoid division by zero
        totalAdminCostPerPerson.value = if (uniqueNamesCount > 0) adminCost / uniqueNamesCount else 0.0
    }

    private fun updateTotalCostPerPerson() {
        val mealCostPerPersonValue = totalMealCostPerPerson.value ?: 0.0
        val adminCostPerPersonValue = totalAdminCostPerPerson.value ?: 0.0
        overallTotalCostPerPerson.value = mealCostPerPersonValue + adminCostPerPersonValue
    }
}