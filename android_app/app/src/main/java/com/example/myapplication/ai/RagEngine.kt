package com.example.myapplication.ai

class RagEngine(private val chunks: List<RagChunk>) {

    fun search(query: String, k: Int = 3): List<RagChunk> {
        val q = query.lowercase()
        return chunks
            .map { c -> c to score(c.text.lowercase(), q) }
            .sortedByDescending { it.second }
            .take(k)
            .map { it.first }
    }

    private fun score(text: String, q: String): Int {
        if (q.isBlank()) return 0
        return text.split(" ").count { token -> token.contains(q) || q.contains(token) }
    }

    companion object {
        fun default(): RagEngine {
            return RagEngine(
                listOf(
                    RagChunk("netflix_terms", "넷플릭스는 일반적으로 언제든지 해지할 수 있으며, 해지 후 다음 결제일까지 시청 가능합니다."),
                    RagChunk("youtube_terms", "유튜브 프리미엄은 결제 주기에 따라 자동 갱신되며, 해지 시 다음 결제일까지 혜택이 유지됩니다.")
                )
            )
        }
    }
}
