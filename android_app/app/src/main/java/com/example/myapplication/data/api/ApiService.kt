package com.example.myapplication.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("analyze")
    suspend fun analyzeData(
        @Body request: AnalysisRequest
    ): AnalysisResponse
}