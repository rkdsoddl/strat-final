package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_logs")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val serviceName: String,
    val packageName: String,
    val cost: Int,
    val timeMinutes: Int,
    val logType: String,

    val category: String = "ETC",
    val paymentCount: Int = 1
)