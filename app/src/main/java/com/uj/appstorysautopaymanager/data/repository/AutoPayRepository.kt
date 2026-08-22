package com.uj.appstorysautopaymanager.data.repository

import android.content.Context
import com.uj.appstorysautopaymanager.data.local.dao.BillDao
import com.uj.appstorysautopaymanager.data.local.dao.CategoryDao
import com.uj.appstorysautopaymanager.data.local.dao.MandateDao
import com.uj.appstorysautopaymanager.data.local.dao.NotificationDao
import com.uj.appstorysautopaymanager.data.local.dao.TransactionDao
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.data.local.entity.Category
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.local.entity.NotificationEntity
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import com.uj.appstorysautopaymanager.worker.PaymentSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoPayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billDao: BillDao,
    private val transactionDao: TransactionDao,
    private val mandateDao: MandateDao,
    private val categoryDao: CategoryDao,
    private val notificationDao: NotificationDao
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
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun getTransactionBySmsId(smsId: String): Transaction? = transactionDao.getTransactionBySmsId(smsId)
    suspend fun exists(smsId: String): Boolean = transactionDao.exists(smsId)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
    suspend fun getUnsyncedTransactions(): List<Transaction> = transactionDao.getUnsyncedTransactions()
    suspend fun markTransactionSynced(id: Long, backendPaymentId: String) = transactionDao.markSynced(id, backendPaymentId)

    // Room is the source of truth - every transaction lands here first, then this kicks off a
    // best-effort push to the SoundBox backend. Only fires on an actual new row (IGNORE conflict
    // strategy returns -1 when smsId already existed, nothing new to sync in that case).
    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction)
        if (id != -1L) PaymentSyncWorker.enqueue(context)
        return id
    }


    suspend fun applyTransactionEvent(transaction: Transaction): Long {
        val windowMillis = 5L * 60L * 1000L
        val isNotifSource = transaction.smsId.startsWith("notif_")
        val existing = transactionDao.findNearbyTransaction(
            transaction.amount,
            transaction.transactionType,
            transaction.date - windowMillis,
            transaction.date + windowMillis,
            transaction.date,
            isNotifSource
        )
        if (existing == null) {
            val id = transactionDao.insertTransaction(transaction)
            if (id != -1L) PaymentSyncWorker.enqueue(context)
            return id
        }
        if (isPlaceholderMerchant(existing.merchant) && !isPlaceholderMerchant(transaction.merchant)) {
            transactionDao.updateTransaction(existing.copy(merchant = transaction.merchant, category = transaction.category))
        }
        return existing.id
    }

    private fun isPlaceholderMerchant(merchant: String): Boolean =
        merchant == "Merchant" || merchant == "UPI Credit" || merchant.contains("@")

    // Mandates
    val allMandates: Flow<List<Mandate>> = mandateDao.getAllMandates()
    suspend fun insertMandate(mandate: Mandate): Long = mandateDao.insertMandate(mandate)
    suspend fun updateMandate(mandate: Mandate) = mandateDao.updateMandate(mandate)
    suspend fun getMandateByRef(ref: String): Mandate? = mandateDao.getMandateByRef(ref)
    suspend fun applyMandateEvent(mandate: Mandate): Boolean {
        if (mandate.status == "CANCELLED") {
            mandateDao.cancelActiveMandatesByMerchant(mandate.merchant)
            return true
        }
        val existing = mandateDao.getActiveMandateByMerchant(mandate.merchant)
        return if (existing != null) {
            mandateDao.updateMandate(
                existing.copy(
                    amount = mandate.amount,
                    frequency = mandate.frequency,
                    nextExpectedDebit = mandate.nextExpectedDebit,
                    bank = mandate.bank,
                    referenceNumber = mandate.referenceNumber
                )
            )
            false
        } else {
            mandateDao.insertMandate(mandate)
            true
        }
    }

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Notifications (in-app log, backs the Notifications screen)
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    suspend fun insertNotification(notification: NotificationEntity): Long = notificationDao.insertNotification(notification)
    suspend fun markAllNotificationsRead() = notificationDao.markAllRead()
    suspend fun deleteNotification(notification: NotificationEntity) = notificationDao.deleteNotification(notification)
    suspend fun deleteNotificationById(id: Long) = notificationDao.deleteById(id)
}
