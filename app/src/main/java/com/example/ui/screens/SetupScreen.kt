package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
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
import com.example.data.api.GeminiApiClient
import com.example.data.models.PdfFileMetaData
import com.example.ui.viewmodel.JeeCbtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: JeeCbtViewModel) {
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val connectionResult by viewModel.connectionResult.collectAsState()

    val questionsPdfMeta by viewModel.questionsPdfMetaData.collectAsState()
    val answerKeyPdfMeta by viewModel.answerKeyPdfMetaData.collectAsState()
    val durationMins by viewModel.selectedDurationMinutes.collectAsState()

    val questionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setQuestionsPdf(it) }
    }

    val answerKeyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAnswerKeyPdf(it) }
    }

    var showModelDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Devotional & App Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Jay Ganpati Bappa • Radhe Krishna • Jay Premanand Ji Maharaj ❤️❤️❤️",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "JM App Icon",
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "JEE Main 2027",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "PDF → CBT Converter",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Gemini Engine Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF818CF8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini AI Engine",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Model Selector
                    ExposedDropdownMenuBox(
                        expanded = showModelDropdown,
                        onExpandedChange = { showModelDropdown = !showModelDropdown }
                    ) {
                        val currentModel = GeminiApiClient.SUPPORTED_MODELS.find { it.id == selectedModelId }
                        OutlinedTextField(
                            value = currentModel?.displayName ?: selectedModelId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Gemini Model", color = Color(0xFF94A3B8)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("gemini_model_dropdown"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            GeminiApiClient.SUPPORTED_MODELS.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(model.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectModel(model.id)
                                        showModelDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Connection Button
                    Button(
                        onClick = { viewModel.testGeminiConnection() },
                        enabled = !isTestingConnection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_gemini_connection_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA))
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TEST GEMINI CONNECTION", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Connection Result Banner
                    connectionResult?.let { resMsg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        val isSuccess = resMsg.contains("successfully", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSuccess) Color(0xFF065F46) else Color(0xFF991B1B))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = resMsg,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Duration Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Test Duration", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$durationMins Minutes (Standard JEE Main)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.setDurationMinutes(durationMins - 15) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF334155))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                        }
                        Text(
                            "$durationMins m",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.setDurationMinutes(durationMins + 15) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF334155))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                        }
                    }
                }
            }

            // PDF Analysis Mode Card
            val currentProcessingMode by viewModel.processingMode.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("PDF Analysis Engine Mode", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Select speed vs deep layout vision trade-off", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.data.models.ProcessingMode.values().forEach { mode ->
                            val isSelected = currentProcessingMode == mode
                            Surface(
                                onClick = { viewModel.selectProcessingMode(mode) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF6366F1) else Color(0xFF0F172A)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        mode.displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        mode.description,
                                        color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PDF Upload Areas
            Text(
                text = "UPLOAD EXAMINATION PDFS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            // 1. QUESTIONS PDF UPLOAD AREA
            PdfUploadBox(
                title = "1. QUESTIONS PDF",
                description = "Upload Question Paper (Text or Scanned)",
                metaData = questionsPdfMeta,
                onSelectClick = { questionsLauncher.launch("application/pdf") },
                onClearClick = { viewModel.clearQuestionsPdf() },
                testTag = "upload_questions_pdf_button"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. OFFICIAL ANSWER KEY PDF UPLOAD AREA
            PdfUploadBox(
                title = "2. OFFICIAL ANSWER KEY PDF",
                description = "Upload Official Answer Key (NTA Format)",
                metaData = answerKeyPdfMeta,
                onSelectClick = { answerKeyLauncher.launch("application/pdf") },
                onClearClick = { viewModel.clearAnswerKeyPdf() },
                testTag = "upload_answer_key_pdf_button"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = { viewModel.startProcessing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_processing_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PROCESS PDFS & CONVERT TO CBT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.loadSamplePaper() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("load_sample_paper_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))))
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOAD NTA JEE MAIN SAMPLE MOCK PAPER", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PdfUploadBox(
    title: String,
    description: String,
    metaData: PdfFileMetaData?,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit,
    testTag: String
) {
    val borderColor by animateColorAsState(
        targetValue = if (metaData != null) Color(0xFF10B981) else Color(0xFF334155),
        animationSpec = tween(durationMillis = 250),
        label = "upload_border_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (metaData == null) onSelectClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(width = 1.5.dp, color = borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                AnimatedVisibility(
                    visible = metaData != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = onClearClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = metaData,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.96f)) togetherWith (fadeOut(tween(150)) + scaleOut(targetScale = 0.96f))
                },
                label = "pdf_upload_state"
            ) { fileMeta ->
                if (fileMeta != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF065F46).copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✓ ${fileMeta.fileName}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "File size: ${fileMeta.fileSizeFormatted}",
                                color = Color(0xFFA7F3D0),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to Select PDF", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(description, color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
