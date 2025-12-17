package com.example.myapplication.utils

import android.content.Context
import android.util.Log
import com.example.myapplication.R
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvLoader(private val context: Context) {

    fun readMasterData() {
        try {
            val inputStream = context.resources.openRawResource(R.raw.subscription_db)
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.forEachLine { line ->
                val data = line.split(",")
                Log.d("CsvCheck", "읽은 데이터: $data")
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CsvCheck", "CSV 읽기 실패: ${e.message}")
        }
    }
}