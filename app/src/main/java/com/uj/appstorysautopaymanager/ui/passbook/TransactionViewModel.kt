package com.uj.appstorysautopaymanager.ui.passbook

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.util.SmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: AutoPayRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _selectedType = MutableStateFlow("All")
    val selectedType: StateFlow<String> = _selectedType

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        _searchQuery,
        _selectedCategory,
        _selectedType
    ) { txns, query, cat, type ->
        txns.filter { txn ->
            val matchesQuery = txn.merchant.contains(query, ignoreCase = true) ||
                    txn.referenceNumber.contains(query, ignoreCase = true) ||
                    txn.bankName.contains(query, ignoreCase = true) ||
                    txn.smsBody.contains(query, ignoreCase = true)
            
            val matchesCategory = cat == "All" || txn.category.equals(cat, ignoreCase = true)
            
            val matchesType = type == "All" || txn.transactionType.equals(type, ignoreCase = true)

            matchesQuery && matchesCategory && matchesType
        }
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

    fun setSelectedType(type: String) {
        _selectedType.value = type
    }

    fun scanSmsInbox(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanProgress.value = 0f
            
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox._ID, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.ADDRESS),
                null, null, Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            )

            cursor?.use { c ->
                val totalCount = c.count
                if (totalCount > 0) {
                    var processed = 0
                    val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                    val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)
                    val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)

                    while (c.moveToNext()) {
                        val body = c.getString(bodyIndex)
                        val date = c.getLong(dateIndex)
                        val address = c.getString(addressIndex) ?: "Unknown"
                        val smsId = "${address}_${date}"

                        if (!repository.exists(smsId)) {
                            val parsed = SmsParser.parseSms(body, date, smsId)
                            if (parsed != null) {
                                repository.insertTransaction(parsed.transaction)
                                parsed.mandate?.let {
                                    repository.insertMandate(it)
                                }
                            }
                        }

                        processed++
                        _scanProgress.value = processed.toFloat() / totalCount.toFloat()
                    }
                }
            }
            _isScanning.value = false
        }
    }

    fun deleteTransaction(txn: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(txn)
        }
    }
}
