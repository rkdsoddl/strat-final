package com.example.myapplication.data.api

// 1. 안드로이드 -> 서버로 보낼 때 (요청)
data class AnalyzeRequest(
    val logs: List<Map<String, Any>> // 서비스명, 금액, 시간
)

// 2. 서버 -> 안드로이드로 받을 때 (응답)
data class AnalyzeResponse(
    val user_persona: String,
    val inefficiency_report: List<ReportItem>,
    val recommendation: String
)

data class ReportItem(
    val service: String,
    val status: String,
    val reason: String
)