package com.example.writingpractice.data.remote

import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.GradingResult
import com.example.writingpractice.data.remote.dto.ClaudeMessage
import com.example.writingpractice.data.remote.dto.ClaudeRequest
import com.example.writingpractice.data.remote.dto.GeneratedProblemDto
import com.example.writingpractice.data.remote.dto.GradingResultDto
import com.example.writingpractice.data.remote.dto.WeaknessAnalysisDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedProblem(
    val koreanText: String,
    val referenceAnswer: String,
    val topicTag: String
)

data class WeaknessAnalysisInput(
    val periodLabel: String,
    val totalCorrections: Int,
    val avgScore: Int?,
    val errorCountsByType: Map<String, Int>,
    val sampleCorrections: List<SampleCorrection>
)

data class SampleCorrection(
    val original: String,
    val corrected: String,
    val explanation: String,
    val errorType: String
)

@Singleton
class ClaudeApiClient @Inject constructor(
    private val service: ClaudeApiService,
    private val json: Json
) {
    private val gradingSystemPrompt = """
You are an English writing teacher grading Korean students' English translations.
Given a Korean sentence/paragraph and the student's English answer, analyze the answer carefully.
Return ONLY valid JSON with this exact structure (no markdown, no extra text):
{
  "score": <integer 0-100>,
  "overall_feedback": "<one concise sentence in Korean>",
  "corrections": [
    {
      "original_sentence": "<the student's sentence that contains an error>",
      "corrected_sentence": "<the corrected version>",
      "explanation": "<explanation in Korean of why it is wrong and how to fix it>",
      "error_type": "<one of: GRAMMAR, VOCABULARY, STRUCTURE, PUNCTUATION, SPELLING>"
    }
  ],
  "final_corrected_version": "<the complete corrected English translation>"
}
If the answer is perfect, return an empty corrections array and a score of 100.
Only report genuine errors. Do not invent corrections for acceptable variations in phrasing.
""".trimIndent()

    private val weaknessSystemPrompt = """
You are an English-writing tutor analyzing the recurring mistakes of a Korean student who translates Korean sentences into English.
You will receive aggregate error counts by type and a sample of recent corrections (each with original, corrected, explanation, error type).

Identify recurring linguistic patterns, not individual mistakes. Group them into 3-5 high-level "weakness points" (e.g., "subject-verb agreement after collective nouns", "preposition choice with time expressions").

Return ONLY valid JSON (no markdown, no extra text, no comments) with this exact structure:
{
  "summary": "<1-2 concise Korean sentences capturing the student's overall profile>",
  "overall_level": "<BEGINNER | INTERMEDIATE | ADVANCED>",
  "weakness_points": [
    {
      "error_type": "<GRAMMAR | VOCABULARY | STRUCTURE | PUNCTUATION | SPELLING>",
      "title": "<short Korean title, max 20 chars, e.g. '시제 일치 오류'>",
      "description": "<2-3 Korean sentences explaining the pattern and why it matters>",
      "examples": ["<원문 → 수정문 (간단한 한국어 주석)>", "..."],
      "severity": "<LOW | MEDIUM | HIGH>"
    }
  ],
  "improvement_suggestions": [
    "<one concrete actionable Korean tip>",
    "..."
  ],
  "recommended_patterns": [
    {
      "pattern": "<Korean name of sentence pattern>",
      "example_sentence": "<one English example sentence>"
    }
  ],
  "recommended_practice_level": <integer 1-7>
}

Rules:
- All user-facing strings (summary, title, description, examples, suggestions, pattern names) MUST be in Korean.
- example_sentence MUST be in English.
- weakness_points: 3-5 items, ordered by severity DESC (HIGH first).
- improvement_suggestions: 4-6 specific actionable items (not generic platitudes).
- recommended_patterns: 3-5 items.
- If the data is too sparse (fewer than 5 corrections), still produce a valid response but acknowledge the limited sample in the summary.
- Do not invent errors that aren't supported by the data.
""".trimIndent()

    private val generateSystemPrompt = """
You are a Korean language teacher creating English translation practice exercises.
Generate a batch of Korean texts for English translation practice at the requested level.
Return ONLY a valid JSON array (no markdown, no extra text) where each element has:
{
  "korean_text": "<the Korean text to translate>",
  "reference_answer": "<a natural English translation>",
  "topic_tag": "<one of: daily_life, travel, business, environment, technology, emotion, food, culture, education, science, health, politics, sports, career, law>"
}
""".trimIndent()

    suspend fun gradeAnswer(
        koreanText: String,
        englishAnswer: String
    ): Result<GradingResult> = apiCall {
        val userContent = "Korean original:\n$koreanText\n\nStudent's English answer:\n$englishAnswer"
        val response = service.complete(
            ClaudeRequest(
                model = MODEL,
                maxTokens = 1024,
                system = gradingSystemPrompt,
                messages = listOf(ClaudeMessage("user", userContent))
            )
        )
        val raw = extractJson(response.content.first().text)
        val dto = json.decodeFromString<GradingResultDto>(raw)
        dto.toDomain()
    }

    suspend fun generateProblems(level: Int, weaknesses: List<String> = emptyList()): Result<List<GeneratedProblem>> = apiCall {
        val levelDesc = when (level) {
            1 -> "a single short, simple Korean sentence (subject-verb-object, everyday vocabulary)"
            2 -> "a single Korean sentence with adverbs or modifying phrases"
            3 -> "2-3 Korean sentences connected by conjunctions (하지만, 그래서, 그런데)"
            4 -> "a single longer Korean sentence with a subordinate clause (because/although/when structure)"
            5 -> "a sophisticated Korean sentence with formal grammar and complex structure (에도 불구하고, ~한 덕분에, etc.)"
            6 -> "2-3 difficult Korean sentences with logical flow between them"
            else -> "an academic-style Korean paragraph of 4-7 sentences in formal register"
        }
        val weaknessHint = if (weaknesses.isNotEmpty()) {
            val types = weaknesses.joinToString(", ")
            "\n6 of the problems should specifically target improving these error types the student struggles with: $types. The remaining 4 should cover diverse new patterns and topics."
        } else {
            "\nCover a wide variety of topics and sentence patterns."
        }
        val topics = "daily_life, travel, business, environment, technology, emotion, food, culture, education, science, health, politics, sports, career, law"
        val userMessage = "Generate exactly 10 Korean sentences at this level: $levelDesc.$weaknessHint Use diverse topics from: $topics. Return a JSON array of exactly 10 objects."
        val response = service.complete(
            ClaudeRequest(
                model = MODEL,
                maxTokens = 4096,
                system = generateSystemPrompt,
                messages = listOf(ClaudeMessage("user", userMessage))
            )
        )
        val raw = extractJson(response.content.first().text)
        json.decodeFromString<List<GeneratedProblemDto>>(raw).map { dto ->
            GeneratedProblem(dto.koreanText, dto.referenceAnswer, dto.topicTag)
        }
    }

    suspend fun analyzeWeaknesses(input: WeaknessAnalysisInput): Result<WeaknessAnalysisDto> = apiCall {
        val userMessage = buildWeaknessUserMessage(input)
        val response = service.complete(
            ClaudeRequest(
                model = MODEL,
                maxTokens = 2048,
                system = weaknessSystemPrompt,
                messages = listOf(ClaudeMessage("user", userMessage))
            )
        )
        val raw = extractJson(response.content.first().text)
        json.decodeFromString<WeaknessAnalysisDto>(raw)
    }

    private fun buildWeaknessUserMessage(input: WeaknessAnalysisInput): String {
        val countsLine = input.errorCountsByType.entries
            .joinToString(", ") { "${it.key}=${it.value}" }
        val avgLine = input.avgScore?.let { "Average score: ${it}\n" } ?: ""
        val sampleLines = input.sampleCorrections.mapIndexed { i, c ->
            val orig = c.original.take(200)
            val corr = c.corrected.take(200)
            val expl = c.explanation.take(200)
            "${i + 1}. [${c.errorType}]\n   Original: \"$orig\"\n   Corrected: \"$corr\"\n   Explanation: \"$expl\""
        }.joinToString("\n")
        return """
Period: ${input.periodLabel}
Total corrections: ${input.totalCorrections}
${avgLine}Error counts by type: $countsLine

Sample corrections (most recent ${input.sampleCorrections.size}):
$sampleLines

Analyze these patterns and return the weakness analysis JSON.
""".trimIndent()
    }

    suspend fun ping(): Result<Unit> = apiCall {
        service.complete(
            ClaudeRequest(
                model = MODEL,
                maxTokens = 10,
                messages = listOf(ClaudeMessage("user", "Hi"))
            )
        )
        Unit
    }

    companion object {
        private const val MODEL = "claude-sonnet-4-6"
    }

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
        val message = if (body != null) {
            try {
                (json.parseToJsonElement(body) as? JsonObject)
                    ?.get("error")?.jsonObject
                    ?.get("message")
                    ?.let { (it as? JsonPrimitive)?.content }
                    ?: body
            } catch (_: Exception) { body }
        } else {
            "HTTP ${e.code()}"
        }
        Result.failure(RuntimeException(message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun extractJson(text: String): String =
        text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

    private fun GradingResultDto.toDomain() = GradingResult(
        score = score,
        overallFeedback = overallFeedback,
        corrections = corrections.map { c ->
            Correction(
                originalSentence = c.originalSentence,
                correctedSentence = c.correctedSentence,
                explanation = c.explanation,
                errorType = runCatching { ErrorType.valueOf(c.errorType) }.getOrDefault(ErrorType.GRAMMAR)
            )
        },
        finalCorrectedVersion = finalCorrectedVersion
    )
}
