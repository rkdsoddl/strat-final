package com.example.myapplication.ai

import kotlin.math.max

object TextNormalizer {

    private val replacements = listOf(
        "*" to " ",
        Regex("\\s+") to " ",
        Regex("GOOGLE\\s*PAY|GOOGLEPAY") to "GOOGLE",
        Regex("NAVER\\s*\\*?\\s*PAY|NAVERPAY") to "NAVER PAY",
        Regex("APPLE\\s*\\*?\\s*COM|APPLECOM") to "APPLE"
    )

    // alias 사전 (더 필요하면 알려줘요!!!!!!!)
    private val serviceAliases: Map<String, List<String>> = mapOf(
        "NETFLIX" to listOf("NETFLIX", "NETFLX", "넷플릭스"),
        "YOUTUBE_PREMIUM" to listOf("YOUTUBE", "GOOGLE YOUTUBE", "YOUTUBE PREMIUM", "YT PREMIUM", "유튜브"),
        "DISNEY_PLUS" to listOf("DISNEY", "DISNEYPLUS", "디즈니", "디즈니플러스"),
        "WATCHA" to listOf("WATCHA", "왓챠"),
        "WAVVE" to listOf("WAVVE", "웨이브"),
        "TVING" to listOf("TVING", "티빙"),
        "COUPANG_PLAY" to listOf("COUPANGPLAY", "COUPANG PLAY", "쿠팡플레이", "쿠팡"),
        "SPOTIFY" to listOf("SPOTIFY", "스포티파이"),
        "MELON" to listOf("MELON", "멜론"),
        "GENIE" to listOf("GENIE", "지니"),
        "BAEMIN" to listOf("BAEMIN", "배민", "배달의민족"),
        "YOGIYO" to listOf("YOGIYO", "요기요")
    )

    fun basicNormalize(input: String): String {
        var t = input.trim().uppercase()
        // Regex replacements
        replacements.forEach { pair ->
            when (val key = pair.first) {
                is String -> t = t.replace(key, pair.second as String)
                is Regex -> t = t.replace(key, pair.second as String)
            }
        }
        // keep letters/digits/korean + some separators
        t = t.replace(Regex("[^A-Z0-9가-힣\\s\\-_.]"), " ")
        t = t.replace(Regex("\\s+"), " ").trim()
        return t
    }

    fun extractMerchant(normalized: String): String {
        var t = normalized
        t = t.replace(Regex("\\b\\d+[\\d,]*\\b"), " ")
        t = t.replace(Regex("\\b(KRW|USD|원)\\b"), " ")
        t = t.replace(Regex("\\s+"), " ").trim()
        return if (t.isNotBlank()) t.take(80) else normalized.take(80)
    }

    /**
     * Fuzzy-lite matching: longest common substring-ish score via token overlap + contains
     * (빠르고 가벼움. ML 아님)
     */
    fun matchService(normalized: String): Pair<String?, Double> {
        var bestService: String? = null
        var bestScore = 0.0

        for ((svc, aliases) in serviceAliases) {
            for (alias in aliases) {
                val a = basicNormalize(alias)
                val score = simpleSimilarity(normalized, a)
                if (score > bestScore) {
                    bestScore = score
                    bestService = svc
                }
            }
        }

        // threshold
        return if (bestScore >= 0.70) bestService to bestScore else null to bestScore
    }

    private fun simpleSimilarity(text: String, alias: String): Double {
        if (alias.isBlank()) return 0.0
        if (text.contains(alias)) return 1.0

        val tTokens = text.split(" ").filter { it.isNotBlank() }.toSet()
        val aTokens = alias.split(" ").filter { it.isNotBlank() }.toSet()
        if (tTokens.isEmpty() || aTokens.isEmpty()) return 0.0

        val intersect = tTokens.intersect(aTokens).size
        val union = tTokens.union(aTokens).size

        // Jaccard-like
        return intersect.toDouble() / max(union, 1).toDouble()
    }
}
