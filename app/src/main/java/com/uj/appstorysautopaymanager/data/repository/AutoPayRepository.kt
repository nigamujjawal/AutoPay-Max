package com.uj.appstorysautopaymanager.data.repository

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
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoPayRepository @Inject constructor(
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
    suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun getTransactionBySmsId(smsId: String): Transaction? = transactionDao.getTransactionBySmsId(smsId)
    suspend fun exists(smsId: String): Boolean = transactionDao.exists(smsId)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    // Cross-source choke point for non-autopay transactions: SmsReceiver, TransactionViewModel's
    // inbox backfill, and UpiNotificationListenerService all route regular (non-mandate) inserts
    // through here so the same real payment reported by both a bank SMS and a UPI app
    // notification (in either order) doesn't show twice in Passbook.
    //
    // Matched by amount + type within a 5-minute window, restricted to the *other* source (SMS
    // vs. notification) - notification text is too short to carry a reference number to match
    // on precisely, and only a cross-source pair is actually "the same payment reported twice";
    // two same-source hits close together are far more likely to be genuinely different
    // transactions (see findNearbyTransaction).
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
            return transactionDao.insertTransaction(transaction)
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
    suspend fun deleteMandate(mandate: Mandate) = mandateDao.deleteMandate(mandate)
    suspend fun getMandateByRef(ref: String): Mandate? = mandateDao.getMandateByRef(ref)

    // Single choke point for both SmsReceiver (live) and TransactionViewModel (inbox backfill):
    // a revoke event cancels the existing active mandate for that merchant rather than
    // inserting its own separate CANCELLED row (which would leave the original stuck active);
    // a create event updates an already-active mandate for that merchant instead of adding a
    // duplicate row, e.g. when both the bank's and Paytm's own confirmation SMS arrive for it.
    //
    // Returns false when this event just reconfirmed an already-known mandate state (a second
    // "created" SMS for a merchant that's already active - e.g. the bank's and Paytm's own
    // confirmation both landing for the same real setup). Callers use that to skip adding a
    // second Passbook entry for what is, to the user, the same event reported twice. A revoke
    // always returns true and gets its own Passbook entry even if no matching active mandate
    // was found - that could just mean its "created" SMS was never in the inbox, not that this
    // revoke is redundant, so it's kept rather than risking silently dropping real information.
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
