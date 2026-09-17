package com.example.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CustomCategoryEntity): Long

    @Query("SELECT * FROM custom_categories WHERE transactionType = :type ORDER BY id ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories WHERE transactionType = :type ORDER BY id ASC")
    suspend fun getCategoriesByTypeDirect(type: TransactionType): List<CustomCategoryEntity>

    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    fun getAll(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    suspend fun getAllDirect(): List<CustomCategoryEntity>
}
