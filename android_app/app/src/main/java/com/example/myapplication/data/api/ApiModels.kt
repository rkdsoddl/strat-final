package com.example.myapplication.data.api

import com.google.gson.annotations.SerializedName

// [1] 요청 모델 (Android -> Python Server)
data class AnalysisRequest(
    @SerializedName("logs")
    val logs: List<LogData>
)

data class LogData(
    @SerializedName("date") val date: Long,
    @SerializedName("serviceName") val serviceName: String,
    @SerializedName("category") val category: String,
    @SerializedName("cost") val cost: Int,
    @SerializedName("timeMinutes") val timeMinutes: Int,
    @SerializedName("payment_count") val paymentCount: Int,
    @SerializedName("logType") val logType: String
)

// [2] 응답 모델 (Python Server -> Android)
data class AnalysisResponse(
    @SerializedName("persona")
    val persona: String,

    @SerializedName("recommendation")
    val recommendation: String,

    // monthly_report
    @SerializedName("monthly_report")
    val monthlyReport: List<ReportItem>,

    // 주간 요약 리포트
    @SerializedName("weekly_report")
    val weeklyReport: List<WeeklyItem>,

    // 실시간 최소 효율 게이지
    @SerializedName("realtime_status")
    val realtimeStatus: Map<String, RealtimeItem>,

    @SerializedName("pie_chart")
    val pieChart: List<PieChartItem>
)

// 월간 리포트 상세 항목
data class ReportItem(
    @SerializedName("service") val service: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("detail") val detail: String,
    @SerializedName("alert_level") val alertLevel: String
)

// 주간 리포트 항목
data class WeeklyItem(
    @SerializedName("service") val service: String,
    @SerializedName("usage_this_week") val usageThisWeek: Int,
    @SerializedName("message") val message: String
)

// 실시간 효율 데이터
data class RealtimeItem(
    @SerializedName("remaining") val remaining: Int,  // 남은 시간/횟수
    @SerializedName("unit") val unit: String,         // "분" 또는 "회"
    @SerializedName("percent") val percent: Int       // 달성률 (%)
)

// 파이차트 항목
data class PieChartItem(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("percent") val percent: Double
)