package com.example.data.models

import androidx.compose.ui.graphics.Color

enum class ProcessingMode(val displayName: String, val description: String) {
    FAST("Fast", "Local extraction first, AI only when needed"),
    BALANCED("Balanced", "Local inspection + targeted AI verification (Recommended)"),
    ACCURATE("Accurate", "Full deep document vision OCR & verification")
}

enum class ConfidenceLevel(val label: String, val colorHex: Long) {
    HIGH("HIGH", 0xFF10B981),    // Green
    MEDIUM("MEDIUM", 0xFFF59E0B), // Yellow/Orange
    LOW("LOW", 0xFFEF4444);      // Red

    val color: Color
        get() = Color(colorHex)
}

enum class QuestionType(val displayName: String) {
    SINGLE("Single Choice"),
    MULTIPLE("Multiple Choice"),
    NUMERICAL("Numerical Value"),
    INTEGER("Integer Value"),
    MATRIX("Matrix Match"),
    ASSERTION("Assertion - Reason");

    companion object {
        fun fromString(str: String?): QuestionType {
            return when (str?.lowercase()?.trim()) {
                "single", "mcq", "single choice" -> SINGLE
                "multiple", "multiple choice" -> MULTIPLE
                "numerical", "decimal" -> NUMERICAL
                "integer" -> INTEGER
                "matrix", "match" -> MATRIX
                "assertion", "reason" -> ASSERTION
                else -> SINGLE
            }
        }
    }
}

enum class AnswerMappingStatus {
    MAPPED,
    UNCERTAIN,
    UNMAPPED
}

data class JeeQuestion(
    val id: Int,
    val stableQuestionId: String = "q-${id.toString().padStart(4, '0')}",
    val subject: String, // "Physics", "Chemistry", "Mathematics", or "Unknown"
    val type: QuestionType,
    val question: String,
    val originalText: String = question,
    val extractedText: String = question,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val sourcePage: Int = 1,
    val sourcePageStart: Int = sourcePage,
    val sourcePageEnd: Int = sourcePage,
    val extractionMethod: String = "text-layer", // "text-layer", "gemini-vision", "smart-ocr"
    val cropStatus: String = "verified", // "verified", "needs_review", "full_region"
    val cropBoundingBox: String = "X: 5%, Y: 8%, W: 90%, H: 28%",
    val textConfidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val boundaryConfidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val optionConfidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val answerMappingConfidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val mappingSource: String = "Official Key PDF",
    val hasDiagram: Boolean = false,
    val isDuplicateFlagged: Boolean = false,
    val answerMappingStatus: AnswerMappingStatus = AnswerMappingStatus.MAPPED,
    val note: String? = null
)

enum class PaletteState(val label: String, val colorHex: Long) {
    NOT_VISITED("Not Visited", 0xFFE2E8F0), // Light slate
    NOT_ANSWERED("Not Answered", 0xFFEF4444), // Red
    ANSWERED("Answered", 0xFF10B981), // Green
    MARKED_FOR_REVIEW("Marked for Review", 0xFF8B5CF6), // Purple
    ANSWERED_AND_MARKED("Answered & Marked", 0xFF0284C7); // Teal/Blue

    val color: Color
        get() = Color(colorHex)
}

data class UserQuestionState(
    val isVisited: Boolean = false,
    val isMarkedForReview: Boolean = false,
    val selectedOption: String = "", // e.g. "A" or "A,B" for multiple
    val textResponse: String = "" // for numerical/integer
) {
    fun getPaletteState(): PaletteState {
        val hasAnswer = selectedOption.isNotBlank() || textResponse.isNotBlank()
        return when {
            hasAnswer && isMarkedForReview -> PaletteState.ANSWERED_AND_MARKED
            isMarkedForReview -> PaletteState.MARKED_FOR_REVIEW
            hasAnswer -> PaletteState.ANSWERED
            isVisited -> PaletteState.NOT_ANSWERED
            else -> PaletteState.NOT_VISITED
        }
    }
}

data class PdfFileMetaData(
    val uriString: String,
    val fileName: String,
    val fileSizeFormatted: String,
    val isQuestionsPdf: Boolean
)

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val description: String,
    val isDefault: Boolean = false
)

data class JeeExamPaper(
    val title: String,
    val questions: List<JeeQuestion>,
    val totalQuestions: Int = questions.size,
    val physicsCount: Int = questions.count { it.subject.equals("Physics", ignoreCase = true) },
    val chemistryCount: Int = questions.count { it.subject.equals("Chemistry", ignoreCase = true) },
    val mathCount: Int = questions.count { it.subject.equals("Mathematics", ignoreCase = true) },
    val mappedCount: Int = questions.count { it.answerMappingStatus == AnswerMappingStatus.MAPPED },
    val uncertainCount: Int = questions.count { it.answerMappingStatus == AnswerMappingStatus.UNCERTAIN },
    val unmappedCount: Int = questions.count { it.answerMappingStatus == AnswerMappingStatus.UNMAPPED },
    val highConfidenceCount: Int = questions.count { it.textConfidence == ConfidenceLevel.HIGH },
    val mediumConfidenceCount: Int = questions.count { it.textConfidence == ConfidenceLevel.MEDIUM },
    val lowConfidenceCount: Int = questions.count { it.textConfidence == ConfidenceLevel.LOW },
    val cropVerifiedCount: Int = questions.count { it.cropStatus == "verified" },
    val cropNeedsReviewCount: Int = questions.count { it.cropStatus != "verified" },
    val durationMinutes: Int = 180
)

enum class ProcessingStage(val title: String, val progress: Float) {
    UPLOADING_QUESTIONS_PDF("Uploading Questions PDF", 0.08f),
    UPLOADING_ANSWER_KEY_PDF("Uploading Answer Key PDF", 0.16f),
    STAGE_A_LOCAL_INSPECTION("Stage A: Local PDF Layout Inspection", 0.28f),
    SMART_OCR_DETECTION("Stage A: Smart OCR & Boundary Scan", 0.42f),
    STAGE_B_GEMINI_UNDERSTANDING("Stage B: Gemini Document Understanding", 0.60f),
    EXTRACTING_BOUNDARIES("Extracting Questions & Diagram Bounds", 0.75f),
    TRANSPARENT_CROPPING("Generating Safe Transparent Bounding Crops", 0.85f),
    MAPPING_OFFICIAL_ANSWERS("Mapping Official Answer Key Transparency", 0.92f),
    VALIDATING_PAPER("Validation & Traceability Audit", 0.98f),
    BUILDING_CBT("Building CBT & Inspector", 1.00f),
    COMPLETED("Ready", 1.00f),
    FAILED("Processing Failed", 0.0f)
}

data class SubjectScore(
    val subject: String,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val score: Int, // +4 for correct, -1 for incorrect, 0 for unanswered
    val maxScore: Int,
    val accuracyPercentage: Float
)

data class ExamResult(
    val paperTitle: String,
    val totalQuestions: Int,
    val totalAttempted: Int,
    val totalCorrect: Int,
    val totalIncorrect: Int,
    val totalUnanswered: Int,
    val totalScore: Int,
    val maxPossibleScore: Int,
    val overallAccuracy: Float,
    val timeTakenSeconds: Long,
    val subjectScores: List<SubjectScore>,
    val questionResults: List<QuestionResultDetail>
)

data class QuestionResultDetail(
    val question: JeeQuestion,
    val userAnswer: String,
    val officialAnswer: String,
    val isCorrect: Boolean,
    val isUnanswered: Boolean,
    val isUncertainOfficial: Boolean,
    val marksAwarded: Int
)

