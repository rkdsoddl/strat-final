package com.example.myapplication.utils

import android.util.Log
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MockDataGenerator(private val db: AppDatabase) {

    suspend fun generate() = withContext(Dispatchers.IO) {
        // 기존 데이터 삭제 (중복 방지)
        db.userDao().clearAll()

        val mocks = listOf(
            UserEntity(date = 20231214, serviceName = "넷플릭스", packageName = "com.netflix", cost = 13500, timeMinutes = 10, logType = "PAYMENT"), // 비효율
            UserEntity(date = 20231214, serviceName = "유튜브", packageName = "com.google.youtube", cost = 14900, timeMinutes = 3000, logType = "PAYMENT"), // 효율
            UserEntity(date = 20231215, serviceName = "배달의민족", packageName = "com.baemin", cost = 25000, timeMinutes = 0, logType = "PAYMENT") // 지출
        )

        mocks.forEach { db.userDao().insertLog(it) }
        Log.d("MockCheck", "✅ 가상 데이터 3건 DB 입력 완료")
    }
}