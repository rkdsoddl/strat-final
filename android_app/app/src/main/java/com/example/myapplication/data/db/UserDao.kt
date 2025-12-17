package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    // 1. 로그 저장 (데이터 넣기)
    @Insert
    suspend fun insertLog(log: UserEntity)

    // 2. 모든 로그 가져오기 (서버로 보낼 때 씀)
    @Query("SELECT * FROM user_logs")
    suspend fun getAllLogs(): List<UserEntity>

    // 3. 지우기 (초기화용)
    @Query("DELETE FROM user_logs")
    suspend fun clearAll()
}