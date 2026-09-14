package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(details: SavingsAccountDetailsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detailsList: List<SavingsAccountDetailsEntity>): List<Long>

    @Update
    suspend fun update(details: SavingsAccountDetailsEntity): Int

    @Delete
    suspend fun delete(details: SavingsAccountDetailsEntity): Int

    @Query("SELECT * FROM savings_account_details")
    suspend fun getAllDirect(): List<SavingsAccountDetailsEntity>

    @Query("SELECT * FROM savings_account_details WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): SavingsAccountDetailsEntity?

    @Query("SELECT * FROM savings_account_details WHERE accountId = :accountId")
    fun getByAccountIdFlow(accountId: Long): Flow<SavingsAccountDetailsEntity?>

    @Query("DELETE FROM savings_account_details")
    suspend fun deleteAll(): Int
}
