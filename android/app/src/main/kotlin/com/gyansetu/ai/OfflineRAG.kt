package com.gyansetu.ai

import com.gyansetu.data.SyllabusDao
import com.gyansetu.data.SyllabusEntity

/**
 * Lightweight on-device retriever. The Android port can grow this into a
 * full TF-IDF + MiniLM hybrid; for the hackathon MVP we ship a fast keyword
 * scan over Room rows, which works for a curriculum-sized corpus (~1000 rows).
 */
class OfflineRAG(private val dao: SyllabusDao) {

    suspend fun lookup(query: String): SyllabusEntity? {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return null
        // Tokens longer than 2 chars; require at least one match against
        // either English or Gujarati surface forms.
        val tokens = q.split(Regex("\\s+|[?.,!]")).filter { it.length > 2 }.distinct()
        if (tokens.isEmpty()) return null

        val rows = dao.search("%${tokens.first()}%")
        // Re-rank: prefer rows that match more tokens.
        return rows.maxByOrNull { row ->
            tokens.count { row.en.contains(it, true) || row.gu.contains(it) }
        }
    }
}
