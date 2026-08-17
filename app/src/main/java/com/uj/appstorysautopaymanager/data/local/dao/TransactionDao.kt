package com.uj.appstorysautopaymanager.data.local.dao

import androidx.room.*
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
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

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsId = :smsId)")
    suspend fun exists(smsId: String): Boolean
}
