package com.example.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JeeCbtViewModel

@Composable
fun ResultScreen(viewModel: JeeCbtViewModel) {
    val result by viewModel.examResult.collectAsState()
    val res = result ?: return

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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Examination Performance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = res.paperTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFA5B4FC))
                    )
                }

                Button(
                    onClick = { viewModel.resetToSetup() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.testTag("start_new_test_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Test")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val animatedScore by animateIntAsState(
                targetValue = res.totalScore,
                animationSpec = tween(durationMillis = 600),
                label = "score_anim"
            )

            // Score Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF6366F1)))
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL SCORE", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$animatedScore / ${res.maxPossibleScore}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = if (res.totalScore >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ScoreStatItem("Accuracy", "${String.format("%.1f", res.overallAccuracy)}%", Color(0xFF38BDF8))
                        ScoreStatItem("Attempted", "${res.totalAttempted} / ${res.totalQuestions}", Color.White)
                        ScoreStatItem("Correct", "${res.totalCorrect}", Color(0xFF10B981))
                        ScoreStatItem("Incorrect", "${res.totalIncorrect}", Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subject Breakdown Title
            Text(
                text = "SUBJECT-WISE ANALYSIS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subject Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                res.subjectScores.forEach { subj ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                subj.subject,
                                color = when (subj.subject.lowercase()) {
                                    "physics" -> Color(0xFF38BDF8)
                                    "chemistry" -> Color(0xFFF43F5E)
                                    else -> Color(0xFF10B981)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${subj.score} marks",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Acc: ${String.format("%.0f", subj.accuracyPercentage)}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                "✓${subj.correct}  ✗${subj.incorrect}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "QUESTION-BY-QUESTION REVIEW",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question Details Review List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(res.questionResults) { detail ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                detail.isUncertainOfficial -> Color(0xFF27272A)
                                detail.isCorrect -> Color(0xFF064E3B)
                                detail.isUnanswered -> Color(0xFF18181B)
                                else -> Color(0xFF7F1D1D)
                            }
                        )
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
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Q${detail.question.id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(detail.question.subject, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Surface(
                                    color = when {
                                        detail.isUncertainOfficial -> Color(0xFF71717A)
                                        detail.isCorrect -> Color(0xFF10B981)
                                        detail.isUnanswered -> Color(0xFF64748B)
                                        else -> Color(0xFFEF4444)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            detail.isUncertainOfficial -> "Needs Verification"
                                            detail.isCorrect -> "+4 Correct"
                                            detail.isUnanswered -> "0 Unanswered"
                                            else -> "-1 Incorrect"
                                        },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = detail.question.question,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Your Answer: ${detail.userAnswer}",
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Official Key: ${detail.officialAnswer}",
                                    color = Color(0xFFFDE047),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreStatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
    }
}
