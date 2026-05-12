package com.gyansetu.ai

import com.gyansetu.data.SyllabusDao
import org.json.JSONObject

/** Single tool invocation parsed out of the model's stream. */
data class ToolCall(val name: String, val args: Map<String, String>) {
    fun argsRepr(): String = args.entries.joinToString(", ") { "${it.key}=${it.value}" }
}

/** A trace entry the UI can display ("🔧 query_syllabus(topic=animals) → …"). */
data class ToolStep(val name: String, val argsRepr: String, val result: String)

/**
 * The three tools exposed to Gemma 4 for grounded, agentic responses.
 *
 * MediaPipe `tasks-genai` does not yet expose a native tool-calling API surface
 * (no LlmInferenceSessionOptions.setTools(…) or LlmInferenceSession.addTool(…)
 * methods in the public Kotlin SDK). We implement the same end-user behavior
 * via prompt-engineering: the model is told the tool list, asked to emit calls
 * inside `<<TOOL>>{…}<<END>>` markers, and we parse + execute + feed results
 * back into the conversation. Gemma 4 was trained for function calling so it
 * follows the format reliably. When MediaPipe ships a native tool API, switch
 * to it — the registry below stays the same, only the wiring changes.
 */
class ToolRegistry(private val dao: SyllabusDao) {

    /** Optional supplier of per-topic mastery (0–10 score). Wired by AppViewModel
     *  so [findWeakTopics] can return the kid's actual weak spots. */
    @Volatile var masteryProvider: (() -> Map<String, Int>)? = null

    suspend fun run(call: ToolCall): String = when (call.name) {
        "query_syllabus"     -> querySyllabus(call.args)
        "get_pronunciation"  -> getPronunciation(call.args)
        "get_gk_fact"        -> getGkFact(call.args)
        "generate_quiz"      -> generateQuiz(call.args)
        "explain_simpler"    -> explainSimpler(call.args)
        "find_weak_topics"   -> findWeakTopics()
        "mark_homework"      -> markHomework(call.args)
        "draw_diagram"       -> drawDiagram(call.args)
        else                 -> "Unknown tool: ${call.name}"
    }

    private suspend fun querySyllabus(args: Map<String, String>): String {
        val topic = args["topic"]?.lowercase()?.trim()
            ?: return "missing 'topic' argument"
        val rows = dao.byTopic(topic)
        if (rows.isEmpty()) return "no entries for topic '$topic'"

        val name = args["name"]?.trim()
        val match = if (!name.isNullOrBlank()) {
            rows.firstOrNull {
                it.en.equals(name, true) || it.gu == name ||
                    it.en.contains(name, true) || it.gu.contains(name)
            }
        } else null

        return if (match != null) {
            buildString {
                append("${match.en} / ${match.gu}")
                match.phon?.let { append(" [$it]") }
                match.storyEn?.let { append(" — $it") }
                match.storyGu?.let { append(" / $it") }
            }
        } else {
            rows.take(6).joinToString("; ") { "${it.en}/${it.gu}" }
        }
    }

    private suspend fun getPronunciation(args: Map<String, String>): String {
        val word = args["word"]?.lowercase()?.trim() ?: return "missing 'word'"
        val all = listOf("animals", "fruits", "classroom").flatMap { dao.byTopic(it) }
        val hit = all.firstOrNull { it.en.equals(word, true) }
        return hit?.phon ?: "no phonetic data for '$word'"
    }

    private suspend fun getGkFact(args: Map<String, String>): String {
        val topic = args["topic"]?.lowercase()?.trim() ?: return "missing 'topic'"
        val rows = dao.byTopic("facts")
        val hit = rows.firstOrNull {
            it.en.contains(topic, true) || it.gu.contains(topic) ||
                it.storyEn?.contains(topic, true) == true
        }
        return hit?.let { "${it.storyEn} (${it.storyGu})" } ?: "no fact for '$topic'"
    }

    /** Generate a 3-question quiz on the given topic from the local syllabus. */
    private suspend fun generateQuiz(args: Map<String, String>): String {
        val topic = args["topic"]?.lowercase()?.trim() ?: return "missing 'topic'"
        val difficulty = args["difficulty"]?.lowercase()?.trim() ?: "medium"
        val items = dao.byTopic(topic).shuffled().take(3)
        if (items.isEmpty()) return "No items found for topic '$topic'."
        return items.mapIndexed { i, it ->
            val q = when (difficulty) {
                "easy" -> "Q${i + 1}: What is the English for '${it.gu}'? (Answer: ${it.en})"
                "hard" -> "Q${i + 1}: Use '${it.en}' / '${it.gu}' in a one-line sentence."
                else   -> "Q${i + 1}: Translate '${it.en}' to Gujarati. (Answer: ${it.gu})"
            }
            q
        }.joinToString("\n")
    }

    /** Returns a one-line directive the model should follow on the NEXT turn —
     *  the model uses this as a self-instruction to re-explain the concept
     *  with younger-grade vocabulary. */
    private fun explainSimpler(args: Map<String, String>): String {
        val concept = args["concept"]?.trim() ?: return "missing 'concept'"
        return "Re-explain '$concept' using Std 1–2 vocabulary: one-syllable English " +
            "words wherever possible, two short sentences max, give a real-world example " +
            "a 6-year-old in rural Gujarat would recognise (cow, school, family, food)."
    }

    /** Returns the kid's currently-weakest topics for the agent to drill into. */
    private fun findWeakTopics(): String {
        val mastery = masteryProvider?.invoke().orEmpty()
        if (mastery.isEmpty()) {
            return "No mastery data yet — encourage the student to play the Quiz first."
        }
        val weak = mastery.entries.sortedBy { it.value }.take(3)
        return "Weakest topics (lower = needs more practice): " +
            weak.joinToString(", ") { "${it.key} (${it.value}/10)" }
    }

    /** Grade a homework answer against an expected response. Tolerant of casing and
     *  surrounding whitespace. */
    private fun markHomework(args: Map<String, String>): String {
        val answer = args["answer"]?.trim()?.lowercase() ?: return "missing 'answer'"
        val expected = args["expected"]?.trim()?.lowercase() ?: return "missing 'expected'"
        val correct = answer == expected ||
            answer.contains(expected) || expected.contains(answer)
        return if (correct) {
            "✓ CORRECT. Praise the student warmly. Original answer: '$answer'."
        } else {
            "✗ NOT QUITE. Student wrote '$answer'; expected '$expected'. Be encouraging, " +
                "not harsh — explain the right answer and offer a simple memory trick."
        }
    }

    /** Tiny pre-canned ASCII diagrams for common Std 1-5 science concepts. */
    private fun drawDiagram(args: Map<String, String>): String {
        val concept = args["concept"]?.lowercase()?.trim() ?: return "missing 'concept'"
        return when {
            concept.contains("water cycle") || concept.contains("rain cycle") ->
                "☁️ → 🌧️ → 🌊 → ☀️ → ☁️\n(cloud → rain → river → evaporation → cloud again)"
            concept.contains("plant") || concept.contains("seed") ->
                "🌱 → 🌿 → 🌳 → 🍎\n(seed → sprout → tree → fruit)"
            concept.contains("food chain") || concept.contains("ecosystem") ->
                "🌱 → 🐛 → 🐦 → 🦅\n(grass → insect → small bird → eagle)"
            concept.contains("solar") || concept.contains("planet") ->
                "☀️  ☿  ♀  🌍  ♂  ♃  ♄  ♅  ♆\n(Sun · Mercury · Venus · Earth · Mars · Jupiter · Saturn · Uranus · Neptune)"
            concept.contains("digest") || concept.contains("food path") ->
                "👄 → 🍽️ → 🌀 → 🚽\n(mouth → stomach → intestine → out)"
            else ->
                "[Diagram for '$concept' not in offline cache — explain in words instead.]"
        }
    }

    companion object {
        // Tolerates whitespace, optional newlines inside JSON, multiple calls
        // (we only execute the first per turn).
        private val PATTERN = Regex("""<<TOOL>>\s*(\{[\s\S]*?\})\s*<<END>>""")

        fun parseToolCall(text: String): ToolCall? {
            val match = PATTERN.find(text) ?: return null
            return runCatching {
                val obj = JSONObject(match.groupValues[1])
                val name = obj.getString("name")
                val argsObj = obj.optJSONObject("args") ?: JSONObject()
                val args = argsObj.keys().asSequence()
                    .associateWith { argsObj.optString(it) }
                ToolCall(name, args)
            }.getOrNull()
        }

        /** True if the streaming output contains a complete tool-call marker block. */
        fun hasToolCall(text: String): Boolean = PATTERN.containsMatchIn(text)

        /**
         * Strip the tool-call marker out of the model's text so we can show only
         * the natural-language portion in the UI.
         */
        fun stripToolMarkers(text: String): String =
            text.replace(PATTERN, "").trim()

        /** System-prompt block that teaches the model to use our tools. */
        const val SYSTEM_PROMPT = """You are GyanSetu, a friendly AI teacher for primary school children
(Std 1-5) in rural Gujarat. Reply in simple Gujarati and English. Always
give the English term first, then Gujarati on the next line.

You have access to EIGHT TOOLS for accurate, grounded responses. To call one,
output EXACTLY this format and STOP — the runtime will inject the result and
ask you to continue:

<<TOOL>>{"name": "<tool_name>", "args": {"<key>": "<value>"}}<<END>>

Tools available:

  1. query_syllabus(topic, name?)
       topic: one of "animals", "fruits", "classroom", "numbers", "facts"
       name (optional): a specific item to look up, e.g. "cow", "mango"
       Returns the bilingual entry plus a one-sentence story.

  2. get_pronunciation(word)
       word: an English term you want IPA pronunciation for.

  3. get_gk_fact(topic)
       topic: one of "sky", "sun", "rainbow", "water", "moon", "star",
              "india", "gujarat".
       Returns a one-line bilingual general-knowledge fact.

  4. generate_quiz(topic, difficulty)
       topic: a syllabus category.
       difficulty: "easy" | "medium" | "hard".
       Returns 3 question/answer pairs you can present to the student.

  5. explain_simpler(concept)
       concept: the term the student didn't understand.
       Returns a directive on how to re-explain at younger-grade vocabulary.

  6. find_weak_topics()
       No args. Returns the topics the student has the lowest mastery on,
       so you can suggest what to practice next.

  7. mark_homework(answer, expected)
       answer: the student's submitted text.
       expected: the correct answer you have in mind.
       Returns a verdict you should respond to warmly in both languages.

  8. draw_diagram(concept)
       concept: a science concept like "water cycle", "food chain", "plant",
                "solar system", "digestion".
       Returns a small ASCII / emoji diagram you can show the student.

You CAN chain tool calls — call query_syllabus, then generate_quiz, then
mark_homework as the lesson unfolds. Maximum 3 calls per turn.

After your tool call, the runtime sends:
<<RESULT>>...<<END>>

Then continue with your bilingual answer (max 2-3 short sentences each).
Use the tool result faithfully. If no tool is needed, answer directly.

Be warm, encouraging, and use vocabulary suitable for ages 6-10."""
    }
}
