package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.RecurringBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: RecurringBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<RecurringBillEntity>): List<Long>

    @Update
    suspend fun update(bill: RecurringBillEntity): Int

    @Delete
    suspend fun delete(bill: RecurringBillEntity): Int

    @Query("SELECT * FROM recurring_bills WHERE isActive = 1 ORDER BY dueDay ASC")
    fun getAllActive(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills ORDER BY id ASC")
    suspend fun getAllDirect(): List<RecurringBillEntity>

    @Query("SELECT * FROM recurring_bills WHERE id = :id")
    suspend fun getById(id: Long): RecurringBillEntity?

    @Query("SELECT * FROM recurring_bills WHERE accountId = :accountId AND isActive = 1")
    fun getByAccountId(accountId: Long): Flow<List<RecurringBillEntity>>

    @Query("DELETE FROM recurring_bills")
    suspend fun deleteAll(): Int
}
