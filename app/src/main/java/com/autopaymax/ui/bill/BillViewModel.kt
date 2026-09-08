package com.autopaymax.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopaymax.data.local.entity.Bill
import com.autopaymax.data.repository.AutoPayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillViewModel @Inject constructor(
    private val repository: AutoPayRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _sortBy = MutableStateFlow("Due Date")
    val sortBy: StateFlow<String> = _sortBy

    val filteredBills: StateFlow<List<Bill>> = combine(
        repository.allBills,
        _searchQuery,
        _selectedCategory,
        _sortBy
    ) { bills, query, cat, sort ->
        var list = bills.filter { bill ->
            val matchesQuery = bill.title.contains(query, ignoreCase = true) ||
                    bill.notes.contains(query, ignoreCase = true)
            val matchesCategory = cat == "All" || bill.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCategory
        }

        list = when (sort) {
            "Amount" -> list.sortedBy { it.amount }
            "Title" -> list.sortedBy { it.title }
            else -> list.sortedBy { it.dueDate }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.allCategories.map { cats ->
        listOf("All") + cats.map { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun addBill(bill: Bill) {
        viewModelScope.launch {
            repository.insertBill(bill)
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun markPaid(bill: Bill) {
        viewModelScope.launch {
            val newStatus = if (bill.status == "PAID") "PENDING" else "PAID"
            repository.updateBill(bill.copy(status = newStatus))
        }
    }

    fun archiveBill(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill.copy(isArchived = true))
        }
    }

    fun duplicateBill(bill: Bill) {
        viewModelScope.launch {
            repository.insertBill(bill.copy(id = 0, title = "${bill.title} (Copy)"))
        }
    }
}
