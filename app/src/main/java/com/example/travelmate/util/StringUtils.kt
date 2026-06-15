package com.example.travelmate.util

import java.text.Normalizer
import java.util.regex.Pattern

fun String.normalizeVietnamese(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(temp).replaceAll("")
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase()
        .trim()
}
