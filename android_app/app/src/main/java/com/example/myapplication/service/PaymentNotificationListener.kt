package com.example.myapplication.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val extras = sbn?.notification?.extras
        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getString("android.text") ?: ""
        val packageName = sbn?.packageName ?: ""

        // 로그 확인용
        Log.d("NotiListener", "앱: $packageName | 내용: $text")

        // 1. 결제 키워드가 있는지 확인 (은행, 카드사마다 다를 수 있음)
        if (text.contains("승인") || text.contains("출금") || text.contains("결제")) {

            // 2. 금액 추출 (숫자와 ,원 만 찾아서 숫자로 변환)
            // 예: "13,500원 승인" -> 13500
            val amountRegex = Regex("[0-9,]+원")
            val amountMatch = amountRegex.find(text)
            val amountStr = amountMatch?.value?.replace(",", "")?.replace("원", "")
            val cost = amountStr?.toIntOrNull() ?: 0

            if (cost > 0) {
                // 3. 서비스명 파악 (여기가 핵심! Pandas가 이 이름을 씁니다)
                var serviceName = "기타"

                // [규칙 정의] 문자에 '우아한형제들'이 있으면 -> 배달의민족
                if (text.contains("우아한형제들") || text.contains("배달의민족")) {
                    serviceName = "배달의민족"
                }
                // [규칙 정의] 넷플릭스
                else if (text.contains("넷플릭스") || text.contains("Netflix")) {
                    serviceName = "넷플릭스"
                }
                // [규칙 정의] 유튜브
                else if (text.contains("Google") || text.contains("유튜브")) {
                    serviceName = "유튜브"
                }

                // 만약 우리가 관심 있는 서비스라면 DB에 저장!
                if (serviceName != "기타") {
                    saveToDb(serviceName, packageName, cost)
                }
            }
        }
    }

    private fun saveToDb(service: String, pkg: String, money: Int) {
        val db = AppDatabase.getDatabase(applicationContext)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()).toLong()

        // 백그라운드 작업으로 DB 저장
        CoroutineScope(Dispatchers.IO).launch {
            val log = UserEntity(
                date = today,
                serviceName = service,
                packageName = pkg,
                cost = money,
                timeMinutes = 0, // 결제 로그니까 시간은 0
                logType = "PAYMENT"
            )
            db.userDao().insertLog(log)
            Log.d("NotiListener", "DB 저장 완료: $service, $money 원")
        }
    }
}