package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMealDao {
    @Query("SELECT * FROM saved_meals ORDER BY timestamp DESC")
    fun getAllSavedMeals(): Flow<List<SavedMealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: SavedMealEntity)

    @Delete
    suspend fun deleteMeal(meal: SavedMealEntity)
}
