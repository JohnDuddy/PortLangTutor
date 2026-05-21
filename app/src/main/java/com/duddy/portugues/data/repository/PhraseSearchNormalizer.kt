package com.duddy.portugues.data.repository

import java.text.Normalizer

object PhraseSearchNormalizer {
    fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .trim()
}
