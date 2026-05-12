package com.gyansetu.data

/* Shared content shapes for the GK Quiz and Match Game. These used to be private
 * inside the screens (with hard-coded pools); now the model generates them at
 * runtime via [com.gyansetu.ai.ContentGenerator] and the screens consume the
 * results from [com.gyansetu.viewmodel.AppViewModel]. */

data class QuizOption(
    val gu: String,
    val en: String,
    val icon: String,
    val correct: Boolean,
)

data class QuizQuestion(
    val emoji: String,
    val gu: String,
    val en: String,
    /** One of: animals, fruits, classroom, numbers, facts, geography.
     *  Drives the per-topic mastery matrix that Teacher Mode renders. */
    val topic: String,
    val options: List<QuizOption>,
)

data class MatchPair(
    val topic: String,
    val gu: String,
    val en: String,
    val icon: String,
)

/** A starter prompt rendered on the empty Ask screen. Tap → asks Gemma. */
data class AskSuggestion(
    val gu: String,
    val en: String,
)

/** Bilingual celebration message shown on ResultsScreen. */
data class Encouragement(
    val gu: String,
    val en: String,
)

/** Bilingual fun-fact / curiosity prompt rendered on HomeScreen. */
data class DailyTip(
    val gu: String,
    val en: String,
)

/** Teacher Mode AI Recommendation — English-only paragraph. */
data class TeacherRec(
    val text: String,
)

/** Tri-state for content that the model produces lazily: Loading on first
 *  generation, Ready once we have model output, Failed when the model errored
 *  or returned unparseable JSON — Failed always carries a usable [fallback]
 *  list so the UI can render something instead of an error. */
sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data class Ready<T>(val data: T) : ContentState<T>
    data class Failed<T>(val reason: String, val fallback: T) : ContentState<T>
}
