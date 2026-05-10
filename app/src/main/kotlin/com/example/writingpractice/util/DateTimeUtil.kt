package com.example.writingpractice.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayIso(): String = LocalDate.now().format(formatter)
}
