package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.LoanBillingCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanBillingCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: LoanBillingCycleEntity): Long

    @Update
    suspend fun update(cycle: LoanBillingCycleEntity): Int

    @Query("SELECT * FROM loan_billing_cycles WHERE accountId = :accountId AND isPaid = 0 ORDER BY cycleDueDate ASC")
    fun getPendingCycles(accountId: Long): Flow<List<LoanBillingCycleEntity>>

    @Query("SELECT * FROM loan_billing_cycles WHERE isPaid = 0 ORDER BY cycleDueDate ASC")
    fun getAllPendingCycles(): Flow<List<LoanBillingCycleEntity>>

    @Query("SELECT * FROM loan_billing_cycles WHERE accountId = :accountId AND isPaid = 0 ORDER BY cycleDueDate ASC")
    suspend fun getPendingCyclesDirect(accountId: Long): List<LoanBillingCycleEntity>

    @Query("SELECT * FROM loan_billing_cycles WHERE id = :id")
    suspend fun getById(id: Long): LoanBillingCycleEntity?

    @Query("UPDATE loan_billing_cycles SET isPaid = 1, paidAmount = :paidAmount, paidDate = :paidDate WHERE id = :cycleId")
    suspend fun markCyclePaid(cycleId: Long, paidAmount: Long, paidDate: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM loan_billing_cycles WHERE accountId = :accountId AND isPaid = 1 ORDER BY cycleDueDate DESC")
    fun getPaidCycles(accountId: Long): Flow<List<LoanBillingCycleEntity>>

    @Query("SELECT * FROM loan_billing_cycles WHERE accountId = :accountId ORDER BY cycleDueDate DESC")
    fun getAllCycles(accountId: Long): Flow<List<LoanBillingCycleEntity>>

    @Query("SELECT * FROM loan_billing_cycles WHERE accountId = :accountId AND isPaid = 1 ORDER BY cycleDueDate DESC")
    suspend fun getPaidCyclesDirect(accountId: Long): List<LoanBillingCycleEntity>

    @androidx.room.Delete
    suspend fun delete(cycle: LoanBillingCycleEntity): Int
}
