package com.finance.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name")
    fun getAllCompanies(): Flow<List<Company>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(company: Company): Long

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getById(id: Long): Company?

    @Update
    suspend fun update(company: Company)

    @Delete
    suspend fun delete(company: Company)
}
