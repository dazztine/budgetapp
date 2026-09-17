package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(details: CreditAccountDetailsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detailsList: List<CreditAccountDetailsEntity>): List<Long>

    @Update
    suspend fun update(details: CreditAccountDetailsEntity): Int

    @Delete
    suspend fun delete(details: CreditAccountDetailsEntity): Int

    @Query("SELECT * FROM credit_account_details")
    fun getAll(): Flow<List<CreditAccountDetailsEntity>>

    @Query("SELECT * FROM credit_account_details")
    suspend fun getAllDirect(): List<CreditAccountDetailsEntity>

    @Query("SELECT * FROM credit_account_details WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): CreditAccountDetailsEntity?

    @Query("SELECT * FROM credit_account_details WHERE accountId = :accountId")
    fun getByAccountIdFlow(accountId: Long): Flow<CreditAccountDetailsEntity?>

    @Query("DELETE FROM credit_account_details")
    suspend fun deleteAll(): Int
}
