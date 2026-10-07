package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.budgettracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY category IS NOT NULL, category ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY category IS NOT NULL, category ASC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category IS NULL AND period = :period LIMIT 1")
    fun observeOverall(period: String = "MONTHLY"): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE category IS NULL AND period = :period LIMIT 1")
    suspend fun getOverall(period: String = "MONTHLY"): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE category = :category AND period = :period LIMIT 1")
    fun observeByCategory(category: String, period: String = "MONTHLY"): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE category = :category AND period = :period LIMIT 1")
    suspend fun getByCategory(category: String, period: String = "MONTHLY"): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity): Int

    @Delete
    suspend fun delete(budget: BudgetEntity): Int

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Enforces the business rule that there can only be ONE overall budget per period
     * (since SQLite UNIQUE indexes treat NULLs as distinct).
     * For category-specific budgets, leverages the unique index on (category, period).
     */
    @Transaction
    suspend fun upsert(budget: BudgetEntity): Long {
        return if (budget.category == null) {
            val existingOverall = getOverall(budget.period)
            if (existingOverall != null) {
                val updated = budget.copy(
                    id = existingOverall.id,
                    createdAt = existingOverall.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
                update(updated)
                existingOverall.id
            } else {
                insert(budget)
            }
        } else {
            val existing = getByCategory(budget.category, budget.period)
            if (existing != null) {
                val updated = budget.copy(
                    id = existing.id,
                    createdAt = existing.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
                update(updated)
                existing.id
            } else {
                insert(budget)
            }
        }
    }
}
