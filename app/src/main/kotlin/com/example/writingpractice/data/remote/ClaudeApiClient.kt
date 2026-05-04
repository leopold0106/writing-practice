package com.example.writingpractice.data.remote

import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.GradingResult
import com.example.writingpractice.data.remote.dto.ClaudeMessage
import com.example.writingpractice.data.remote.dto.ClaudeRequest
import com.example.writingpractice.data.remote.dto.GeneratedProblemDto
import com.example.writingpractice.data.remote.dto.GradingResultDto
import kotlinx.serialization.json.Json
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
Generate a Korean text for English translation practice at the requested level.
Return ONLY valid JSON (no markdown, no extra text):
{
  "korean_text": "<the Korean text to translate>",
  "reference_answer": "<a natural English translation>",
  "topic_tag": "<one of: 일상생활, 여행, 비즈니스, 환경, 기술, 감정, 음식, 문화>"
}
""".trimIndent()

    suspend fun gradeAnswer(
        koreanText: String,
        englishAnswer: String
    ): Result<GradingResult> = runCatching {
        val userContent = "Korean original:\n$koreanText\n\nStudent's English answer:\n$englishAnswer"
        val response = service.complete(
            ClaudeRequest(
                system = gradingSystemPrompt,
                messages = listOf(ClaudeMessage("user", userContent))
            )
        )
        val raw = extractJson(response.content.first().text)
        val dto = json.decodeFromString<GradingResultDto>(raw)
        dto.toDomain()
    }

    suspend fun generateProblem(level: Int): Result<GeneratedProblem> = runCatching {
        val levelDesc = when (level) {
            1 -> "a single simple Korean sentence"
            2 -> "two related Korean sentences on the same topic"
            3 -> "three related Korean sentences forming a short paragraph"
            else -> "a short Korean paragraph of 4-6 sentences"
        }
        val response = service.complete(
            ClaudeRequest(
                system = generateSystemPrompt,
                messages = listOf(
                    ClaudeMessage(
                        "user",
                        "Generate $levelDesc for English translation practice. Make it natural, practical, and from a varied topic."
                    )
                )
            )
        )
        val raw = extractJson(response.content.first().text)
        val dto = json.decodeFromString<GeneratedProblemDto>(raw)
        GeneratedProblem(dto.koreanText, dto.referenceAnswer, dto.topicTag)
    }

    suspend fun ping(): Result<Unit> = runCatching {
        service.complete(
            ClaudeRequest(
                maxTokens = 1,
                messages = listOf(ClaudeMessage("user", "Hi"))
            )
        )
        Unit
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
