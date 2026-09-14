package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Delete
    suspend fun delete(transaction: TransactionEntity): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllDirect(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY timestamp DESC, id DESC")
    fun getByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun getCountByAccount(accountId: Long): Int

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC, id DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT DISTINCT category 
        FROM transactions 
        WHERE type = :type AND category != '' 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getDistinctCategories(type: TransactionType, limit: Int = 20): Flow<List<String>>

    @Query("""
        SELECT DISTINCT title 
        FROM transactions 
        WHERE type = :type AND title != '' 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getDistinctTitles(type: TransactionType, limit: Int = 20): Flow<List<String>>

    @Query("""
        SELECT DISTINCT title 
        FROM transactions 
        WHERE type = :type AND category = :category AND title != '' 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getDistinctTitlesByCategory(type: TransactionType, category: String, limit: Int = 20): Flow<List<String>>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS totalIncome,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS totalExpense
        FROM transactions
        WHERE timestamp >= :startTime AND timestamp <= :endTime
          AND type IN ('INCOME', 'EXPENSE')
    """)
    fun getMonthlyTotals(startTime: Long, endTime: Long): Flow<MonthlyTotals>

    @Query("""
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (:startTime IS NULL OR timestamp >= :startTime)
          AND (:endTime IS NULL OR timestamp <= :endTime)
        ORDER BY timestamp DESC, id DESC
    """)
    fun getFilteredTransactions(
        accountId: Long? = null,
        type: TransactionType? = null,
        category: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE installmentPlanId = :planId ORDER BY timestamp DESC, id DESC")
    fun getByInstallmentPlan(planId: Long): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int
}

data class MonthlyTotals(
    val totalIncome: Long,
    val totalExpense: Long
) {
    val netSavings: Long get() = totalIncome - totalExpense
    val savingsRate: Float get() = if (totalIncome > 0) (netSavings.toFloat() / totalIncome) * 100f else 0f
}
