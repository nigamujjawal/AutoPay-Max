package com.uj.appstorysautopaymanager.data.repository

import com.uj.appstorysautopaymanager.data.local.dao.BillDao
import com.uj.appstorysautopaymanager.data.local.dao.CategoryDao
import com.uj.appstorysautopaymanager.data.local.dao.MandateDao
import com.uj.appstorysautopaymanager.data.local.dao.TransactionDao
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.data.local.entity.Category
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoPayRepository @Inject constructor(
    private val billDao: BillDao,
    private val transactionDao: TransactionDao,
    private val mandateDao: MandateDao,
    private val categoryDao: CategoryDao
) {
    // Bills
    val allBills: Flow<List<Bill>> = billDao.getAllBills()
    
    suspend fun getBillById(id: Long): Bill? = billDao.getBillById(id)
    suspend fun insertBill(bill: Bill): Long = billDao.insertBill(bill)
    suspend fun updateBill(bill: Bill) = billDao.updateBill(bill)
    suspend fun deleteBill(bill: Bill) = billDao.deleteBill(bill)
    fun getBillsByStatus(status: String): Flow<List<Bill>> = billDao.getBillsByStatus(status)

    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun getTransactionBySmsId(smsId: String): Transaction? = transactionDao.getTransactionBySmsId(smsId)
    suspend fun exists(smsId: String): Boolean = transactionDao.exists(smsId)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    // Mandates
    val allMandates: Flow<List<Mandate>> = mandateDao.getAllMandates()
    suspend fun insertMandate(mandate: Mandate): Long = mandateDao.insertMandate(mandate)
    suspend fun updateMandate(mandate: Mandate) = mandateDao.updateMandate(mandate)
    suspend fun deleteMandate(mandate: Mandate) = mandateDao.deleteMandate(mandate)
    suspend fun getMandateByRef(ref: String): Mandate? = mandateDao.getMandateByRef(ref)

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
