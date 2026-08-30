package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_meals")
data class SavedMealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val servings: Int,
    val ingredientsHave: String,
    val ingredientsNeeded: String,
    val instructions: String,
    val sources: String,
    val timestamp: Long = System.currentTimeMillis()
)
