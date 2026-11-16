package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE shamsiYear = :year ORDER BY shamsiMonth DESC, shamsiDay DESC, timestamp DESC")
    fun getExpensesByYearFlow(year: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE shamsiYear = :year AND shamsiMonth = :month ORDER BY shamsiDay DESC, timestamp DESC")
    fun getExpensesByYearAndMonthFlow(year: Int, month: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isAnnual = 1 AND shamsiYear = :year")
    fun getAnnualExpensesByYearFlow(year: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isAnnual = 1 ORDER BY timestamp DESC")
    fun getAllAnnualExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE shamsiDateString >= :startDate AND shamsiDateString <= :endDate ORDER BY shamsiDateString DESC, timestamp DESC")
    fun getExpensesByDateRangeFlow(startDate: String, endDate: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
