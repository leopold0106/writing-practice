package com.example.writingpractice.data.remote

import com.example.writingpractice.data.remote.dto.ClaudeResponse
import com.example.writingpractice.data.remote.dto.ContentBlock
import com.example.writingpractice.data.remote.dto.GradingResultDto
import com.example.writingpractice.data.remote.dto.Usage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the grading-response parsing that used to strand answers at PENDING
 * ("채점중") forever: any parse failure was retried indefinitely and never recorded.
 */
class GradingResponseParsingTest {

    private fun decode(raw: String) =
        apiJson.decodeFromString<GradingResultDto>(extractJson(raw))

    @Test
    fun `perfect answer with corrections key omitted parses`() {
        // What the model actually returns for a short, correct sentence.
        val dto = decode("""{"score": 100, "overall_feedback": "완벽합니다"}""")

        assertEquals(100, dto.score)
        assertEquals("완벽합니다", dto.overallFeedback)
        assertTrue(dto.corrections.isEmpty())
        assertEquals("", dto.finalCorrectedVersion)
    }

    @Test
    fun `explicit nulls are coerced to defaults`() {
        val dto = decode(
            """{"score": 100, "overall_feedback": null,
               "corrections": null, "final_corrected_version": null}"""
        )

        assertEquals(100, dto.score)
        assertEquals("", dto.overallFeedback)
        assertTrue(dto.corrections.isEmpty())
    }

    @Test
    fun `score remains required`() {
        assertThrows(Exception::class.java) {
            decode("""{"overall_feedback": "좋아요"}""")
        }
    }

    @Test
    fun `markdown fenced json parses`() {
        val dto = decode(
            """
            ```json
            {"score": 80, "overall_feedback": "괜찮습니다", "corrections": [],
             "final_corrected_version": "I am looking for a new job."}
            ```
            """.trimIndent()
        )

        assertEquals(80, dto.score)
        assertEquals("I am looking for a new job.", dto.finalCorrectedVersion)
    }

    @Test
    fun `prose around the json body is discarded`() {
        val dto = decode(
            "Here is the grading result:\n" +
                """{"score": 70, "overall_feedback": "시제를 확인하세요"}""" +
                "\nLet me know if you need more detail."
        )

        assertEquals(70, dto.score)
    }

    @Test
    fun `corrections with missing fields still parse`() {
        val dto = decode(
            """{"score": 60, "overall_feedback": "문법 오류",
               "corrections": [{"original_sentence": "I am look for a job"}]}"""
        )

        assertEquals(1, dto.corrections.size)
        assertEquals("I am look for a job", dto.corrections.first().originalSentence)
        assertEquals("GRAMMAR", dto.corrections.first().errorType)
    }

    @Test
    fun `text blocks are concatenated and non-text blocks skipped`() {
        val response = ClaudeResponse(
            id = "msg_1",
            content = listOf(
                ContentBlock(type = "thinking", text = "ignore me"),
                ContentBlock(type = "text", text = """{"score": 90,"""),
                ContentBlock(type = "text", text = """ "overall_feedback": "좋습니다"}""")
            ),
            usage = Usage(inputTokens = 1, outputTokens = 1)
        )

        assertEquals(90, decode(textOf(response)).score)
    }

    @Test
    fun `empty content is a retryable failure rather than a crash`() {
        val response = ClaudeResponse(
            id = "msg_2",
            content = emptyList(),
            usage = Usage(inputTokens = 1, outputTokens = 0)
        )

        val failure = assertThrows(ApiFailure::class.java) { textOf(response) }
        assertTrue(failure.retryable)
    }
}
