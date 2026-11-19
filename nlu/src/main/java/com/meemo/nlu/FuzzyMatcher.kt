package com.meemo.nlu

data class FuzzyMatch<T>(val value: T, val score: Float)

class FuzzyMatcher {
    fun <T> rank(query: String, candidates: List<T>, label: (T) -> String): List<FuzzyMatch<T>> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        return candidates.map { candidate ->
            FuzzyMatch(candidate, similarity(normalizedQuery, normalize(label(candidate))))
        }.sortedByDescending { it.score }
    }

    fun <T> bestMatch(
        query: String,
        candidates: List<T>,
        label: (T) -> String,
        threshold: Float = 0.8f
    ): FuzzyMatch<T>? = rank(query, candidates, label).firstOrNull()?.takeIf { it.score >= threshold }

    private fun normalize(value: String) = value.lowercase().trim().replace(Regex("[^\\p{L}\\p{N}]"), "")

    private fun similarity(left: String, right: String): Float {
        if (left == right) return 1f
        if (left.isEmpty() || right.isEmpty()) return 0f
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            val temp = previous
            previous = current
            current = temp
        }
        return 1f - previous[right.length].toFloat() / maxOf(left.length, right.length)
    }
}
