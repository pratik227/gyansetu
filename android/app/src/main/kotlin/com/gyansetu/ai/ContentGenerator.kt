package com.gyansetu.ai

import android.util.Log
import com.gyansetu.data.AskSuggestion
import com.gyansetu.data.DailyTip
import com.gyansetu.data.Encouragement
import com.gyansetu.data.MatchPair
import com.gyansetu.data.QuizOption
import com.gyansetu.data.QuizQuestion
import com.gyansetu.data.TeacherRec
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Produces fresh GK quiz, match-game, ask-suggestion, encouragement, daily-tip,
 * teacher-recommendation, and item-explanation content via the on-device Gemma 4 model.
 *
 * Variety strategy (every call gets all four):
 *   1. CREATIVE / VERY_CREATIVE sampling (high temperature) so the model breaks
 *      out of the few attractor modes a 2B model collapses to under low temp.
 *   2. A randomised "seed topics" hint pulled from a 60+ subject pool that spans
 *      the actual world — vehicles, dinosaurs, space, weather, sports, instruments,
 *      etc. — not just the old animals/fruits/classroom cage.
 *   3. A recently-seen blacklist (in-memory ring buffer) appended to every prompt
 *      so the same questions/words don't recur within a session.
 *   4. System prompts contain ONLY structural format examples, never concrete
 *      content like "national bird of India" — those examples were anchoring
 *      every generation onto peacock-shaped answers.
 *
 * Reliability: parsers stay lenient — drop bad items, return whatever survived.
 * Callers fall back to a static pool if too few survived.
 */
class ContentGenerator(private val gemma: GemmaInferenceEngine) {

    // In-memory recently-shown rings. We only track within a single app process;
    // cross-launch repetition is acceptable since process death is rare on the
    // splash → home flow we expect kids to use.
    private val recentQuizQs = ArrayDeque<String>()
    private val recentMatchEns = ArrayDeque<String>()
    private val recentAskEns = ArrayDeque<String>()
    private val recentTipEns = ArrayDeque<String>()
    private val recentEncouragementEns = ArrayDeque<String>()

    private fun pushAndCap(deque: ArrayDeque<String>, items: Collection<String>, cap: Int = 30) {
        items.forEach { deque.addLast(it.lowercase().trim()) }
        while (deque.size > cap) deque.removeFirst()
    }

    private fun seedHint(pool: List<String>, n: Int): String =
        pool.shuffled().take(n).joinToString(", ")

    private fun avoidBlock(items: Collection<String>, label: String): String =
        if (items.isEmpty()) ""
        else "\n\nAVOID repeating any of these recently-shown $label (do NOT use these words/questions again):\n" +
            items.joinToString("\n") { "- $it" }

    /* ─── Quiz ──────────────────────────────────────────────────────────── */

    suspend fun generateQuiz(count: Int = 5): List<QuizQuestion> {
        // Force a *different* random subject for every slot — this is the strongest
        // anti-repetition lever for a 2B model. Without per-slot assignment the
        // model collapses all $count questions onto its high-probability prior
        // (peacock/lion/tiger/mango).
        val assignedSubjects = QUIZ_SUBJECT_POOL.shuffled().take(count)
        val numbered = assignedSubjects
            .mapIndexed { i, t -> "  ${i + 1}. about: \"$t\"" }
            .joinToString("\n")
        val avoid = avoidBlock(recentQuizQs, "quiz questions")
        val userPrompt = """
            Generate exactly $count NEW quiz questions. Each question MUST be about
            a DIFFERENT subject from this list, in this exact order:
            $numbered

            DO NOT default to "national bird / national animal / capital / colours
            in rainbow / days in week" — pick fresh angles within each subject.$avoid

            Output ONLY the JSON array. No prose. No markdown fences.
        """.trimIndent()
        Log.d(TAG, "generateQuiz subjects: $assignedSubjects")
        val raw = gemma.generate(
            prompt = userPrompt,
            systemPrompt = QUIZ_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.VERY_CREATIVE,
        )
        Log.d(TAG, "generateQuiz raw length=${raw.length}, head=${raw.take(200)}")
        val parsed = parseQuiz(raw)
        Log.d(TAG, "generateQuiz parsed ${parsed.size} valid questions: ${parsed.map { it.en }}")
        pushAndCap(recentQuizQs, parsed.map { it.en })
        return parsed
    }

    /* ─── Match ─────────────────────────────────────────────────────────── */

    suspend fun generateMatchPairs(count: Int = 5): List<MatchPair> {
        // Same per-slot assignment trick — each pair gets its own random category
        // so the batch can never come back as 5-of-the-same-thing.
        val assignedCategories = MATCH_SUBJECT_POOL.shuffled().take(count)
        val numbered = assignedCategories
            .mapIndexed { i, t -> "  ${i + 1}. from category: \"$t\"" }
            .joinToString("\n")
        val avoid = avoidBlock(recentMatchEns, "vocabulary words")
        val userPrompt = """
            Generate exactly $count NEW vocabulary pairs. Each pair MUST come from a
            DIFFERENT category, in this exact order:
            $numbered

            Pick uncommon, fun words — surprise the kid.$avoid

            Output ONLY the JSON array. No prose. No markdown fences.
        """.trimIndent()
        Log.d(TAG, "generateMatchPairs categories: $assignedCategories")
        val raw = gemma.generate(
            prompt = userPrompt,
            systemPrompt = MATCH_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.VERY_CREATIVE,
        )
        Log.d(TAG, "generateMatchPairs raw length=${raw.length}, head=${raw.take(200)}")
        val parsed = parseMatch(raw)
        Log.d(TAG, "generateMatchPairs parsed ${parsed.size} valid pairs: ${parsed.map { it.en }}")
        pushAndCap(recentMatchEns, parsed.map { it.en })
        return parsed
    }

    /* ─── Ask suggestions ───────────────────────────────────────────────── */

    suspend fun generateAskSuggestions(count: Int = 3): List<AskSuggestion> {
        val assignedSubjects = QUIZ_SUBJECT_POOL.shuffled().take(count)
        val numbered = assignedSubjects
            .mapIndexed { i, t -> "  ${i + 1}. about: \"$t\"" }
            .joinToString("\n")
        val avoid = avoidBlock(recentAskEns, "questions")
        Log.d(TAG, "generateAskSuggestions subjects: $assignedSubjects")
        val raw = gemma.generate(
            prompt = """
                Generate exactly $count NEW curiosity questions, in this exact order:
                $numbered

                DO NOT use any "sky / sun / rainbow" examples.$avoid

                Output ONLY the JSON array. No prose. No markdown fences.
            """.trimIndent(),
            systemPrompt = ASK_SUGGESTIONS_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.VERY_CREATIVE,
        )
        Log.d(TAG, "generateAskSuggestions raw head=${raw.take(200)}")
        val parsed = parseAskSuggestions(raw)
        Log.d(TAG, "generateAskSuggestions parsed: ${parsed.map { it.en }}")
        pushAndCap(recentAskEns, parsed.map { it.en })
        return parsed
    }

    /* ─── Encouragement ─────────────────────────────────────────────────── */

    suspend fun generateEncouragement(stars: Int, total: Int): Encouragement? {
        val band = when {
            total == 0 -> "low"
            stars == total -> "perfect"
            stars.toFloat() / total >= 0.5f -> "good"
            else -> "low"
        }
        val avoid = avoidBlock(recentEncouragementEns, "celebration lines")
        val raw = gemma.generate(
            prompt = "Score: $stars / $total ($band band). Write ONE celebration line " +
                "with a fresh metaphor — never reuse 'well done', 'good job', 'paid off'.$avoid\n\n" +
                "Output ONLY the JSON object.",
            systemPrompt = ENCOURAGEMENT_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.VERY_CREATIVE,
        )
        return parseSingleObject(raw)?.let { o ->
            val gu = o.optString("gu").trim()
            val en = o.optString("en").trim()
            if (gu.isEmpty() || en.isEmpty()) null else Encouragement(gu, en).also {
                pushAndCap(recentEncouragementEns, listOf(en), cap = 10)
            }
        }
    }

    /* ─── Daily tip ─────────────────────────────────────────────────────── */

    suspend fun generateDailyTip(): DailyTip? {
        val seeds = seedHint(QUIZ_SUBJECT_POOL, 6)
        val avoid = avoidBlock(recentTipEns, "fun facts")
        val raw = gemma.generate(
            prompt = "Generate today's fun fact. Pick something surprising and unusual " +
                "from any subject — inspiration: $seeds.$avoid\n\nOutput ONLY the JSON object.",
            systemPrompt = DAILY_TIP_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.VERY_CREATIVE,
        )
        return parseSingleObject(raw)?.let { o ->
            val gu = o.optString("gu").trim()
            val en = o.optString("en").trim()
            if (gu.isEmpty() || en.isEmpty()) null else DailyTip(gu, en).also {
                pushAndCap(recentTipEns, listOf(en), cap = 15)
            }
        }
    }

    /* ─── Teacher recommendation ────────────────────────────────────────── */

    suspend fun generateTeacherRecommendation(
        studentName: String,
        weakestTopic: String,
        masteryScore: Int,
    ): TeacherRec? {
        val technique = TEACHING_TECHNIQUE_POOL.random()
        val raw = gemma.generate(
            prompt = "Student: $studentName. Weakest topic: \"$weakestTopic\" with " +
                "$masteryScore/10 mastery. Recommend a fresh activity using this " +
                "technique style: \"$technique\".\n\nOutput ONLY the JSON object.",
            systemPrompt = TEACHER_REC_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.CREATIVE,
        )
        return parseSingleObject(raw)?.let { o ->
            val text = o.optString("text").trim()
            if (text.isEmpty()) null else TeacherRec(text)
        }
    }

    /* ─── Item explanation (streaming) ──────────────────────────────────── */

    fun streamItemExplanation(en: String, gu: String, readingLevel: String): Flow<String> {
        val levelHint = when (readingLevel) {
            "1-2" -> "Reading level: ages 6-7. ONE-syllable English words. Two sentences max."
            "5"   -> "Reading level: ages 10-11. Slightly longer vocabulary, three sentences."
            else  -> "Reading level: ages 8-9. Simple vocabulary, two to three short sentences."
        }
        return gemma.generateStream(
            prompt = "Tell me about: $en (Gujarati: $gu).\n$levelHint\n" +
                "Use a fresh angle this time — not the same explanation as last time.\nGyanSetu:",
            systemPrompt = ITEM_EXPLANATION_SYSTEM_PROMPT,
            sampling = GemmaInferenceEngine.SamplingMode.CREATIVE,
        )
    }

    /* ─── Parsers ───────────────────────────────────────────────────────── */

    private fun parseSingleObject(raw: String): JSONObject? {
        val start = raw.indexOf('{'); val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    private fun extractJsonArray(raw: String): JSONArray? {
        val start = raw.indexOf('['); val end = raw.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return runCatching { JSONArray(raw.substring(start, end + 1)) }.getOrNull()
    }

    private fun parseAskSuggestions(raw: String): List<AskSuggestion> {
        val arr = extractJsonArray(raw) ?: return emptyList()
        val seenEn = mutableSetOf<String>()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val en = o.getString("en").trim()
                val gu = o.getString("gu").trim()
                require(en.isNotEmpty() && gu.isNotEmpty()) { "empty suggestion" }
                require(seenEn.add(en.lowercase())) { "duplicate en: $en" }
                AskSuggestion(gu = gu, en = en)
            }.getOrNull()
        }
    }

    private fun parseQuiz(raw: String): List<QuizQuestion> {
        val arr = extractJsonArray(raw) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val opts = o.getJSONArray("options")
                require(opts.length() == 4) { "needs 4 options, got ${opts.length()}" }
                val parsedOpts = (0 until opts.length()).map { j ->
                    val op = opts.getJSONObject(j)
                    QuizOption(
                        gu = op.getString("gu").trim(),
                        en = op.getString("en").trim(),
                        icon = op.optString("icon", "✨").trim().ifEmpty { "✨" },
                        correct = op.optBoolean("correct", false),
                    )
                }
                require(parsedOpts.count { it.correct } == 1) { "needs exactly 1 correct option" }
                require(parsedOpts.all { it.gu.isNotEmpty() && it.en.isNotEmpty() }) { "empty option text" }
                val topic = o.optString("topic", "facts").trim().lowercase()
                QuizQuestion(
                    emoji = o.optString("emoji", "✨").trim().ifEmpty { "✨" },
                    gu = o.getString("gu").trim().also { require(it.isNotEmpty()) },
                    en = o.getString("en").trim().also { require(it.isNotEmpty()) },
                    topic = if (topic in MASTERY_BUCKETS) topic else "facts",
                    options = parsedOpts,
                )
            }.getOrNull()
        }
    }

    private fun parseMatch(raw: String): List<MatchPair> {
        val arr = extractJsonArray(raw) ?: return emptyList()
        val seenEn = mutableSetOf<String>()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val en = o.getString("en").trim().lowercase()
                require(en.isNotEmpty() && seenEn.add(en)) { "duplicate or empty en: $en" }
                MatchPair(
                    topic = o.optString("topic", "world").trim().lowercase().ifEmpty { "world" },
                    gu = o.getString("gu").trim().also { require(it.isNotEmpty()) },
                    en = en,
                    icon = o.optString("icon", "✨").trim().ifEmpty { "✨" },
                )
            }.getOrNull()
        }
    }

    companion object {
        private const val TAG = "ContentGen"

        // Mastery buckets for QuizQuestion.topic — kept narrow because
        // TeacherMode renders a row per bucket. Anything else maps to "facts".
        private val MASTERY_BUCKETS = setOf("animals", "fruits", "classroom", "numbers", "facts", "geography")

        // Wide subject pool — the model picks 8-10 random items from this each
        // call as a "topics for inspiration" seed. Spans the actual world
        // instead of the old 3-bucket cage.
        private val QUIZ_SUBJECT_POOL = listOf(
            // animals — many sub-categories so we don't keep getting peacock/lion/tiger
            "wild African animals", "Indian wildlife", "birds of South Asia", "songbirds",
            "ocean fish", "sharks and whales", "freshwater fish", "reptiles", "amphibians",
            "insects", "spiders and bugs", "dinosaurs", "extinct animals",
            "cats and kittens", "dogs and puppies", "horses", "cattle and buffalo",
            // fruits / food — beyond the obvious
            "Indian fruits", "tropical fruits", "berries", "citrus fruits", "vegetables",
            "Indian sweets", "spices and herbs", "drinks", "grains and pulses",
            // body / health
            "parts of the body", "the five senses", "skeleton and bones", "teeth",
            "muscles", "blood and heart", "food groups", "vitamins",
            // nature / weather / earth
            "monsoon and rain", "trees of India", "flowers", "rivers of India", "mountain ranges",
            "deserts", "forests", "weather and seasons", "natural disasters",
            // space
            "the sun and planets", "the Moon", "stars and galaxies", "rockets and astronauts",
            // geography
            "Indian states", "world continents", "famous cities of India", "world capitals",
            "famous monuments of India", "rivers of the world", "world mountains",
            // numbers / math (vary beyond 'days in a week')
            "counting in Gujarati 1–20", "shapes — triangle, square, circle", "calendar months",
            "clock hours", "measuring units (cm, kg, litre)", "simple arithmetic facts",
            "even and odd numbers", "place value",
            // vehicles / transport
            "land vehicles — cars, trucks, trains", "boats and ships", "aeroplanes and helicopters",
            "Indian Railways", "auto-rickshaws and scooters",
            // sports / games
            "cricket", "kabaddi", "football", "Indian traditional games",
            // music / culture / festivals
            "Indian musical instruments — tabla, sitar", "world musical instruments",
            "festivals of India — Diwali, Holi, Navratri, Eid, Christmas",
            "Indian classical dance",
            // simple science
            "states of matter", "magnets", "simple machines — lever, pulley", "electricity basics",
            "water cycle", "day and night", "shadows", "the rainbow",
            // professions / community helpers
            "people who help us — doctor, farmer, teacher, postman",
            // colors / shapes
            "primary colors", "warm and cool colors", "2D shapes", "3D shapes",
            // history / heritage
            "Indian freedom heroes — Gandhi, Bhagat Singh", "ancient India",
            "great Indian inventions — zero, yoga", "great scientists — Kalam, Curie, Einstein",
            // tools & household
            "kitchen items", "tools — hammer, saw, drill", "household electronics",
            "school stationery", "garden tools",
            // sea / sky / earth life
            "underwater life", "polar animals", "rainforest creatures", "farm life",
        )

        // Match pool — kid-friendly nouns suitable for one-emoji vocab pairs.
        private val MATCH_SUBJECT_POOL = listOf(
            "wild animals", "farm animals", "birds", "fish and sea creatures", "insects",
            "reptiles", "dinosaurs",
            "Indian fruits", "tropical fruits", "berries", "vegetables",
            "school items", "stationery",
            "body parts",
            "clothes", "footwear", "headwear",
            "land vehicles", "water vehicles", "air vehicles",
            "weather words", "seasons",
            "household items", "kitchen items",
            "musical instruments — Indian and world",
            "tools — carpenter, mechanic",
            "trees and plants", "flowers",
            "shapes — 2D and 3D",
            "numbers 1–20", "colors",
            "professions",
            "festivals of India",
            "sports and games",
            "drinks and snacks",
            "fruits' shapes", "spice rack",
            "gym and exercise",
        )

        // Teaching technique pool — TeacherMode rotates through these so the
        // recommendation isn't the same "drill X" boilerplate every refresh.
        private val TEACHING_TECHNIQUE_POOL = listOf(
            "flashcards with real objects",
            "drawing and labelling on paper",
            "a 5-minute song or rhyme",
            "peer teaching — pair with a stronger student",
            "story-based learning around a familiar character",
            "a hands-on outdoor activity in the schoolyard",
            "movement-based — clap, jump, point",
            "sorting and matching games with small cards",
            "a 'spot the X' game during morning assembly",
            "drawing a mind-map together",
            "making a poster for the classroom wall",
            "a quick verbal quiz with rapid-fire answers",
        )

        /* ─── System prompts (no anchor content — only structural format) ─── */

        private val QUIZ_SYSTEM_PROMPT = """
            You are GyanSetu's quiz writer. Generate JSON quiz questions for primary-school
            children (Std 1–5, ages 6–10) in rural Gujarat.

            Output: a JSON array. NOTHING else — no prose, no markdown fences.

            Each question object MUST contain:
            - "emoji": one emoji visually representing the question
            - "topic": ONE of {animals, fruits, classroom, numbers, facts, geography}.
              For any subject outside those buckets, use "facts".
            - "gu": the question text in simple Gujarati script
            - "en": the same question in simple English (≤ 10 words)
            - "options": exactly 4 objects, each with "gu", "en", "icon" (one emoji),
              and "correct" (boolean). Exactly ONE option must have "correct": true.

            HARD RULES:
            1. The world is huge. Generate questions about ANYTHING age-appropriate —
               vehicles, space, weather, dinosaurs, oceans, sports, music, history,
               professions, body, plants, simple science. Don't keep returning the
               same handful of "national bird / national animal / capital" questions.
            2. Within ONE batch, each question must be on a DIFFERENT subject.
            3. Vocabulary suitable for ages 6–10. Sentences ≤ 10 words.
            4. Exactly ONE correct option; the other three plausible but clearly wrong.
            5. NEVER include violent, scary, religiously-divisive, or political content.
            6. "gu" must be Gujarati script (no transliteration).
            7. Vary emoji and icons — don't reuse the same emoji across options.

            Format example (this is just the SHAPE — your CONTENT must be different):
            [{"emoji":"<emoji>","topic":"facts","gu":"<Gujarati>","en":"<English>","options":[{"gu":"<g1>","en":"<e1>","icon":"<i1>","correct":false},{"gu":"<g2>","en":"<e2>","icon":"<i2>","correct":true},{"gu":"<g3>","en":"<e3>","icon":"<i3>","correct":false},{"gu":"<g4>","en":"<e4>","icon":"<i4>","correct":false}]}]
        """.trimIndent()

        private val MATCH_SYSTEM_PROMPT = """
            You are GyanSetu's vocabulary teacher. Generate JSON pairs for a match-the-pair
            game for primary-school children (ages 6–10) in rural Gujarat.

            Output: a JSON array. NOTHING else — no prose, no markdown fences.

            Each object MUST contain:
            - "topic": a short lowercase label for the category (free-form — animals,
              vehicles, weather, body, kitchen, etc.)
            - "gu": the word in Gujarati script
            - "en": the word in lowercase English — ONE word only
            - "icon": ONE emoji that visually represents the word

            HARD RULES:
            1. The world is full of things to teach. Mix wildly across categories
               within ONE batch — animals, vehicles, weather, food, body, clothes,
               instruments, sports, household, plants, professions, tools, etc.
               Do NOT return all-animals or all-fruits batches.
            2. Each "en" must be UNIQUE within the array.
            3. Common, simple words a 6-year-old would know.
            4. "gu" must be Gujarati script (no transliteration).
            5. "icon" must be a single emoji directly representing the word.
            6. NO violent, scary, or culturally-insensitive items.

            Format example (SHAPE only — your CONTENT must be different):
            [{"topic":"<cat>","gu":"<gu>","en":"<en>","icon":"<emoji>"}]
        """.trimIndent()

        private val ASK_SUGGESTIONS_SYSTEM_PROMPT = """
            You are GyanSetu's curiosity coach. Generate fun, simple "starter questions"
            a 6–10-year-old in rural Gujarat might ask their AI tutor.

            Output: a JSON array of objects {"gu": "...", "en": "..."}.
            NOTHING else — no prose, no markdown fences.

            HARD RULES:
            1. Each "en" is a SINGLE short question, max 8 words, simple vocabulary.
            2. "gu" is the same question in Gujarati script (not transliteration).
            3. Mix WIDELY across categories — animals, weather, body, food, space,
               nature, vehicles, sports, dinosaurs, plants, insects, oceans, machines,
               history, geography, festivals, music. Don't keep returning sky/sun/rainbow.
            4. Spark curiosity — prefer "why / how / what / when".
            5. Avoid scary, violent, religiously-divisive, or political topics.

            Format example (SHAPE only — different content each call):
            [{"gu":"<g1>","en":"<e1>"},{"gu":"<g2>","en":"<e2>"},{"gu":"<g3>","en":"<e3>"}]
        """.trimIndent()

        private val ENCOURAGEMENT_SYSTEM_PROMPT = """
            You are GyanSetu's cheerful tutor for primary-school children (ages 6–10) in
            rural Gujarat. The child just finished a learning game.

            Output: a single JSON object {"gu":"<short Gujarati>", "en":"<short English>"}.
            NOTHING else — no prose, no markdown fences.

            HARD RULES:
            1. ONE short sentence per language, max 6 words each.
            2. Warm, encouraging, age-appropriate.
            3. Use a FRESH metaphor each time — never reuse "well done", "good job",
               "paid off", "hard work", "shabash", "wonderful". Reach for unusual,
               vivid, kid-imagery: "your brain is a rocket!", "you climbed the
               mountain!", "stars are dancing for you!", etc.
            4. "gu" must be Gujarati script.
            5. Tone matches the score band: jubilant for "perfect", warm for "good",
               gentle and never discouraging for "low".

            Format example (SHAPE only):
            {"gu":"<Gujarati line>","en":"<English line>"}
        """.trimIndent()

        private val DAILY_TIP_SYSTEM_PROMPT = """
            You are GyanSetu's daily curiosity coach for primary-school children
            (ages 6–10) in rural Gujarat. Generate ONE short fun-fact.

            Output: a single JSON object {"gu":"<Gujarati>", "en":"<English>"}.
            NOTHING else — no prose, no markdown fences.

            HARD RULES:
            1. Max 8 words per language.
            2. SURPRISING, playful — about anywhere in the world. Animals, space,
               weather, food, body, plants, oceans, machines, history. Don't keep
               returning the same handful of facts.
            3. Avoid scary, religious-divisive, or political topics.
            4. "gu" must be Gujarati script.

            Format example (SHAPE only — different content each call):
            {"gu":"<Gujarati>","en":"<English>"}
        """.trimIndent()

        private val TEACHER_REC_SYSTEM_PROMPT = """
            You are GyanSetu's teaching assistant. A teacher needs a quick, concrete
            recommendation for a primary-school student's next lesson.

            Output: a single JSON object {"text":"<recommendation in English>"}.
            NOTHING else — no prose, no markdown fences.

            HARD RULES:
            1. 2–3 short sentences, English only.
            2. Concrete activity tied to the weakest topic — use the technique style
               the user requests, but adapt it specifically.
            3. Match a 6–10 year old's attention span (≤ 15 minutes).
            4. Professional, warm tone. Use the student's name once.
        """.trimIndent()

        private val ITEM_EXPLANATION_SYSTEM_PROMPT = """
            You are GyanSetu, a warm AI tutor for primary-school children (ages 6–10) in
            rural Gujarat. The user gives you a vocabulary item; you explain it in a
            fresh, kid-friendly way.

            Output format (plain text, no JSON, no markdown):
            English explanation on the first line.
            Gujarati explanation on the next line (Gujarati script).
            One "Fun fact:" line in English.

            HARD RULES:
            1. Total length: 3 short lines as described above.
            2. Vocabulary suitable for ages 6–10. Keep sentences short.
            3. Vary your phrasing each call — kids will revisit this often.
            4. Do NOT include the original word as a heading; jump straight to the
               explanation.
            5. NEVER include scary, violent, religious-divisive, or political content.
        """.trimIndent()

        /* ─── Static fallbacks ──────────────────────────────────────────── */

        val staticEncouragements = mapOf(
            "perfect" to Encouragement("સંપૂર્ણ ગુણ!", "Perfect score!"),
            "good"    to Encouragement("શાબાશ!", "Well done!"),
            "low"     to Encouragement("સારો પ્રયત્ન!", "Good try!"),
        )

        val staticDailyTips: List<DailyTip> = listOf(
            DailyTip("તું જાણે છે? હાથી તરી શકે છે!", "Did you know? Elephants can swim!"),
            DailyTip("મધમાખી ૫ માઇલ સુધી ઊડે છે.", "Bees can fly up to 5 miles."),
            DailyTip("મગજ રાત્રે પણ કામ કરે છે.", "Your brain works even when you sleep."),
            DailyTip("ઑક્ટોપસને ત્રણ હૃદય હોય છે!", "An octopus has three hearts!"),
            DailyTip("શાહમૃગની આંખ તેના મગજ કરતા મોટી છે.", "An ostrich's eye is bigger than its brain."),
            DailyTip("ચંદ્ર પૃથ્વીથી દૂર જઈ રહ્યો છે.", "The Moon drifts away from Earth each year."),
        )

        val staticAskSuggestions: List<AskSuggestion> = listOf(
            AskSuggestion("આકાશ વાદળી કેમ છે?", "Why is the sky blue?"),
            AskSuggestion("સૂર્ય શું છે?", "What is the sun?"),
            AskSuggestion("મેઘધનુષ કેવી રીતે બને?", "How does a rainbow form?"),
            AskSuggestion("ડાયનાસોર કેમ લુપ્ત થયા?", "Why did dinosaurs go extinct?"),
            AskSuggestion("ચંદ્ર પર શું છે?", "What is on the Moon?"),
        )

        // Bigger fallback quiz pool — used only when the model is unavailable
        // or returned too few valid items. Spans many subjects so even the
        // fallback isn't repetitive.
        val staticQuizPool: List<QuizQuestion> = listOf(
            QuizQuestion("🇮🇳", "ભારતનું રાષ્ટ્રીય પક્ષી કયું છે?", "Which is the national bird of India?", "animals", listOf(
                QuizOption("કાગડો", "Crow", "🐦", false),
                QuizOption("મોર", "Peacock", "🦚", true),
                QuizOption("પોપટ", "Parrot", "🦜", false),
                QuizOption("ગરુડ", "Eagle", "🦅", false),
            )),
            QuizQuestion("🌍", "ગુજરાતનું પાટનગર શું છે?", "What is the capital of Gujarat?", "geography", listOf(
                QuizOption("સુરત", "Surat", "🏙️", false),
                QuizOption("વડોદરા", "Vadodara", "🏛️", false),
                QuizOption("ગાંધીનગર", "Gandhinagar", "🏢", true),
                QuizOption("અમદાવાદ", "Ahmedabad", "🌆", false),
            )),
            QuizQuestion("📅", "એક અઠવાડિયામાં કેટલા દિવસ હોય?", "How many days are in a week?", "numbers", listOf(
                QuizOption("પાંચ", "Five", "5️⃣", false),
                QuizOption("છ", "Six", "6️⃣", false),
                QuizOption("સાત", "Seven", "7️⃣", true),
                QuizOption("આઠ", "Eight", "8️⃣", false),
            )),
            QuizQuestion("🐅", "ભારતનું રાષ્ટ્રીય પ્રાણી કયું છે?", "What is the national animal of India?", "animals", listOf(
                QuizOption("સિંહ", "Lion", "🦁", false),
                QuizOption("વાઘ", "Tiger", "🐅", true),
                QuizOption("હાથી", "Elephant", "🐘", false),
                QuizOption("ગાય", "Cow", "🐄", false),
            )),
            QuizQuestion("🌈", "મેઘધનુષમાં કેટલા રંગ હોય છે?", "How many colours are in a rainbow?", "facts", listOf(
                QuizOption("પાંચ", "Five", "5️⃣", false),
                QuizOption("છ", "Six", "6️⃣", false),
                QuizOption("સાત", "Seven", "7️⃣", true),
                QuizOption("આઠ", "Eight", "8️⃣", false),
            )),
            QuizQuestion("🥭", "ફળોનો રાજા કયું ફળ છે?", "Which fruit is the king of fruits?", "fruits", listOf(
                QuizOption("સફરજન", "Apple", "🍎", false),
                QuizOption("કેળું", "Banana", "🍌", false),
                QuizOption("કેરી", "Mango", "🥭", true),
                QuizOption("દ્રાક્ષ", "Grape", "🍇", false),
            )),
            QuizQuestion("🐄", "ગાય આપણને શું આપે છે?", "What does a cow give us?", "animals", listOf(
                QuizOption("ઊન", "Wool", "🧶", false),
                QuizOption("દૂધ", "Milk", "🥛", true),
                QuizOption("મધ", "Honey", "🍯", false),
                QuizOption("ઈંડું", "Egg", "🥚", false),
            )),
            QuizQuestion("☀️", "સૂર્ય શું છે?", "What is the sun?", "facts", listOf(
                QuizOption("ગ્રહ", "Planet", "🪐", false),
                QuizOption("તારો", "Star", "⭐", true),
                QuizOption("ઉપગ્રહ", "Satellite", "🛰️", false),
                QuizOption("ધૂમકેતુ", "Comet", "☄️", false),
            )),
            QuizQuestion("🟧", "ભારતના ધ્વજમાં ઉપરનો રંગ કયો છે?", "Which is the top colour in the Indian flag?", "geography", listOf(
                QuizOption("લીલો", "Green", "🟢", false),
                QuizOption("સફેદ", "White", "⚪", false),
                QuizOption("કેસરી", "Saffron", "🟠", true),
                QuizOption("વાદળી", "Blue", "🔵", false),
            )),
            QuizQuestion("🦚", "મોર શું બોલે છે?", "What sound does a peacock make?", "animals", listOf(
                QuizOption("મ્યાઉં", "Meow", "🐈", false),
                QuizOption("ભૌ ભૌ", "Woof", "🐕", false),
                QuizOption("કેકા", "Keka", "🦚", true),
                QuizOption("ગર્જ", "Roar", "🦁", false),
            )),
            QuizQuestion("🚂", "ભારતની પ્રથમ ટ્રેન ક્યારે ચાલી?", "When did India's first train run?", "facts", listOf(
                QuizOption("૧૭૫૩", "1753", "📜", false),
                QuizOption("૧૮૫૩", "1853", "🚂", true),
                QuizOption("૧૯૫૩", "1953", "📅", false),
                QuizOption("૨૦૫૩", "2053", "🔮", false),
            )),
            QuizQuestion("🪐", "આપણા સૌરમંડળમાં કેટલા ગ્રહો છે?", "How many planets are in our solar system?", "numbers", listOf(
                QuizOption("૭", "Seven", "7️⃣", false),
                QuizOption("૮", "Eight", "8️⃣", true),
                QuizOption("૯", "Nine", "9️⃣", false),
                QuizOption("૧૦", "Ten", "🔟", false),
            )),
            QuizQuestion("🐘", "હાથી શું ખાય છે?", "What does an elephant eat?", "animals", listOf(
                QuizOption("માંસ", "Meat", "🍖", false),
                QuizOption("પાંદડા અને ઘાસ", "Leaves and grass", "🌿", true),
                QuizOption("માછલી", "Fish", "🐟", false),
                QuizOption("મધ", "Honey", "🍯", false),
            )),
            QuizQuestion("⛅", "વાદળમાં શું હોય છે?", "What are clouds made of?", "facts", listOf(
                QuizOption("કપાસ", "Cotton", "☁️", false),
                QuizOption("પાણીના ટીપા", "Water droplets", "💧", true),
                QuizOption("ધુમાડો", "Smoke", "💨", false),
                QuizOption("ધૂળ", "Dust", "🌫️", false),
            )),
            QuizQuestion("🦷", "પુખ્ત માણસને કેટલા દાંત હોય છે?", "How many teeth does an adult have?", "numbers", listOf(
                QuizOption("૨૦", "20", "🔢", false),
                QuizOption("૨૮", "28", "🔢", false),
                QuizOption("૩૨", "32", "🦷", true),
                QuizOption("૪૦", "40", "🔢", false),
            )),
        )
    }
}
