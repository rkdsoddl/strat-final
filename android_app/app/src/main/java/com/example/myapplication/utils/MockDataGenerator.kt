package com.example.myapplication.utils

import android.util.Log
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MockDataGenerator(private val db: AppDatabase) {

    enum class EfficiencyType {
        TIME_BASED,   // 사용 시간 기준
        COUNT_BASED   // 사용 횟수 기준
    }

    data class SubscriptionInfo(
        val name: String,
        val packageName: String,
        val cost: Int,
        val type: EfficiencyType,
        val category: String
    )

    suspend fun generate() = withContext(Dispatchers.IO) {
        // 1. 기존 데이터 초기화
        db.userDao().clearAll()

        val mockList = mutableListOf<UserEntity>()
        val calendar = Calendar.getInstance()

        // =================================================================
        // [1] 핵심 구독 서비스 5개 정의
        // =================================================================
        val subscriptions = listOf(
            SubscriptionInfo("넷플릭스", "com.netflix.mediaclient", 13500, EfficiencyType.TIME_BASED, "OTT"),
            SubscriptionInfo("유튜브 프리미엄", "com.google.android.youtube", 14900, EfficiencyType.TIME_BASED, "OTT"),
            SubscriptionInfo("멜론", "com.iloen.melon", 10900, EfficiencyType.TIME_BASED, "MUSIC"),
            SubscriptionInfo("배민클럽", "com.woowahan.baemin", 3990, EfficiencyType.COUNT_BASED, "FOOD"),
            SubscriptionInfo("쿠팡와우", "com.coupang.mobile", 4990, EfficiencyType.COUNT_BASED, "SHOPPING")
        )

        // 랜덤하게 2~3개는 '효율(잘 씀)', 나머지는 '비효율(낭비)'로 설정
        val shuffledIndices = subscriptions.indices.shuffled()
        val efficientIndices = shuffledIndices.take(3).toSet()

        subscriptions.forEachIndexed { index, sub ->
            val isEfficient = efficientIndices.contains(index)

            // ---------------------------------------------------------
            // (A) 월 구독료 결제 로그 생성 (1번만 발생)
            // ---------------------------------------------------------
            // 날짜: 25일~30일 전쯤 결제
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -Random.nextInt(25, 30))
            val paymentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time).toLong()

            mockList.add(UserEntity(
                date = paymentDate,
                serviceName = sub.name,
                packageName = sub.packageName,
                cost = sub.cost,        // 비용 발생 O
                timeMinutes = 0,
                logType = "SUB_PAYMENT",
                category = sub.category,
                paymentCount = 1
            ))

            // ---------------------------------------------------------
            // (B) 실제 사용 로그 생성 (비용 0원, 사용량 누적)
            // ---------------------------------------------------------
            // 최근 30일간의 사용 패턴 시뮬레이션
            for (day in 30 downTo 1) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -day)
                val usageDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time).toLong()

                // 서비스 타입별 확률 분리
                val useProbability = if (sub.type == EfficiencyType.TIME_BASED) {
                    // [시간 기반] 넷플릭스, 멜론 등
                    // 효율: 0.8 (30일 중 약 24일 사용 -> 자주 씀)
                    // 비효율: 0.1 (30일 중 약 3일 사용 -> 거의 안 씀)
                    if (isEfficient) 0.8 else 0.1
                } else {
                    // [횟수 기반] 쿠팡, 배민
                    // 효율: 0.28 (30일 * 0.28 = 약 8.4회)
                    // 비효율: 0.05 (30일 * 0.05 = 약 1.5회)
                    if (isEfficient) 0.28 else 0.05
                }

                if (Random.nextDouble() < useProbability) {
                    var minutes = 0
                    var count = 0

                    if (sub.type == EfficiencyType.TIME_BASED) {
                        // 시간 기반: 하루 1~3시간(60~180분) 사용
                        minutes = if (isEfficient) Random.nextInt(60, 180) else Random.nextInt(5, 15)
                        count = 1
                    } else {
                        // 횟수 기반: 사용 시간은 짧음
                        minutes = 10
                        count = 1
                    }

                    mockList.add(UserEntity(
                        date = usageDate,
                        serviceName = sub.name,
                        packageName = sub.packageName,
                        cost = 0, // 사용 시에는 비용 0
                        timeMinutes = minutes,
                        logType = "SUB_USAGE",
                        category = sub.category,
                        paymentCount = count
                    ))
                }
            }
        }

        // =================================================================
        // [2] 파이차트를 위한 기타 지출 데이터 (구독 외)
        // =================================================================
        val others = listOf(
            "스타벅스" to "FOOD", "GS25" to "SHOPPING", "카카오택시" to "TRANSPORT", "올리브영" to "SHOPPING"
        )

        repeat(20) {
            val (name, cat) = others.random()
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -Random.nextInt(1, 30))
            val d = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time).toLong()

            mockList.add(UserEntity(
                date = d,
                serviceName = name,
                packageName = "",
                cost = Random.nextInt(5000, 20000),
                timeMinutes = 0,
                logType = "SPENDING",
                category = cat,
                paymentCount = 1
            ))
        }

        // DB에 일괄 저장
        db.userDao().insertLog(mockList)
        Log.d("MockData", "✅ 스마트 데이터 생성 완료: ${mockList.size}건 (구독 5종 + 기타 지출)")
    }
}