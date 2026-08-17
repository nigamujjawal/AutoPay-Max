package com.uj.appstorysautopaymanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AutoPayRepository
) : ViewModel() {

    val recentTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .map { txns -> txns.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats = combine(
        repository.allTransactions,
        repository.allBills
    ) { txns, bills ->
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + (24 * 60 * 60 * 1000)

        val monthTxns = txns.filter { txn ->
            val txnCal = Calendar.getInstance().apply { timeInMillis = txn.date }
            txnCal.get(Calendar.MONTH) == currentMonth && txnCal.get(Calendar.YEAR) == currentYear
        }

        val monthlySpending = monthTxns.filter { it.transactionType == "DEBIT" }.sumOf { it.amount }

        val todayBills = bills.filter { bill ->
            bill.dueDate in todayStart until todayEnd
        }

        val pendingBills = bills.filter { it.status == "PENDING" && !it.isArchived }
        val paidBills = bills.filter { it.status == "PAID" && !it.isArchived }
        val overdueBills = bills.filter { it.status == "PENDING" && it.dueDate < todayStart && !it.isArchived }

        val totalSubscriptionCost = bills.filter { (it.category.lowercase() == "ott" || it.category.lowercase() == "music") && !it.isArchived }.sumOf { it.amount }
        val emiTotal = bills.filter { it.category.lowercase() == "emi" && !it.isArchived }.sumOf { it.amount }

        val categorySummary = monthTxns.filter { it.transactionType == "DEBIT" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        DashboardStats(
            monthlySpending = monthlySpending,
            todayBillsCount = todayBills.size,
            pendingBillsCount = pendingBills.size,
            paidBillsCount = paidBills.size,
            overdueBillsCount = overdueBills.size,
            totalSubscriptionCost = totalSubscriptionCost,
            emiTotal = emiTotal,
            categorySummary = categorySummary,
            todayBills = todayBills
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}

data class DashboardStats(
    val monthlySpending: Double = 0.0,
    val todayBillsCount: Int = 0,
    val pendingBillsCount: Int = 0,
    val paidBillsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val totalSubscriptionCost: Double = 0.0,
    val emiTotal: Double = 0.0,
    val categorySummary: Map<String, Double> = emptyMap(),
    val todayBills: List<Bill> = emptyList()
)
