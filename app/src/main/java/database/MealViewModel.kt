package com.kushal.mealapp.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MealViewModel(private val mealDao: MealDao) : ViewModel() {

    val allMembers: Flow<List<Member>> = mealDao.getAllMembers()


    // Flow for meals and deposits
    val allMeals: Flow<List<Meal>> = mealDao.getAllMeals()
    val allDeposits: Flow<List<Deposit>> = mealDao.getAllDeposits()

    // LiveData for Meal1 and related stats
    val allMeals1: LiveData<List<Meal1>> = mealDao.getAllMeals1()
    private val numUniqueNames: LiveData<Int> = mealDao.getNumUniqueNames()
    val uniqueNames: LiveData<String> = mealDao.distinctName()


    val totalAdminCost: LiveData<Double> = mealDao.getTotalAdminCost()
    val totalMealCost: LiveData<Double> = mealDao.getTotalMealCost()
    val totalMeals: LiveData<Int> = mealDao.getTotalMeals()

    // New computed variables from DAO
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
        // Replace with actual database query logic
        return listOf()
    }

    // Functions to insert data
    fun addMeal(meal: Meal) {
        viewModelScope.launch { mealDao.insertMeal(meal) }
    }

    fun addDeposit(deposit: Deposit) {
        viewModelScope.launch { mealDao.insertDeposit(deposit) }
    }

    fun addMeal1(meal1: Meal1) {
        viewModelScope.launch { mealDao.insertMeal1(meal1) }
    }

    fun insertMember(member: Member) {
        viewModelScope.launch { mealDao.insertMember(member) }
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
