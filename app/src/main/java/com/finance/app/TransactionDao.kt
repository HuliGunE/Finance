package com.finance.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE companyId = :companyId ORDER BY dateMillis DESC")
    fun getByCompany(companyId: Long): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE companyId = :companyId AND type = 'income'")
    fun getTotalIncome(companyId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE companyId = :companyId AND type = 'expense'")
    fun getTotalExpense(companyId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Delete
    suspend fun delete(transaction: Transaction)
}
