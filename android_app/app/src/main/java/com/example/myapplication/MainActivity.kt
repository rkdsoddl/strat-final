package com.example.myapplication

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 메인 화면 UI 함수 호출
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // 화면에 보여줄 텍스트
    var usageResult by remember { mutableStateOf("버튼을 눌러 사용 시간을 확인하세요.") }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. 권한 체크 및 설정 이동 버튼
        Button(onClick = {
            checkAndRequestPermissions(context)
        }) {
            Text(text = "권한 설정하러 가기 (필수)")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 유튜브 사용 시간 가져오기 버튼
        Button(onClick = {
            // 권한이 있는지 먼저 확인
            if (hasUsageStatsPermission(context)) {
                // 유튜브 패키지명: com.google.android.youtube
                val time = getAppUsageTime(context, "com.google.android.youtube")
                usageResult = "오늘 유튜브 사용 시간: $time"
            } else {
                usageResult = "먼저 권한을 허용해주세요!"
                Toast.makeText(context, "사용 정보 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(text = "오늘 유튜브 사용시간 조회")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. 결과 텍스트
        Text(text = usageResult)
    }
}

// 1. 권한이 있는지 확인하고 없으면 설정 화면으로 보내는 함수
fun checkAndRequestPermissions(context: Context) {
    // (1) 알림 접근 권한 (결제 문자 읽기용)
    if (!isNotificationServiceEnabled(context)) {
        Toast.makeText(context, "알림 접근 권한을 켜주세요!", Toast.LENGTH_LONG).show()
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    } else {
        Toast.makeText(context, "알림 권한은 이미 있습니다.", Toast.LENGTH_SHORT).show()
    }

    // (2) 사용 정보 접근 권한 (앱 사용 시간용)
    if (!hasUsageStatsPermission(context)) {
        Toast.makeText(context, "사용 추적 권한을 켜주세요!", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

// 2. 특정 앱의 오늘 사용 시간 가져오는 함수
fun getAppUsageTime(context: Context, targetPackageName: String): String {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 시간 범위: 오늘 0시 ~ 현재
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis

    val usageStatsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, endTime
    )

    // 해당 패키지 찾기
    val stats = usageStatsList.find { it.packageName == targetPackageName }

    return if (stats != null) {
        val totalSeconds = stats.totalTimeInForeground / 1000
        val min = totalSeconds / 60
        val sec = totalSeconds % 60
        "${min}분 ${sec}초"
    } else {
        "사용 기록 없음 (0분)"
    }
}

// 3. 알림 리스너 권한 체크 헬퍼
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

// 4. 사용 정보 접근 권한 체크 헬퍼
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(), context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}