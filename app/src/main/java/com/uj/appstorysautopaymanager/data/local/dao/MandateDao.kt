package com.uj.appstorysautopaymanager.data.local.dao

import androidx.room.*
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import kotlinx.coroutines.flow.Flow

@Dao
interface MandateDao {
    @Query("SELECT * FROM mandates ORDER BY nextExpectedDebit ASC")
    fun getAllMandates(): Flow<List<Mandate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandate(mandate: Mandate): Long

    @Update
    suspend fun updateMandate(mandate: Mandate)

    @Delete
    suspend fun deleteMandate(mandate: Mandate)

    @Query("SELECT * FROM mandates WHERE referenceNumber = :ref LIMIT 1")
    suspend fun getMandateByRef(ref: String): Mandate?
}
