package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(details: LoanAccountDetailsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detailsList: List<LoanAccountDetailsEntity>): List<Long>

    @Update
    suspend fun update(details: LoanAccountDetailsEntity): Int

    @Delete
    suspend fun delete(details: LoanAccountDetailsEntity): Int

    @Query("SELECT * FROM loan_details")
    suspend fun getAllDirect(): List<LoanAccountDetailsEntity>

    @Query("SELECT * FROM loan_details WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): LoanAccountDetailsEntity?

    @Query("SELECT * FROM loan_details WHERE accountId = :accountId")
    fun getByAccountIdFlow(accountId: Long): Flow<LoanAccountDetailsEntity?>

    @Query("SELECT * FROM loan_details WHERE reminderEnabled = 1")
    fun getAllWithReminders(): Flow<List<LoanAccountDetailsEntity>>

    @Query("DELETE FROM loan_details")
    suspend fun deleteAll(): Int
}
