package com.example.writingpractice.data.remote

import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.GradingResult
import com.example.writingpractice.data.remote.dto.ClaudeMessage
import com.example.writingpractice.data.remote.dto.ClaudeRequest
import com.example.writingpractice.data.remote.dto.GradingResultDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

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
        val raw = response.content.first().text.trim()
        val dto = json.decodeFromString<GradingResultDto>(raw)
        dto.toDomain()
    }

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
