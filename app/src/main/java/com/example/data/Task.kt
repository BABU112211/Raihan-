package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val priority: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
