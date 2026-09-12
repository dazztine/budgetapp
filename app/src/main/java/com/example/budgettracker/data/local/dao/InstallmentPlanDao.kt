package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: InstallmentPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<InstallmentPlanEntity>): List<Long>

    @Update
    suspend fun update(plan: InstallmentPlanEntity): Int

    @Delete
    suspend fun delete(plan: InstallmentPlanEntity): Int

    @Query("SELECT * FROM installment_plans WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPlanEntity?

    @Query("SELECT * FROM installment_plans WHERE accountId = :accountId ORDER BY purchaseDate DESC")
    fun getByAccountId(accountId: Long): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE accountId = :accountId ORDER BY purchaseDate DESC")
    suspend fun getByAccountIdDirect(accountId: Long): List<InstallmentPlanEntity>

    @Query("SELECT * FROM installment_plans WHERE remainingBalance > 0 ORDER BY purchaseDate DESC")
    fun getAllActive(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans ORDER BY purchaseDate DESC")
    fun getAll(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans ORDER BY id ASC")
    suspend fun getAllDirect(): List<InstallmentPlanEntity>

    @Query("DELETE FROM installment_plans")
    suspend fun deleteAll(): Int
}
