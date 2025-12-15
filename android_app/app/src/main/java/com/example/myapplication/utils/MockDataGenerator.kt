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

    suspend fun generate() = withContext(Dispatchers.IO) {
        // 1. 기존 데이터 삭제
        db.userDao().clearAll()

        val mockList = mutableListOf<UserEntity>()
        val calendar = Calendar.getInstance()

        // 2. 서비스 목록 정의
        val services = listOf(
            Triple("넷플릭스", "com.netflix.mediaclient", 13500),
            Triple("유튜브", "com.google.android.youtube", 14900),
            Triple("배달의민족", "com.woowahan.baemin", 0), // 기본값 0, 아래에서 랜덤 생성
            Triple("쿠팡", "com.coupang.mobile", 4990),
            Triple("멜론", "com.iloen.melon", 10900)
        )

        // 3. 50개 생성 반복문 시작
        repeat(50) {
            // (1) 기본 정보 랜덤 선택
            val target = services.random()
            val name = target.first
            val pkg = target.second
            var baseCost = target.third

            // 날짜 랜덤 생성
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -Random.nextInt(0, 30))
            val randomDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time).toLong()

            // 1. 카테고리 결정 로직
            val category = if (name == "넷플릭스" || name == "유튜브") "OTT"
            else if (name == "멜론") "MUSIC"
            else if (name == "배달의민족") "FOOD"
            else "SHOPPING" // 쿠팡 등

            // 2. 횟수 및 비용 결정 로직
            var paymentCount = 1
            var finalCost = baseCost
            var minutes = 0

            if (category == "FOOD" || category == "SHOPPING") {
                // 배달/쇼핑은 횟수가 중요함 (1~5회 랜덤)
                paymentCount = Random.nextInt(1, 6)

                // 비용도 횟수만큼 뻥튀기 (예: 1회당 2만원 가정)
                val unitPrice = Random.nextInt(15000, 30000)
                finalCost = unitPrice * paymentCount

                // 배달/쇼핑은 사용시간 0분 (보통 앱 사용시간보다 결제액이 중요하므로)
                minutes = 0
            } else {
                // OTT/MUSIC은 구독형이라 횟수는 1회
                paymentCount = 1

                // 사용 시간은 랜덤 (0분 ~ 3000분)
                // 10% 확률로 '낭비' 패턴(사용시간 0) 생성
                minutes = if (Random.nextInt(100) < 10) 0 else Random.nextInt(60, 3000)
            }

            // 3. UserEntity 생성 및 리스트 추가
            mockList.add(
                UserEntity(
                    date = randomDate,
                    serviceName = name,
                    packageName = pkg,
                    cost = finalCost,       // 계산된 최종 비용
                    timeMinutes = minutes,  // 계산된 시간
                    logType = "MOCK",       // 식별자

                    // 👇 새로 추가한 컬럼에 값 넣기
                    category = category,
                    paymentCount = paymentCount
                )
            )
            // ---------------------------------------------------------------
        }

        // 4. DB 저장
        mockList.forEach { db.userDao().insertLog(it) }
        Log.d("MockCheck", "✅ 가상 데이터 50개 생성 완료 (카테고리/횟수 포함)")
    }
}