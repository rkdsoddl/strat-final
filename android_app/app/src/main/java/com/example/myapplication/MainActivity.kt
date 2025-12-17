package com.example.myapplication

import android.app.AppOpsManager
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.api.AnalyzeRequest
import com.example.myapplication.data.api.AnalyzeResponse
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.MockDataGenerator
import com.example.myapplication.utils.UsageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // DB 인스턴스 미리 생성
        val db = AppDatabase.getDatabase(this)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        db = db,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(db: AppDatabase, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. 권한 설정 버튼 (기존 유지)
        Button(onClick = { checkAndRequestPermissions(context) }) {
            Text("1. 권한 설정 (필수)")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 가상 데이터 생성 버튼 (Mock)
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                MockDataGenerator(db).generate()
            }
            Toast.makeText(context, "가상 데이터 생성 완료!", Toast.LENGTH_SHORT).show()
        }) {
            Text("2. 가상 데이터 생성 (Mock)")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. 실제 사용 시간 수집 버튼 (UsageManager)
        Button(onClick = {
            if (hasUsageStatsPermission(context)) {
                CoroutineScope(Dispatchers.IO).launch {
                    UsageManager(context).collectUsageStats()
                }
                Toast.makeText(context, "실제 사용 시간 수집 완료!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "권한이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("3. 실제 사용 시간 수집")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. 서버 분석 버튼 (Retrofit)
        Button(onClick = {
            sendDataToServer(db)
            Toast.makeText(context, "Logcat에서 'API_TEST'를 확인하세요!", Toast.LENGTH_LONG).show()
        }) {
            Text("4. 서버로 보내고 분석하기 (Start)")
        }
    }
}

// 서버로 데이터를 보내는 로직
fun sendDataToServer(db: AppDatabase) {
    CoroutineScope(Dispatchers.IO).launch {
        // (1) DB에서 데이터 꺼내오기
        val logs = db.userDao().getAllLogs()

        if (logs.isEmpty()) {
            Log.e("API_TEST", "❌ 보낼 데이터가 없습니다. 2번 버튼을 먼저 누르세요!")
            return@launch
        }

        // (2) 서버 양식에 맞게 변환
        val logMaps = logs.map { entity ->
            mapOf(
                "serviceName" to entity.serviceName,
                "cost" to entity.cost,
                "timeMinutes" to entity.timeMinutes,

                "category" to entity.category,
                
                "payment_count" to entity.paymentCount
            )
        }

        val requestData = AnalyzeRequest(logs = logMaps)

        // (3) 서버 전송 (Retrofit)
        RetrofitClient.instance.analyzeData(requestData).enqueue(object : Callback<AnalyzeResponse> {
            override fun onResponse(call: Call<AnalyzeResponse>, response: Response<AnalyzeResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("API_TEST", "✅ 분석 성공!")
                    Log.d("API_TEST", "👤 페르소나: ${result?.user_persona}")

                    result?.inefficiency_report?.forEach {
                        Log.d("API_TEST", "📢 [${it.service}] ${it.status}: ${it.reason}")
                    }
                } else {
                    Log.e("API_TEST", "❌ 서버 에러: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AnalyzeResponse>, t: Throwable) {
                Log.e("API_TEST", "❌ 통신 실패: ${t.message}")
            }
        })
    }
}

// 권한 체크 및 요청 함수
fun checkAndRequestPermissions(context: Context) {
    // 알림 권한
    if (!isNotificationServiceEnabled(context)) {
        Toast.makeText(context, "알림 권한을 켜주세요", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }
    // 사용 정보 권한
    if (!hasUsageStatsPermission(context)) {
        Toast.makeText(context, "사용 정보 권한을 켜주세요", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(), context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}