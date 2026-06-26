package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.api.SeoResponse
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.InputType
import com.example.viewmodel.SeoUiState
import com.example.viewmodel.SeoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFFDFBFF) // Clean light professional theme
                ) { innerPadding ->
                    SeoGeneratorScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SeoGeneratorScreen(
    modifier: Modifier = Modifier,
    viewModel: SeoViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val inputType by viewModel.inputType.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val history by viewModel.history.collectAsState()

    // Inputs
    val textPrompt by viewModel.textPrompt.collectAsState()
    val imageBitmap by viewModel.imageBitmap.collectAsState()
    val presetImageResId by viewModel.presetImageResId.collectAsState()
    val videoLink by viewModel.videoLink.collectAsState()
    val videoDescription by viewModel.videoDescription.collectAsState()
    val videoFileName by viewModel.videoFileName.collectAsState()

    val scrollState = rememberScrollState()

    // Media picking launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setImageUri(uri, context)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val name = getFileName(context, uri)
            viewModel.setVideoUri(uri, name)
        }
    }

    // Dynamic warning check for empty API key
    val isApiKeyMissing = remember {
        BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFFDFBFF))
    ) {
        // Elegant Design Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI ASSISTANT",
                    color = Color(0xFF43474E).copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SEO Generator",
                    color = Color(0xFF001D36),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1E4FF))
                    .clickable { /* Profile */ },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon_1782444856671),
                    contentDescription = "App Icon Account Logo",
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Hero Banner Card with custom generated illustration
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            border = BorderStroke(1.dp, Color(0xFFC3C7D0))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_seo_banner_1782444874279),
                    contentDescription = "SEO Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Light Gradient Scrim matching the polished style
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x33001D36), Color(0xAA001D36))
                            )
                        )
                )
                // Overlay text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1E4FF))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "POWERED BY GEMINI 3.5 FLASH",
                            color = Color(0xFFD1E4FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "High-Converting Meta Tags",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 2. API Key Warning Alert if missing
            if (isApiKeyMissing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E6)),
                    border = BorderStroke(1.dp, Color(0xFFF5C2C1)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Setup Required",
                                color = Color(0xFFBA1A1A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "To use the SEO Generator, click the Secrets panel in the Google AI Studio sidebar and set GEMINI_API_KEY.",
                                color = Color(0xFF410002),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Selection Tabs (Input Type)
            Text(
                text = "CHOOSE INPUT TYPE",
                color = Color(0xFF43474E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E2EC))
                    .padding(4.dp)
                    .padding(bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InputTabButton(
                    text = "Text",
                    icon = Icons.Default.Edit,
                    isSelected = inputType == InputType.TEXT,
                    onClick = { viewModel.setInputType(InputType.TEXT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_text")
                )
                InputTabButton(
                    text = "Image",
                    icon = Icons.Default.ImageIcon,
                    isSelected = inputType == InputType.IMAGE,
                    onClick = { viewModel.setInputType(InputType.IMAGE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_image")
                )
                InputTabButton(
                    text = "Video",
                    icon = Icons.Default.PlayArrow,
                    isSelected = inputType == InputType.VIDEO,
                    onClick = { viewModel.setInputType(InputType.VIDEO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_video")
                )
            }

            // 4. Main Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF1F8)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E2EC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    when (inputType) {
                        InputType.TEXT -> {
                            Text(
                                text = "Text Content or Topic Description",
                                color = Color(0xFF001D36),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Provide details, articles, keywords, or summary about your product or website topic.",
                                color = Color(0xFF43474E),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = textPrompt,
                                onValueChange = { viewModel.textPrompt.value = it },
                                placeholder = { Text("E.g., Review of modern leather smartwatches focusing on fitness tracking and sleep analysis...", color = Color(0xFF73777F)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .testTag("input_text_prompt"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF191C1E)),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0061A4),
                                    unfocusedBorderColor = Color(0xFFC3C7D0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PROACTIVE SAMPLES",
                                color = Color(0xFF0061A4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SampleChip(
                                    label = "Leather Smartwatch",
                                    onClick = {
                                        viewModel.textPrompt.value = "Review of modern leather smartwatch focusing on premium aesthetics, 7-day battery life, fitness tracking, and advanced sleep cycle analysis."
                                    }
                                )
                                SampleChip(
                                    label = "রেশমি শাড়ি (Bengali)",
                                    onClick = {
                                        viewModel.textPrompt.value = "ঐতিহ্যবাহী লাল রেশমি শাড়ি নিয়ে আকর্ষণীয় এবং আধুনিক রিভিউ। বিবাহ এবং বিশেষ উৎসবের জন্য সেরা কালেকশন।"
                                    }
                                )
                            }
                        }

                        InputType.IMAGE -> {
                            Text(
                                text = "Analyze Image",
                                color = Color(0xFF001D36),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pick an image from your gallery, or select a preset sample. Gemini will inspect it visually to extract perfect tags.",
                                color = Color(0xFF43474E),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Image Preview or Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(BorderStroke(1.dp, Color(0xFFC3C7D0)), RoundedCornerShape(16.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap!!.asImageBitmap(),
                                        contentDescription = "Selected Image Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Clear Image button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xCC000000))
                                            .clickable { viewModel.selectPresetImage(null, context) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ImageIcon,
                                            contentDescription = "Select Image",
                                            tint = Color(0xFF0061A4),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap to choose Image from Gallery",
                                            color = Color(0xFF191C1E),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Supports JPEG, PNG",
                                            color = Color(0xFF73777F),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "OR TRY A PRESET PREVIEW",
                                color = Color(0xFF0061A4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PresetThumbnail(
                                    resId = R.drawable.img_app_icon_1782444856671,
                                    label = "App Icon Logo",
                                    isSelected = presetImageResId == R.drawable.img_app_icon_1782444856671,
                                    onClick = { viewModel.selectPresetImage(R.drawable.img_app_icon_1782444856671, context) }
                                )
                                PresetThumbnail(
                                    resId = R.drawable.img_seo_banner_1782444874279,
                                    label = "SEO Banner",
                                    isSelected = presetImageResId == R.drawable.img_seo_banner_1782444874279,
                                    onClick = { viewModel.selectPresetImage(R.drawable.img_seo_banner_1782444874279, context) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Focus Guidelines (Optional)",
                                color = Color(0xFF001D36),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = textPrompt,
                                onValueChange = { viewModel.textPrompt.value = it },
                                placeholder = { Text("E.g., highlight the neon tech aesthetics and search marketing...", color = Color(0xFF73777F)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF191C1E)),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0061A4),
                                    unfocusedBorderColor = Color(0xFFC3C7D0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        InputType.VIDEO -> {
                            Text(
                                text = "Video SEO Details",
                                color = Color(0xFF001D36),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add video web links, specify content descriptions/transcripts, or attach video files.",
                                color = Color(0xFF43474E),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Video Link
                            Text(
                                text = "Video Link",
                                color = Color(0xFF001D36),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = videoLink,
                                onValueChange = { viewModel.videoLink.value = it },
                                placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color(0xFF73777F)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_video_link"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF191C1E)),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0061A4),
                                    unfocusedBorderColor = Color(0xFFC3C7D0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Video Description
                            Text(
                                text = "Video Topic or Script Summary",
                                color = Color(0xFF001D36),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = videoDescription,
                                onValueChange = { viewModel.videoDescription.value = it },
                                placeholder = { Text("E.g., Complete tutorial showing how to bake sourdough bread from scratch with tips...", color = Color(0xFF73777F)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("input_video_desc"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF191C1E)),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0061A4),
                                    unfocusedBorderColor = Color(0xFFC3C7D0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Video File attachment metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Select Video File", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = videoFileName ?: "No file attached",
                                    color = if (videoFileName != null) Color(0xFF0061A4) else Color(0xFF73777F),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "PROACTIVE VIDEO SAMPLES",
                                color = Color(0xFF0061A4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SampleChip(
                                    label = "Tech Review Link",
                                    onClick = {
                                        viewModel.videoLink.value = "https://www.youtube.com/watch?v=sample_tech_review"
                                        viewModel.videoDescription.value = "Unboxing and testing the ultimate 4K travel drone. Showing cinematic flyovers, stability, battery life, and smart obstacle avoidance features."
                                    }
                                )
                                SampleChip(
                                    label = "রান্না রেসিপি (Bengali Video)",
                                    onClick = {
                                        viewModel.videoLink.value = "https://www.youtube.com/watch?v=bangla_recipe_sourdough"
                                        viewModel.videoDescription.value = "সহজ উপায়ে ঘরে বসে পারফেক্ট মুচমুচে ফুচকা তৈরির রেসিপি। পারফেক্ট তেঁতুলের টক এবং ফুচকার পুর তৈরির সহজ ট্রিকস।"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Language Selector
            Text(
                text = "FORCE OUTPUT LANGUAGE",
                color = Color(0xFF43474E),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageChip(
                    text = "Auto-detect",
                    isSelected = selectedLang == "Auto",
                    onClick = { viewModel.selectedLanguage.value = "Auto" },
                    modifier = Modifier.weight(1f)
                )
                LanguageChip(
                    text = "English",
                    isSelected = selectedLang == "English",
                    onClick = { viewModel.selectedLanguage.value = "English" },
                    modifier = Modifier.weight(1f)
                )
                LanguageChip(
                    text = "Bengali (বাংলা)",
                    isSelected = selectedLang == "Bengali",
                    onClick = { viewModel.selectedLanguage.value = "Bengali" },
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. Action Button (Generate & Clear)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearInputs() },
                    border = BorderStroke(1.dp, Color(0xFFC3C7D0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF43474E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear Inputs")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }

                Button(
                    onClick = { viewModel.generateSeo(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0061A4)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("generate_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GENERATE METADATA",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 7. Output Section
            AnimatedVisibility(
                visible = uiState != SeoUiState.Idle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                when (val state = uiState) {
                    is SeoUiState.Loading -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF1F8)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C7D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0061A4),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Analyzing Content...",
                                    color = Color(0xFF001D36),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Gemini is building professional, high-converting SEO Titles, 150-200 word Descriptions, and exactly 50 target keywords...",
                                    color = Color(0xFF43474E),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is SeoUiState.Success -> {
                        OutputResultPanel(
                            seoResponse = state.response,
                            onCopy = { label, text -> viewModel.copyToClipboard(context, label, text) }
                        )
                    }

                    is SeoUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E6)),
                            border = BorderStroke(1.dp, Color(0xFFF5C2C1)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Oops! Generation Failed",
                                    color = Color(0xFFBA1A1A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message,
                                    color = Color(0xFF410002),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.generateSeo(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                                ) {
                                    Text("Try Again", color = Color.White)
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }

            // 8. History / Saved Sessions
            if (history.isNotEmpty()) {
                Text(
                    text = "GENERATED HISTORY",
                    color = Color(0xFF43474E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                history.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFC3C7D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "History",
                                        tint = Color(0xFF0061A4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.inputType.name + " Input",
                                        color = Color(0xFF0061A4),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = item.inputDesc,
                                    color = Color(0xFF73777F),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.title,
                                color = Color(0xFF001D36),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                color = Color(0xFF43474E),
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isSelected) Color(0xFF001D36) else Color(0xFF43474E),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = if (isSelected) Color(0xFF001D36) else Color(0xFF43474E),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFFD1E4FF),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF001D36),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PresetThumbnail(
    resId: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF0061A4) else Color(0xFFC3C7D0),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF0061A4) else Color(0xFF43474E),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LanguageChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFFD1E4FF) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0061A4) else Color(0xFFC3C7D0)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF001D36) else Color(0xFF43474E),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutputResultPanel(
    seoResponse: SeoResponse,
    onCopy: (label: String, text: String) -> Unit
) {
    // Break keywords down
    val keywordsList = remember(seoResponse.keywords) {
        seoResponse.keywords
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "GENERATED SEO META ASSETS",
            color = Color(0xFF43474E),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Title Output Container
        OutputCard(
            title = "SEO Optimized Title",
            content = seoResponse.title,
            copyTag = "copy_title_button",
            onCopy = { onCopy("SEO Title", seoResponse.title) }
        )

        // Description Output Container
        OutputCard(
            title = "SEO High-Converting Description",
            content = seoResponse.description,
            copyTag = "copy_description_button",
            onCopy = { onCopy("SEO Description", seoResponse.description) }
        )

        // Keywords Output Container
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFC3C7D0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exactly 50 Target Keywords/Tags",
                            color = Color(0xFF0061A4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total detected keywords: ${keywordsList.size}",
                            color = Color(0xFF43474E),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { onCopy("SEO Keywords", seoResponse.keywords) },
                        modifier = Modifier
                            .background(Color(0xFFEFF1F8), CircleShape)
                            .size(36.dp)
                            .testTag("copy_keywords_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Keywords",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keywordsList.forEachIndexed { index, keyword ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF1F8), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFC3C7D0)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0061A4))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = keyword,
                                    color = Color(0xFF191C1E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
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
fun OutputCard(
    title: String,
    content: String,
    copyTag: String,
    onCopy: () -> Unit
) {
    var hasCopied by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFC3C7D0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF0061A4),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )

                IconButton(
                    onClick = {
                        onCopy()
                        hasCopied = true
                    },
                    modifier = Modifier
                        .background(
                            if (hasCopied) Color(0xFFD1E4FF) else Color(0xFFEFF1F8),
                            CircleShape
                        )
                        .size(36.dp)
                        .testTag(copyTag)
                ) {
                    Icon(
                        imageVector = if (hasCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Content",
                        tint = if (hasCopied) Color(0xFF001D36) else Color(0xFF0061A4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                color = Color(0xFF191C1E),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
