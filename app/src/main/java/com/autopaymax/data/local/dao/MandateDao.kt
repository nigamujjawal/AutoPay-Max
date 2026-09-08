package com.autopaymax.data.local.dao

import androidx.room.*
import com.autopaymax.data.local.entity.Mandate
import kotlinx.coroutines.flow.Flow

@Dao
interface MandateDao {
    @Query("SELECT * FROM mandates WHERE status = 'ACTIVE' ORDER BY nextExpectedDebit DESC")
    fun getAllMandates(): Flow<List<Mandate>>

    // For the Home list: keep cancelled mandates visible (sorted below the active ones) so the
    // user can still open them. The reminder worker keeps using getAllMandates() (active only).
    @Query("SELECT * FROM mandates ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END, nextExpectedDebit DESC")
    fun getAllMandatesForDisplay(): Flow<List<Mandate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandate(mandate: Mandate): Long

    @Update
    suspend fun updateMandate(mandate: Mandate)

    @Query("SELECT * FROM mandates WHERE referenceNumber = :ref LIMIT 1")
    suspend fun getMandateByRef(ref: String): Mandate?

    // Case-insensitive: the same merchant can come out with different casing/punctuation
    // depending on which SMS template named it (bank NPCI vs. Paytm's own confirmation).
    @Query("SELECT * FROM mandates WHERE merchant = :merchant COLLATE NOCASE AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveMandateByMerchant(merchant: String): Mandate?

    @Query("UPDATE mandates SET status = 'CANCELLED' WHERE merchant = :merchant COLLATE NOCASE AND status = 'ACTIVE'")
    suspend fun cancelActiveMandatesByMerchant(merchant: String)
}
