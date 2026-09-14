package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(details: BillAccountDetailsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detailsList: List<BillAccountDetailsEntity>): List<Long>

    @Update
    suspend fun update(details: BillAccountDetailsEntity): Int

    @Delete
    suspend fun delete(details: BillAccountDetailsEntity): Int

    @Query("SELECT * FROM bill_account_details")
    suspend fun getAllDirect(): List<BillAccountDetailsEntity>

    @Query("SELECT * FROM bill_account_details WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): BillAccountDetailsEntity?

    @Query("SELECT * FROM bill_account_details WHERE accountId = :accountId")
    fun getByAccountIdFlow(accountId: Long): Flow<BillAccountDetailsEntity?>

    @Query("DELETE FROM bill_account_details")
    suspend fun deleteAll(): Int
}
