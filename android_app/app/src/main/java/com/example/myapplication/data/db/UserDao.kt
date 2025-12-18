package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy // 충돌 방지 전략 추가

@Dao
interface UserDao {
    // 1. 리스트 받기
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(logs: List<UserEntity>)

    // 2. 모든 로그 가져오기
    @Query("SELECT * FROM user_logs")
    suspend fun getAllLogs(): List<UserEntity>

    // 3. 초기화
    @Query("DELETE FROM user_logs")
    suspend fun clearAll()
}