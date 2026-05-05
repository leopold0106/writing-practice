package com.example.writingpractice.data.remote

import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.GradingResult
import com.example.writingpractice.data.remote.dto.ClaudeMessage
import com.example.writingpractice.data.remote.dto.ClaudeRequest
import com.example.writingpractice.data.remote.dto.GeneratedProblemDto
import com.example.writingpractice.data.remote.dto.GradingResultDto
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
