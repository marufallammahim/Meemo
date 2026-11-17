package com.meemo.core.common

fun String.normalizedWhitespace(): String = trim().replace(Regex("\\s+"), " ")

fun String.stripOuterPunctuation(): String = trim().trim(',', '.', '!', '?', ':', ';', '।')
