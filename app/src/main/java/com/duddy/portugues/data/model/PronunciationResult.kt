package com.duddy.portugues.data.model

/**
 * Azure Pronunciation Assessment scores returned by the backend.
 * All scores are 0–100. Null means Azure didn't report that dimension.
 */
data class PronunciationResult(
    val overall:        PronunciationScores,
    val words:          List<WordAssessment>,
    val phonemesLow:    List<PhonemeScore>,
    val recognizedText: String,
    val referenceText:  String,
)

data class PronunciationScores(
    val pronunciation: Double?,
    val accuracy:      Double?,
    val fluency:       Double?,
    val completeness:  Double?,
    val prosody:       Double?,
)

data class WordAssessment(
    val word:      String,
    val accuracy:  Double?,
    val errorType: PhonemeErrorType,
    val phonemes:  List<PhonemeScore>,
)

data class PhonemeScore(
    val phoneme: String,
    val score:   Double?,
)

enum class PhonemeErrorType {
    None, Mispronunciation, Omission, Insertion,
    UnexpectedBreak, MissingBreak, Monotone, Unknown;

    companion object {
        fun parse(raw: String?): PhonemeErrorType = when (raw) {
            "None"             -> None
            "Mispronunciation" -> Mispronunciation
            "Omission"         -> Omission
            "Insertion"        -> Insertion
            "UnexpectedBreak"  -> UnexpectedBreak
            "MissingBreak"     -> MissingBreak
            "Monotone"         -> Monotone
            else               -> Unknown
        }
    }
}
