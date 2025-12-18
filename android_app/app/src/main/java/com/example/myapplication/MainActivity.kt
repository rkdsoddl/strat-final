package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.myapplication.data.api.*
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.utils.MockDataGenerator
import com.example.myapplication.utils.UsageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usageManager = UsageManager(this)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            usageManager.collectUsageStats()
        }
        setContent {
            MyApplicationTheme { MainScreen() }
        }
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Color(0xFF6200EE), secondary = Color(0xFF03DAC5)),
        content = content
    )
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    var analysisResult by remember { mutableStateOf<AnalysisResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💰 SUBFIT", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    MockDataGenerator(db).generate()
                    Toast.makeText(context, "가상 데이터 생성 완료!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("🛠️ 1. 데이터 생성")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        val logs = withContext(Dispatchers.IO) { db.userDao().getAllLogs() }
                        if (logs.isEmpty()) {
                            Toast.makeText(context, "데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                            isLoading = false
                            return@launch
                        }
                        val requestData = logs.map {
                            LogData(it.date, it.serviceName, it.category, it.cost, it.timeMinutes, it.paymentCount, it.logType)
                        }

                        // 서버 요청
                        val response = RetrofitClient.api.analyzeData(AnalysisRequest(requestData))
                        analysisResult = response

                        // 알림 로직
                        val warnings = response.monthlyReport.filter { it.alertLevel == "WARNING" }
                        if (warnings.isNotEmpty()) {
                            sendNotification(context, "🚨 구독 낭비 경고", "${warnings[0].service} 외 ${warnings.size-1}건 낭비 중!")
                        } else {
                            // 추천 메시지 알림
                            sendNotification(context, "💡 AI 추천 도착", response.recommendation)
                        }
                    } catch (e: Exception) {
                        Log.e("API", "Error", e)
                        Toast.makeText(context, "분석 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF018786))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 분석 중...")
            } else {
                Text("📊 2. AI 소비 분석 시작")
            }
        }
    }

    if (analysisResult != null) {
        AnalysisResultDialog(result = analysisResult!!, onDismiss = { analysisResult = null })
    }
}

fun sendNotification(context: Context, title: String, message: String) {
    val channelId = "sub_alert_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "구독 알림", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

// ==========================================
// [UI] 분석 결과 다이얼로그
// 주간 -> 월간 -> 페르소나 -> 추천 -> 파이차트
// ==========================================
@Composable
fun AnalysisResultDialog(result: AnalysisResponse, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📊 AI 분석 리포트", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. [주간 리포트] (가장 먼저)
                item {
                    Text("📅 주간 리포트 (Weekly)", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
                if (result.weeklyReport.isEmpty()) {
                    item { Text("주간 데이터 없음", fontSize = 12.sp, color = Color.Gray) }
                } else {
                    items(result.weeklyReport) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${item.service}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(item.message, fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                // 2. [월간 리포트] (효율/비효율 분석)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📋 월간 구독 효율 (Monthly)", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
                if (result.monthlyReport.isEmpty()) {
                    item { Text("구독 데이터 없음", fontSize = 12.sp, color = Color.Gray) }
                } else {
                    items(result.monthlyReport) { item ->
                        ReportRow(item)
                    }
                }

                // 3. [페르소나 분석]
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("👤 나의 페르소나", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(result.persona, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF3F51B5))
                        }
                    }
                }

                // 4. [구독 서비스 추천] (지출 기반 맞춤 추천)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("💡 AI 맞춤 추천", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        modifier = Modifier.fillMaxWidth().padding(top=8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤖", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = result.recommendation,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // 5. [파이차트]
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("💰 지출 분석 (Pie Chart)", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    if (result.pieChart.isNotEmpty()) {
                        SimplePieChart(data = result.pieChart)
                    } else {
                        Text("지출 데이터 부족", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("확인") }
        }
    )
}

@Composable
fun ReportRow(item: ReportItem) {
    val isWarning = item.alertLevel == "WARNING"
    val isGood = item.alertLevel == "GOOD"

    val bgColor = when {
        isWarning -> Color(0xFFFFEBEE)
        isGood -> Color(0xFFE8F5E9)
        else -> Color(0xFFF5F5F5)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.service, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(item.status, color = if(isWarning) Color.Red else Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.message, fontSize = 13.sp, color = Color.DarkGray) // "평균보다 30% 더 씀"
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.detail, fontSize = 11.sp, color = Color.Gray) // "월 2시간 30분 사용"
        }
    }
}

@Composable
fun SimplePieChart(data: List<PieChartItem>) {
    val chartColors = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A),
        Color(0xFFFFA726), Color(0xFFAB47BC), Color(0xFF8D6E63)
    )
    Row(modifier = Modifier.fillMaxWidth().height(140.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweepAngle = (item.percent.toFloat() / 100f) * 360f
                    drawArc(color = chartColors[index % chartColors.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                    startAngle += sweepAngle
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            data.take(5).forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(chartColors[index % chartColors.size], CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${item.percent.toInt()}%", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}