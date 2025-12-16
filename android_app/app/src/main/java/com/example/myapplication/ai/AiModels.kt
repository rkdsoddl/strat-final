package com.example.myapplication.ai

data class ClassificationResult(
    val normalizedText: String,
    val merchant: String,
    val service: String?,
    val category: String?,
    val confidence: Double,
    val periodDays: Int?,
    val periodReason: String,
    val meta: Map<String, Any?> = emptyMap()
)

data class UsageLog(
    val service: String,
    val metricType: String, // "time_minutes" | "count" | "amount"
    val value: Double,
    val month: String       // "YYYY-MM"
)

data class PaymentLog(
    val service: String,
    val amount: Int,
    val date: String,       // "YYYY-MM-DD"
    val method: String? = null
)

data class SubscriptionItem(
    val service: String,
    val plan: String? = null,
    val price: Int? = null,
    val billingCycleDays: Int? = null,
    val status: String? = null
)

data class OptimizationResult(
    val persona: String,
    val efficiencyScores: Map<String, Double>,
    val recommendations: List<Map<String, Any>>,
    val summary: String
)

data class RagChunk(
    val source: String,
    val text: String
)

data class DiscountEdge(
    val from: String,
    val to: String,
    val amount: String,
    val condition: String
)
