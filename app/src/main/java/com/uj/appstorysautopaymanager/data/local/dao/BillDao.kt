package com.uj.appstorysautopaymanager.data.local.dao

import androidx.room.*
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE isArchived = 0 ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): Bill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("SELECT * FROM bills WHERE status = :status AND isArchived = 0")
    fun getBillsByStatus(status: String): Flow<List<Bill>>
}
