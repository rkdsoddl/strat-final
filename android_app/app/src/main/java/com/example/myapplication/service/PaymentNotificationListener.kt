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

        // 1. 결제 키워드가 있는지 확인
        if (text.contains("승인") || text.contains("출금") || text.contains("결제")) {

            // 2. 금액 추출 (숫자와 ,원 만 찾아서 숫자로 변환)
            val amountRegex = Regex("[0-9,]+원")
            val amountMatch = amountRegex.find(text)
            val amountStr = amountMatch?.value?.replace(",", "")?.replace("원", "")
            val cost = amountStr?.toIntOrNull() ?: 0

            if (cost > 0) {
                // 3. 서비스명 파악
                var serviceName = "기타"

                if (text.contains("우아한형제들") || text.contains("배달의민족")) {
                    serviceName = "배달의민족"
                }
                else if (text.contains("넷플릭스") || text.contains("Netflix")) {
                    serviceName = "넷플릭스"
                }
                else if (text.contains("Google") || text.contains("유튜브")) {
                    serviceName = "유튜브"
                }

                // 관심 있는 서비스라면 DB에 저장
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
                timeMinutes = 0, // 결제 로그, 시간은 0
                logType = "PAYMENT",
                category = "REALTIME",
                paymentCount = 1
            )

            // log(한 개)를 listOf(log)로 감싸서 리스트 형태로 전달
            db.userDao().insertLog(listOf(log))

            Log.d("NotiListener", "DB 저장 완료: $service, $money 원")
        }
    }
}