package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>): List<Long>

    @Update
    suspend fun update(account: AccountEntity): Int

    @Delete
    suspend fun delete(account: AccountEntity): Int

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY displayOrder ASC, name ASC")
    fun getAllActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY displayOrder ASC, name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllDirect(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    fun getActiveAccountCount(): Flow<Int>

    @Query("""
        SELECT a.initialBalance + COALESCE((
            SELECT SUM(
                CASE 
                    WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amount
                    WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amount
                    WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amount
                    WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amount
                    WHEN t.type = 'INSTALLMENT' AND t.accountId = a.id THEN -t.amount
                    ELSE 0
                END
            )
            FROM transactions t
            WHERE t.accountId = a.id OR t.toAccountId = a.id
        ), 0)
        FROM accounts a
        WHERE a.id = :accountId
    """)
    fun getAccountBalance(accountId: Long): Flow<Long?>

    @Query("""
        SELECT 
            a.id,
            a.name,
            a.type,
            a.presetId,
            a.initialBalance,
            a.isActive,
            a.displayOrder,
            a.initialBalance + COALESCE((
                SELECT SUM(
                    CASE 
                        WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amount
                        WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amount
                        WHEN t.type = 'INSTALLMENT' AND t.accountId = a.id THEN -t.amount
                        ELSE 0
                    END
                )
                FROM transactions t
                WHERE t.accountId = a.id OR t.toAccountId = a.id
            ), 0) AS currentBalance
        FROM accounts a
        WHERE a.isActive = 1
        ORDER BY a.displayOrder ASC, a.name ASC
    """)
    fun getAllActiveWithBalances(): Flow<List<AccountWithBalance>>

    @Query("""
        SELECT COALESCE(SUM(
            a.initialBalance + COALESCE((
                SELECT SUM(
                    CASE 
                        WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amount
                        WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amount
                        WHEN t.type = 'INSTALLMENT' AND t.accountId = a.id THEN -t.amount
                        ELSE 0
                    END
                )
                FROM transactions t
                WHERE t.accountId = a.id OR t.toAccountId = a.id
            ), 0)
        ), 0)
        FROM accounts a
        WHERE a.isActive = 1
    """)
    fun getTotalNetWorth(): Flow<Long>

    @Transaction
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountWithLoanDetails(accountId: Long): Flow<AccountWithLoanDetails?>

    @Transaction
    @Query("SELECT * FROM accounts WHERE type IN ('LOAN', 'BNPL') AND isActive = 1 ORDER BY displayOrder ASC")
    fun getAllActiveLoanAccounts(): Flow<List<AccountWithLoanDetails>>

    @Query("UPDATE accounts SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE accounts SET isActive = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreAccount(id: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE accounts SET displayOrder = :order WHERE id = :id")
    suspend fun updateDisplayOrder(id: Long, order: Int): Int

    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    suspend fun getActiveCountDirect(): Int

    @Query("DELETE FROM accounts")
    suspend fun deleteAll(): Int
}
