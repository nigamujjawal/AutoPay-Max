package com.autopaymax.data.local.dao

import androidx.room.*
import com.autopaymax.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE smsId = :smsId LIMIT 1")
    suspend fun getTransactionBySmsId(smsId: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsId = :smsId)")
    suspend fun exists(smsId: String): Boolean

    // amount > 0 excludes mandate-revoke bookkeeping rows (recorded with amount 0.0) - those
    // aren't real payments and shouldn't be pushed to the backend.
    @Query("SELECT * FROM transactions WHERE synced = 0 AND amount > 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("UPDATE transactions SET synced = 1, backendPaymentId = :backendPaymentId WHERE id = :id")
    suspend fun markSynced(id: Long, backendPaymentId: String)

    // Cross-source dedup (SMS vs. UPI app notification reporting the same real payment):
    // amount + type + a time window is the best available key, since notification text is too
    // short to carry a reference number to match on. Restricted to the *other* source
    // (isNotifSource flips which side we're looking for, via the "notif_" smsId prefix) so two
    // genuinely separate transactions of the same amount arriving close together through the
    // same channel (e.g. two notification-only payments, no SMS for either) aren't mistaken
    // for one payment reported twice - only a cross-source pair gets merged.
    @Query("SELECT * FROM transactions WHERE amount = :amount AND transactionType = :type AND date BETWEEN :fromDate AND :toDate AND (substr(smsId, 1, 6) = 'notif_') != :isNotifSource ORDER BY ABS(date - :date) ASC LIMIT 1")
    suspend fun findNearbyTransaction(amount: Double, type: String, fromDate: Long, toDate: Long, date: Long, isNotifSource: Boolean): Transaction?

    // Recurring-mandate heuristic (see AutoPayRepository.detectRecurringMandate): the 3 most
    // recent same-merchant, same-amount debits, newest first. Includes whatever row was just
    // inserted before this is called, so 3 results = this occurrence + 2 priors.
    @Query("SELECT * FROM transactions WHERE merchant = :merchant COLLATE NOCASE AND amount = :amount AND transactionType = 'DEBIT' ORDER BY date DESC LIMIT 3")
    suspend fun getRecentSameMerchantAmount(merchant: String, amount: Double): List<Transaction>
}
