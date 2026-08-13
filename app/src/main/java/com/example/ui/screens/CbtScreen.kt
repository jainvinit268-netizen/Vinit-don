package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.PaletteState
import com.example.data.models.QuestionType
import com.example.ui.viewmodel.JeeCbtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbtScreen(viewModel: JeeCbtViewModel) {
    val examPaper by viewModel.examPaper.collectAsState()
    val activeSubject by viewModel.activeSubject.collectAsState()
    val currentQuestionIdx by viewModel.currentQuestionIndex.collectAsState()
    val userResponses by viewModel.userResponses.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val showLowTimeWarning by viewModel.showLowTimeWarning.collectAsState()
    val paletteOpen by viewModel.paletteOpen.collectAsState()
    val showSubmitDialog by viewModel.showSubmitDialog.collectAsState()

    val paper = examPaper ?: return
    val currentQuestion = paper.questions.getOrNull(currentQuestionIdx) ?: return
    val responseState = userResponses[currentQuestion.id] ?: com.example.data.models.UserQuestionState()

    val hours = timeRemaining / 3600
    val minutes = (timeRemaining % 3600) / 60
    val seconds = timeRemaining % 60
    val timerString = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(paletteOpen) {
        if (paletteOpen) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && paletteOpen) {
            viewModel.togglePalette()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF8FAFC),
                modifier = Modifier.width(320.dp)
            ) {
                PaletteDrawerContent(viewModel = viewModel)
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    // NTA Header
                    Surface(
                        color = Color(0xFF1E293B),
                        contentColor = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "JEE Main 2027",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(containerColor = Color(0xFF6366F1)) {
                                    Text("CBT Mode", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            // Timer Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (showLowTimeWarning) Color(0xFFDC2626) else Color(0xFF0F172A))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = if (showLowTimeWarning) Color.White else Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timerString,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.togglePalette() },
                                modifier = Modifier.testTag("open_palette_button")
                            ) {
                                Icon(Icons.Default.GridOn, contentDescription = "Palette", tint = Color.White)
                            }
                        }
                    }

                    // Low Time Warning Alert Banner
                    if (showLowTimeWarning) {
                        Surface(color = Color(0xFFDC2626)) {
                            Text(
                                text = "⚠️ Warning: Less than 5 minutes remaining!",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Subject Tabs Bar
                    ScrollableTabRow(
                        selectedTabIndex = listOf("Physics", "Chemistry", "Mathematics").indexOfFirst {
                            it.equals(activeSubject, ignoreCase = true)
                        }.coerceAtLeast(0),
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        edgePadding = 8.dp
                    ) {
                        listOf("Physics", "Chemistry", "Mathematics").forEach { subj ->
                            Tab(
                                selected = activeSubject.equals(subj, ignoreCase = true),
                                onClick = { viewModel.selectSubject(subj) },
                                text = {
                                    Text(
                                        text = subj.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Fixed Bottom Controls Bar
                Surface(
                    color = Color(0xFFF1F5F9),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.clearResponse(currentQuestion.id) },
                                modifier = Modifier.testTag("clear_response_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
                            ) {
                                Icon(Icons.Outlined.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.markForReview(currentQuestion.id) },
                                modifier = Modifier.testTag("mark_for_review_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (responseState.isMarkedForReview) Color(0xFF8B5CF6) else Color(0xFF475569)
                                )
                            ) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (responseState.isMarkedForReview) "Unmark" else "Mark for Review",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.previousQuestion() },
                                enabled = currentQuestionIdx > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                                modifier = Modifier.testTag("previous_question_button")
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = null)
                                Text("Previous")
                            }

                            Button(
                                onClick = { viewModel.saveAndNext() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.testTag("save_and_next_button")
                            ) {
                                Text("Save & Next", fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.NavigateNext, contentDescription = null)
                            }

                            Button(
                                onClick = { viewModel.openSubmitDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.testTag("submit_test_button")
                            ) {
                                Text("Submit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Main Clean White Question View with smooth transition
            AnimatedContent(
                targetState = currentQuestionIdx,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 8 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally { width -> -width / 8 } + fadeOut(tween(150)))
                    } else {
                        (slideInHorizontally { width -> -width / 8 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally { width -> width / 8 } + fadeOut(tween(150)))
                    }
                },
                label = "question_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White)
            ) { qIdx ->
                val q = paper.questions.getOrNull(qIdx) ?: return@AnimatedContent
                val qResponse = userResponses[q.id] ?: com.example.data.models.UserQuestionState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Question Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question No. ${q.id}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.ExtraBold
                            )
                        )

                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = q.type.displayName,
                                color = Color(0xFF1D4ED8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                    // Question Text
                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF1E293B),
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Options Area based on Question Type
                    when (q.type) {
                        QuestionType.SINGLE, QuestionType.ASSERTION, QuestionType.MATRIX -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                q.options.forEach { opt ->
                                    val optionLabel = opt.take(1) // e.g. "A"
                                    val isSelected = qResponse.selectedOption == optionLabel

                                    val cardBg by animateColorAsState(
                                        if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        animationSpec = tween(200),
                                        label = "opt_bg"
                                    )
                                    val cardBorder by animateColorAsState(
                                        if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1),
                                        animationSpec = tween(200),
                                        label = "opt_border"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectSingleOption(q.id, optionLabel) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = cardBg,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = cardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.selectSingleOption(q.id, optionLabel) },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = opt,
                                                color = Color(0xFF0F172A),
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        QuestionType.MULTIPLE -> {
                            val selectedSet = qResponse.selectedOption.split(",").filter { it.isNotBlank() }.toSet()
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                q.options.forEach { opt ->
                                    val optionLabel = opt.take(1)
                                    val isSelected = selectedSet.contains(optionLabel)

                                    val cardBg by animateColorAsState(
                                        if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        animationSpec = tween(200),
                                        label = "opt_bg"
                                    )
                                    val cardBorder by animateColorAsState(
                                        if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1),
                                        animationSpec = tween(200),
                                        label = "opt_border"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleMultipleOption(q.id, optionLabel) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = cardBg,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = cardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { viewModel.toggleMultipleOption(q.id, optionLabel) },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = opt,
                                                color = Color(0xFF0F172A),
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        QuestionType.NUMERICAL, QuestionType.INTEGER -> {
                            Column {
                                Text(
                                    "Enter your answer (Numerical value):",
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = qResponse.textResponse,
                                    onValueChange = { viewModel.updateTextResponse(q.id, it) },
                                    placeholder = { Text("e.g. 25 or 3.5") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("numerical_answer_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF0F172A),
                                        unfocusedTextColor = Color(0xFF0F172A),
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = Color(0xFF0284C7),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitDialog) {
        val responses = userResponses
        val attemptedCount = responses.values.count { it.selectedOption.isNotBlank() || it.textResponse.isNotBlank() }
        val unattemptedCount = paper.questions.size - attemptedCount
        val markedCount = responses.values.count { it.isMarkedForReview }

        AlertDialog(
            onDismissRequest = { viewModel.closeSubmitDialog() },
            title = { Text("Submit Examination?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to submit your CBT examination?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Total Questions: ${paper.questions.size}", fontWeight = FontWeight.Bold)
                    Text("• Attempted: $attemptedCount", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    Text("• Unattempted: $unattemptedCount", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    Text("• Marked for Review: $markedCount", color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier.testTag("confirm_submit_button")
                ) {
                    Text("SUBMIT TEST NOW", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeSubmitDialog() }) {
                    Text("Resume Test")
                }
            }
        )
    }
}

@Composable
fun PaletteDrawerContent(viewModel: JeeCbtViewModel) {
    val examPaper by viewModel.examPaper.collectAsState()
    val activeSubject by viewModel.activeSubject.collectAsState()
    val userResponses by viewModel.userResponses.collectAsState()

    val paper = examPaper ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Question Palette",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            PaletteLegendItem(PaletteState.NOT_VISITED)
            PaletteLegendItem(PaletteState.NOT_ANSWERED)
            PaletteLegendItem(PaletteState.ANSWERED)
            PaletteLegendItem(PaletteState.MARKED_FOR_REVIEW)
            PaletteLegendItem(PaletteState.ANSWERED_AND_MARKED)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Grid Sections
        listOf("Physics", "Chemistry", "Mathematics").forEach { subj ->
            val subjQuestions = paper.questions.filter { it.subject.equals(subj, ignoreCase = true) }
            if (subjQuestions.isNotEmpty()) {
                Text(
                    text = subj.uppercase(),
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(subjQuestions) { q ->
                        val resp = userResponses[q.id] ?: com.example.data.models.UserQuestionState()
                        val paletteState = resp.getPaletteState()

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(paletteState.color)
                                .clickable {
                                    val idx = paper.questions.indexOf(q)
                                    if (idx != -1) viewModel.navigateToQuestionIndex(idx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${q.id}",
                                color = if (paletteState == PaletteState.NOT_VISITED) Color(0xFF0F172A) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PaletteLegendItem(state: PaletteState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(state.color)
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
        ) { }
        Spacer(modifier = Modifier.width(8.dp))
        Text(state.label, fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
    }
}
