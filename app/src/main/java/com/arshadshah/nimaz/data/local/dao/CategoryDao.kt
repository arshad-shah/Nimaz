package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalCategory

@Dao
interface CategoryDao
{

	@Query("SELECT * FROM Category")
	suspend fun getAllCategories() : List<LocalCategory>

	@Query("SELECT COUNT(*) FROM Category")
	suspend fun countCategories() : Int

	@Query("SELECT * FROM Category WHERE id = :id")
	suspend fun getCategory(id : Int) : LocalCategory

	@Query("SELECT * FROM Category WHERE name = :name")
	suspend fun getCategory(name : String) : LocalCategory

	@Insert(entity = LocalCategory::class , onConflict = OnConflictStrategy.REPLACE)
	suspend fun saveAllCategories(categories : List<LocalCategory>)
}