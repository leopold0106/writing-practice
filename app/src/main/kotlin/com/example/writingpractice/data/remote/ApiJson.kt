package com.example.writingpractice.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * JSON configuration used for every Claude API response.
 *
 * Deliberately forgiving: the model occasionally omits an optional key or sends an explicit null
 * where a list is expected. Failing to parse those responses used to leave an answer stuck at
 * PENDING ("채점중") forever, so anything the DTOs can default is defaulted rather than thrown on.
 */
@OptIn(ExperimentalSerializationApi::class)
val apiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
    // An explicit null for a field that has a default (e.g. "corrections": null) falls back to
    // the default instead of throwing.
    coerceInputValues = true
}
