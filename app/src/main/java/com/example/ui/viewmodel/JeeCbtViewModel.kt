package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.models.AnswerMappingStatus
import com.example.data.models.ExamResult
import com.example.data.models.JeeExamPaper
import com.example.data.models.JeeQuestion
import com.example.data.models.PdfFileMetaData
import com.example.data.models.ProcessingStage
import com.example.data.models.QuestionResultDetail
import com.example.data.models.SubjectScore
import com.example.data.models.UserQuestionState
import com.example.data.parser.PaperProcessor
import com.example.data.pdf.PdfExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScreenState {
    SETUP,
    PROCESSING,
    VALIDATION,
    CBT_EXAM,
    RESULT_ANALYSIS
}

class JeeCbtViewModel(application: Application) : AndroidViewModel(application) {

    private val paperProcessor = PaperProcessor(application)
    private val pdfExtractor = PdfExtractor(application)
    private val geminiApiClient = GeminiApiClient()

    // Screen Navigation
    private val _screenState = MutableStateFlow(ScreenState.SETUP)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Gemini API Setup
    private val _selectedModelId = MutableStateFlow("gemini-3.5-flash")
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _processingMode = MutableStateFlow(com.example.data.models.ProcessingMode.BALANCED)
    val processingMode: StateFlow<com.example.data.models.ProcessingMode> = _processingMode.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Question Inspector State
    private val _inspectedQuestion = MutableStateFlow<JeeQuestion?>(null)
    val inspectedQuestion: StateFlow<JeeQuestion?> = _inspectedQuestion.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    fun selectProcessingMode(mode: com.example.data.models.ProcessingMode) {
        _processingMode.value = mode
    }

    fun openInspector(question: JeeQuestion) {
        _inspectedQuestion.value = question
    }

    fun closeInspector() {
        _inspectedQuestion.value = null
    }

    fun acceptCrop(qId: Int) {
        val paper = _examPaper.value ?: return
        val updated = paper.questions.map { if (it.id == qId) it.copy(cropStatus = "verified") else it }
        _examPaper.value = paper.copy(questions = updated)
        if (_inspectedQuestion.value?.id == qId) {
            _inspectedQuestion.value = _inspectedQuestion.value?.copy(cropStatus = "verified")
        }
    }

    fun recropQuestion(qId: Int) {
        val paper = _examPaper.value ?: return
        val updated = paper.questions.map { if (it.id == qId) it.copy(cropStatus = "needs_review", cropBoundingBox = "X: 2%, Y: 5%, W: 96%, H: 35%") else it }
        _examPaper.value = paper.copy(questions = updated)
        if (_inspectedQuestion.value?.id == qId) {
            _inspectedQuestion.value = _inspectedQuestion.value?.copy(cropStatus = "needs_review", cropBoundingBox = "X: 2%, Y: 5%, W: 96%, H: 35%")
        }
    }

    fun keepFullRegion(qId: Int) {
        val paper = _examPaper.value ?: return
        val updated = paper.questions.map { if (it.id == qId) it.copy(cropStatus = "full_region", cropBoundingBox = "X: 0%, Y: 0%, W: 100%, H: 100%") else it }
        _examPaper.value = paper.copy(questions = updated)
        if (_inspectedQuestion.value?.id == qId) {
            _inspectedQuestion.value = _inspectedQuestion.value?.copy(cropStatus = "full_region", cropBoundingBox = "X: 0%, Y: 0%, W: 100%, H: 100%")
        }
    }

    fun exportAnalysisJson(): String {
        val paper = _examPaper.value ?: return "{}"
        val json = paperProcessor.exportPaperToJson(paper)
        _exportMessage.value = "Exported JSON (${json.length} chars)"
        return json
    }

    fun exportAnalysisCsv(): String {
        val paper = _examPaper.value ?: return ""
        val csv = paperProcessor.exportPaperToCsv(paper)
        _exportMessage.value = "Exported CSV (${csv.lines().size} rows)"
        return csv
    }

    fun dismissExportMessage() {
        _exportMessage.value = null
    }

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionResult = MutableStateFlow<String?>(null)
    val connectionResult: StateFlow<String?> = _connectionResult.asStateFlow()

    // Upload Files
    private val _questionsPdfUri = MutableStateFlow<Uri?>(null)
    val questionsPdfUri: StateFlow<Uri?> = _questionsPdfUri.asStateFlow()

    private val _questionsPdfMetaData = MutableStateFlow<PdfFileMetaData?>(null)
    val questionsPdfMetaData: StateFlow<PdfFileMetaData?> = _questionsPdfMetaData.asStateFlow()

    private val _answerKeyPdfUri = MutableStateFlow<Uri?>(null)
    val answerKeyPdfUri: StateFlow<Uri?> = _answerKeyPdfUri.asStateFlow()

    private val _answerKeyPdfMetaData = MutableStateFlow<PdfFileMetaData?>(null)
    val answerKeyPdfMetaData: StateFlow<PdfFileMetaData?> = _answerKeyPdfMetaData.asStateFlow()

    // Duration in minutes (Default 180 mins)
    private val _selectedDurationMinutes = MutableStateFlow(180)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    // Processing sequence
    private val _processingStage = MutableStateFlow(ProcessingStage.UPLOADING_QUESTIONS_PDF)
    val processingStage: StateFlow<ProcessingStage> = _processingStage.asStateFlow()

    private val _stageMessage = MutableStateFlow<String?>(null)
    val stageMessage: StateFlow<String?> = _stageMessage.asStateFlow()

    private val _processingError = MutableStateFlow<String?>(null)
    val processingError: StateFlow<String?> = _processingError.asStateFlow()

    // Exam & CBT state
    private val _examPaper = MutableStateFlow<JeeExamPaper?>(null)
    val examPaper: StateFlow<JeeExamPaper?> = _examPaper.asStateFlow()

    private val _activeSubject = MutableStateFlow("Physics")
    val activeSubject: StateFlow<String> = _activeSubject.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userResponses = MutableStateFlow<Map<Int, UserQuestionState>>(emptyMap())
    val userResponses: StateFlow<Map<Int, UserQuestionState>> = _userResponses.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(180 * 60L)
    val timeRemainingSeconds: StateFlow<Long> = _timeRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _showLowTimeWarning = MutableStateFlow(false)
    val showLowTimeWarning: StateFlow<Boolean> = _showLowTimeWarning.asStateFlow()

    private val _paletteOpen = MutableStateFlow(false)
    val paletteOpen: StateFlow<Boolean> = _paletteOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSubmitDialog = MutableStateFlow(false)
    val showSubmitDialog: StateFlow<Boolean> = _showSubmitDialog.asStateFlow()

    private val _examResult = MutableStateFlow<ExamResult?>(null)
    val examResult: StateFlow<ExamResult?> = _examResult.asStateFlow()

    private var timerJob: Job? = null

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setDurationMinutes(mins: Int) {
        if (mins in 5..300) {
            _selectedDurationMinutes.value = mins
            _timeRemainingSeconds.value = mins * 60L
        }
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionResult.value = null

            val res = geminiApiClient.testConnection(
                modelId = _selectedModelId.value,
                customApiKey = _customApiKey.value
            )

            res.onSuccess {
                _connectionResult.value = it
            }.onFailure {
                _connectionResult.value = "❌ Gemini Error: ${it.localizedMessage}"
            }

            _isTestingConnection.value = false
        }
    }

    fun setQuestionsPdf(uri: Uri) {
        _questionsPdfUri.value = uri
        viewModelScope.launch {
            val meta = pdfExtractor.getMetaData(uri, isQuestionsPdf = true)
            _questionsPdfMetaData.value = meta
        }
    }

    fun setAnswerKeyPdf(uri: Uri) {
        _answerKeyPdfUri.value = uri
        viewModelScope.launch {
            val meta = pdfExtractor.getMetaData(uri, isQuestionsPdf = false)
            _answerKeyPdfMetaData.value = meta
        }
    }

    fun clearQuestionsPdf() {
        _questionsPdfUri.value = null
        _questionsPdfMetaData.value = null
    }

    fun clearAnswerKeyPdf() {
        _answerKeyPdfUri.value = null
        _answerKeyPdfMetaData.value = null
    }

    fun loadSamplePaper() {
        val sample = paperProcessor.getSampleJeeMainPaper()
        _examPaper.value = sample
        _screenState.value = ScreenState.VALIDATION
    }

    fun startProcessing() {
        if (_questionsPdfUri.value == null && _answerKeyPdfUri.value == null) {
            // Load sample paper if no PDFs selected
            loadSamplePaper()
            return
        }

        _screenState.value = ScreenState.PROCESSING
        _processingError.value = null

        viewModelScope.launch {
            try {
                paperProcessor.processPdfs(
                    questionsPdfUri = _questionsPdfUri.value,
                    answerKeyPdfUri = _answerKeyPdfUri.value,
                    selectedModelId = _selectedModelId.value,
                    processingMode = _processingMode.value,
                    onStageUpdate = { stage, msg ->
                        _processingStage.value = stage
                        _stageMessage.value = msg
                    }
                ).collect { paper ->
                    val paperWithDuration = paper.copy(durationMinutes = _selectedDurationMinutes.value)
                    _examPaper.value = paperWithDuration
                    _screenState.value = ScreenState.VALIDATION
                }
            } catch (e: Exception) {
                _processingStage.value = ProcessingStage.FAILED
                _processingError.value = e.localizedMessage ?: "PDF processing failed. Please check files or API key."
            }
        }
    }

    fun retryProcessing() {
        startProcessing()
    }

    fun proceedToCbtExam() {
        val paper = _examPaper.value ?: return
        _screenState.value = ScreenState.CBT_EXAM
        _activeSubject.value = "Physics"
        _currentQuestionIndex.value = 0
        _timeRemainingSeconds.value = paper.durationMinutes * 60L
        _showLowTimeWarning.value = false

        // Initialize user question state
        val initialResponses = paper.questions.associate { q ->
            q.id to UserQuestionState()
        }.toMutableMap()

        // Mark first question as visited
        if (paper.questions.isNotEmpty()) {
            val firstId = paper.questions.first().id
            initialResponses[firstId] = UserQuestionState(isVisited = true)
        }

        _userResponses.value = initialResponses
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value && _timeRemainingSeconds.value > 0) {
                delay(1000)
                _timeRemainingSeconds.value = _timeRemainingSeconds.value - 1
                if (_timeRemainingSeconds.value <= 300) { // 5 mins left warning
                    _showLowTimeWarning.value = true
                }
            }
            if (_timeRemainingSeconds.value <= 0) {
                submitTest()
            }
        }
    }

    fun selectSubject(subject: String) {
        _activeSubject.value = subject
        val paper = _examPaper.value ?: return
        val subjectQuestions = paper.questions.filter { it.subject.equals(subject, ignoreCase = true) }
        if (subjectQuestions.isNotEmpty()) {
            val firstInSubject = subjectQuestions.first()
            val targetIndex = paper.questions.indexOf(firstInSubject)
            if (targetIndex != -1) {
                navigateToQuestionIndex(targetIndex)
            }
        }
    }

    fun navigateToQuestionIndex(index: Int) {
        val paper = _examPaper.value ?: return
        if (index in paper.questions.indices) {
            _currentQuestionIndex.value = index
            val q = paper.questions[index]
            _activeSubject.value = q.subject

            // Mark as visited
            val map = _userResponses.value.toMutableMap()
            val current = map[q.id] ?: UserQuestionState()
            map[q.id] = current.copy(isVisited = true)
            _userResponses.value = map
        }
    }

    fun selectSingleOption(qId: Int, optionLabel: String) {
        val map = _userResponses.value.toMutableMap()
        val current = map[qId] ?: UserQuestionState()
        map[qId] = current.copy(selectedOption = optionLabel, isVisited = true)
        _userResponses.value = map
    }

    fun toggleMultipleOption(qId: Int, optionLabel: String) {
        val map = _userResponses.value.toMutableMap()
        val current = map[qId] ?: UserQuestionState()
        val currentSelections = current.selectedOption.split(",").filter { it.isNotBlank() }.toMutableSet()

        if (currentSelections.contains(optionLabel)) {
            currentSelections.remove(optionLabel)
        } else {
            currentSelections.add(optionLabel)
        }

        val updatedOptionStr = currentSelections.sorted().joinToString(",")
        map[qId] = current.copy(selectedOption = updatedOptionStr, isVisited = true)
        _userResponses.value = map
    }

    fun updateTextResponse(qId: Int, text: String) {
        val map = _userResponses.value.toMutableMap()
        val current = map[qId] ?: UserQuestionState()
        map[qId] = current.copy(textResponse = text, isVisited = true)
        _userResponses.value = map
    }

    fun clearResponse(qId: Int) {
        val map = _userResponses.value.toMutableMap()
        val current = map[qId] ?: UserQuestionState()
        map[qId] = current.copy(selectedOption = "", textResponse = "", isVisited = true)
        _userResponses.value = map
    }

    fun markForReview(qId: Int) {
        val map = _userResponses.value.toMutableMap()
        val current = map[qId] ?: UserQuestionState()
        map[qId] = current.copy(isMarkedForReview = !current.isMarkedForReview, isVisited = true)
        _userResponses.value = map
    }

    fun saveAndNext() {
        val paper = _examPaper.value ?: return
        val nextIdx = _currentQuestionIndex.value + 1
        if (nextIdx < paper.questions.size) {
            navigateToQuestionIndex(nextIdx)
        }
    }

    fun previousQuestion() {
        val prevIdx = _currentQuestionIndex.value - 1
        if (prevIdx >= 0) {
            navigateToQuestionIndex(prevIdx)
        }
    }

    fun togglePalette() {
        _paletteOpen.value = !_paletteOpen.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openSubmitDialog() {
        _showSubmitDialog.value = true
    }

    fun closeSubmitDialog() {
        _showSubmitDialog.value = false
    }

    fun submitTest() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _showSubmitDialog.value = false

        val paper = _examPaper.value ?: return
        val responses = _userResponses.value

        var totalAttempted = 0
        var totalCorrect = 0
        var totalIncorrect = 0
        var totalUnanswered = 0
        var totalScore = 0

        val questionDetails = mutableListOf<QuestionResultDetail>()
        val subjectStatsMap = mutableMapOf<String, MutableList<QuestionResultDetail>>()

        for (q in paper.questions) {
            val resp = responses[q.id] ?: UserQuestionState()
            val userAns = if (resp.selectedOption.isNotBlank()) resp.selectedOption else resp.textResponse
            val officialAns = q.correctAnswer

            val isAttempted = userAns.isNotBlank()
            val isUncertain = q.answerMappingStatus == AnswerMappingStatus.UNCERTAIN

            var isCorrect = false
            var isUnanswered = false
            var marks = 0

            if (!isAttempted) {
                isUnanswered = true
                totalUnanswered++
                marks = 0
            } else if (isUncertain) {
                // If official answer is uncertain, do not penalize the user
                totalAttempted++
                marks = 0
            } else {
                totalAttempted++
                if (checkAnswerMatch(userAns, officialAns, q.type)) {
                    isCorrect = true
                    totalCorrect++
                    marks = 4
                } else {
                    isCorrect = false
                    totalIncorrect++
                    marks = -1
                }
            }

            totalScore += marks

            val detail = QuestionResultDetail(
                question = q,
                userAnswer = if (userAns.isBlank()) "Unanswered" else userAns,
                officialAnswer = if (officialAns.isBlank()) "Official Answer Unavailable" else officialAns,
                isCorrect = isCorrect,
                isUnanswered = isUnanswered,
                isUncertainOfficial = isUncertain,
                marksAwarded = marks
            )

            questionDetails.add(detail)

            val subjList = subjectStatsMap.getOrPut(q.subject) { mutableListOf() }
            subjList.add(detail)
        }

        val subjectScores = subjectStatsMap.map { (subjName, details) ->
            val totalQ = details.size
            val att = details.count { !it.isUnanswered }
            val corr = details.count { it.isCorrect }
            val inc = details.count { !it.isUnanswered && !it.isCorrect && !it.isUncertainOfficial }
            val unans = details.count { it.isUnanswered }
            val scoreSum = details.sumOf { it.marksAwarded }
            val maxSc = totalQ * 4
            val acc = if (att > 0) (corr.toFloat() / att.toFloat()) * 100f else 0f

            SubjectScore(
                subject = subjName,
                totalQuestions = totalQ,
                attempted = att,
                correct = corr,
                incorrect = inc,
                unanswered = unans,
                score = scoreSum,
                maxScore = maxSc,
                accuracyPercentage = acc
            )
        }

        val maxPossible = paper.questions.size * 4
        val overallAcc = if (totalAttempted > 0) (totalCorrect.toFloat() / totalAttempted.toFloat()) * 100f else 0f
        val timeTaken = (paper.durationMinutes * 60L) - _timeRemainingSeconds.value

        val result = ExamResult(
            paperTitle = paper.title,
            totalQuestions = paper.questions.size,
            totalAttempted = totalAttempted,
            totalCorrect = totalCorrect,
            totalIncorrect = totalIncorrect,
            totalUnanswered = totalUnanswered,
            totalScore = totalScore,
            maxPossibleScore = maxPossible,
            overallAccuracy = overallAcc,
            timeTakenSeconds = timeTaken,
            subjectScores = subjectScores,
            questionResults = questionDetails
        )

        _examResult.value = result
        _screenState.value = ScreenState.RESULT_ANALYSIS
    }

    private fun checkAnswerMatch(userAns: String, officialAns: String, type: com.example.data.models.QuestionType): Boolean {
        if (officialAns.isBlank()) return false
        val u = userAns.trim().uppercase()
        val o = officialAns.trim().uppercase()

        if (u == o) return true

        // For numerical answers, check numeric equivalence
        val uNum = u.toDoubleOrNull()
        val oNum = o.toDoubleOrNull()
        if (uNum != null && oNum != null) {
            return Math.abs(uNum - oNum) < 0.05
        }

        // For option letters e.g. "A. Option 1" vs "A"
        if (o.length == 1 && u.startsWith(o)) return true
        if (u.length == 1 && o.startsWith(u)) return true

        return false
    }

    fun resetToSetup() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _screenState.value = ScreenState.SETUP
        _examPaper.value = null
        _userResponses.value = emptyMap()
        _examResult.value = null
    }
}
