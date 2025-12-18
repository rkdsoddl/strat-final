package com.example.myapplication.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageManager(private val context: Context) {

    suspend fun collectUsageStats() = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 오늘 날짜 (24시간 기준)
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // 어제부터 오늘까지
        val startTime = calendar.timeInMillis

        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()).toLong()

        // 권한이 없으면 빈 리스트를 반환
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (usageStats.isNullOrEmpty()) {
            Log.e("UsageCheck", "권한이 없거나 데이터가 없습니다.")
            return@withContext
        }

        // 감시할 타겟 앱 목록
        val targetApps = mapOf(
            "com.google.android.youtube" to "유튜브",
            "com.netflix.mediaclient" to "넷플릭스",
            "com.android.chrome" to "크롬" // 테스트용
        )

        for ((pkg, name) in targetApps) {
            // 여러 번 실행된 기록을 합쳐서 계산
            val stats = usageStats.filter { it.packageName == pkg }
            var totalTime = 0L

            stats.forEach {
                totalTime += it.totalTimeInForeground
            }

            val minutes = (totalTime / 1000 / 60).toInt()

            Log.d("UsageCheck", "🔍 $name ($pkg): ${minutes}분 사용")

            if (minutes > 0) {
                val log = UserEntity(
                    date = todayDate,
                    serviceName = name,
                    packageName = pkg,
                    cost = 0,
                    timeMinutes = minutes,
                    logType = "USAGE",
                    category = "USAGE",
                    paymentCount = 0
                )

                // 리스트로 감싸서(listOf) 전달
                db.userDao().insertLog(listOf(log))
            }
        }
    }
}