package com.uj.appstorysautopaymanager.data.local.dao

import androidx.room.*
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import kotlinx.coroutines.flow.Flow

@Dao
interface MandateDao {
    @Query("SELECT * FROM mandates WHERE status = 'ACTIVE' ORDER BY nextExpectedDebit DESC")
    fun getAllMandates(): Flow<List<Mandate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandate(mandate: Mandate): Long

    @Update
    suspend fun updateMandate(mandate: Mandate)

    @Delete
    suspend fun deleteMandate(mandate: Mandate)

    @Query("SELECT * FROM mandates WHERE referenceNumber = :ref LIMIT 1")
    suspend fun getMandateByRef(ref: String): Mandate?

    // Case-insensitive: the same merchant can come out with different casing/punctuation
    // depending on which SMS template named it (bank NPCI vs. Paytm's own confirmation).
    @Query("SELECT * FROM mandates WHERE merchant = :merchant COLLATE NOCASE AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveMandateByMerchant(merchant: String): Mandate?

    @Query("UPDATE mandates SET status = 'CANCELLED' WHERE merchant = :merchant COLLATE NOCASE AND status = 'ACTIVE'")
    suspend fun cancelActiveMandatesByMerchant(merchant: String)
}
