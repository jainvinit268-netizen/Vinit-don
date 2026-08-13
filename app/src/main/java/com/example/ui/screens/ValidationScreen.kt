package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.AnswerMappingStatus
import com.example.data.models.ConfidenceLevel
import com.example.data.models.JeeQuestion
import com.example.ui.viewmodel.JeeCbtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidationScreen(viewModel: JeeCbtViewModel) {
    val examPaper by viewModel.examPaper.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val inspectedQuestion by viewModel.inspectedQuestion.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    val paper = examPaper ?: return

    val filteredQuestions = remember(paper, searchQuery) {
        if (searchQuery.isBlank()) {
            paper.questions
        } else {
            paper.questions.filter { q ->
                q.id.toString().contains(searchQuery) ||
                        q.stableQuestionId.contains(searchQuery, ignoreCase = true) ||
                        q.question.contains(searchQuery, ignoreCase = true) ||
                        q.subject.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PDF Analysis Dashboard",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFA5B4FC))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.exportAnalysisJson() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(Icons.Outlined.Code, contentDescription = "Export JSON", tint = Color(0xFF38BDF8))
                    }
                    IconButton(
                        onClick = { viewModel.exportAnalysisCsv() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(Icons.Outlined.TableChart, contentDescription = "Export CSV", tint = Color(0xFF10B981))
                    }
                    IconButton(
                        onClick = { viewModel.resetToSetup() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }

            AnimatedVisibility(
                visible = exportMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF065F46),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(exportMessage ?: "", color = Color.White, fontSize = 12.sp)
                            IconButton(onClick = { viewModel.dismissExportMessage() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatCard("Total Qs", "${paper.totalQuestions}", Color(0xFF6366F1), Modifier.weight(1f))
                StatCard("Physics", "${paper.physicsCount}", Color(0xFF38BDF8), Modifier.weight(1f))
                StatCard("Chemistry", "${paper.chemistryCount}", Color(0xFFF43F5E), Modifier.weight(1f))
                StatCard("Maths", "${paper.mathCount}", Color(0xFF10B981), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Advanced Quality Audit Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QualityCard("Extraction Quality", "${paper.highConfidenceCount} High", Color(0xFF10B981), Modifier.weight(1f))
                QualityCard("Answer Mapping", "${paper.mappedCount} Mapped", Color(0xFF38BDF8), Modifier.weight(1f))
                QualityCard("Cropping Bounds", "${paper.cropVerifiedCount} Verified", Color(0xFF818CF8), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Filter by Q#, ID, text, or subject...", color = Color(0xFF64748B)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredQuestions) { q ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openInspector(q) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF334155))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Q${q.id} (${q.stableQuestionId})",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        q.subject,
                                        color = when (q.subject.lowercase()) {
                                            "physics" -> Color(0xFF38BDF8)
                                            "chemistry" -> Color(0xFFF43F5E)
                                            "mathematics" -> Color(0xFF10B981)
                                            else -> Color(0xFFFBBF24)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(q.textConfidence.color.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            q.textConfidence.label,
                                            color = q.textConfidence.color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Inspect", tint = Color(0xFF64748B))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = q.question,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Official Key: ${if (q.correctAnswer.isBlank()) "Uncertain" else q.correctAnswer}",
                                    color = if (q.answerMappingStatus == AnswerMappingStatus.MAPPED) Color(0xFF10B981) else Color(0xFFFBBF24),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Pg ${q.sourcePageStart} • ${q.cropStatus.uppercase()}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Start CBT Button
            Button(
                onClick = { viewModel.proceedToCbtExam() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_cbt_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PROCEED TO CBT EXAMINATION", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Question Inspector Dialog
        inspectedQuestion?.let { q ->
            QuestionInspectorDialog(
                question = q,
                onDismiss = { viewModel.closeInspector() },
                onAcceptCrop = { viewModel.acceptCrop(q.id) },
                onReCrop = { viewModel.recropQuestion(q.id) },
                onKeepFull = { viewModel.keepFullRegion(q.id) },
                onOpenInCbt = {
                    viewModel.closeInspector()
                    viewModel.proceedToCbtExam()
                }
            )
        }
    }
}

@Composable
fun QuestionInspectorDialog(
    question: JeeQuestion,
    onDismiss: () -> Unit,
    onAcceptCrop: () -> Unit,
    onReCrop: () -> Unit,
    onKeepFull: () -> Unit,
    onOpenInCbt: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Inspector (${question.stableQuestionId})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Crop Preview / Bounding Box Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("TRANSPARENT CROP BOUNDS", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(question.cropBoundingBox, color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Crop Status: ${question.cropStatus.uppercase()}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Method: ${question.extractionMethod} | Pages: ${question.sourcePageStart}-${question.sourcePageEnd}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Crop Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onAcceptCrop,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Accept Crop", fontSize = 11.sp, color = Color(0xFF10B981))
                    }
                    OutlinedButton(
                        onClick = onReCrop,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Re-Crop", fontSize = 11.sp, color = Color(0xFFFBBF24))
                    }
                    OutlinedButton(
                        onClick = onKeepFull,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Keep Full", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question Text & Details
                Text("EXACT EXTRACTED TEXT:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(question.question, color = Color.White, fontSize = 13.sp)

                if (question.options.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("OPTIONS:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    question.options.forEach { opt ->
                        Text(opt, color = Color(0xFFE2E8F0), fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Answer Key Mapping Transparency
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("OFFICIAL ANSWER KEY", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(if (question.correctAnswer.isBlank()) "Uncertain" else question.correctAnswer, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MAPPING SOURCE", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(question.mappingSource, color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenInCbt,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("OPEN IN CBT ENVIRONMENT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
    }
}

@Composable
fun QualityCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

