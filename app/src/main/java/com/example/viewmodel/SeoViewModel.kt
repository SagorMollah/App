package com.example.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.SeoResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

sealed interface SeoUiState {
    object Idle : SeoUiState
    object Loading : SeoUiState
    data class Success(val response: SeoResponse) : SeoUiState
    data class Error(val message: String) : SeoUiState
}

enum class InputType {
    TEXT, IMAGE, VIDEO
}

data class HistoryItem(
    val timestamp: Long = System.currentTimeMillis(),
    val inputType: InputType,
    val inputDesc: String,
    val title: String,
    val description: String,
    val keywords: String
)

class SeoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SeoUiState>(SeoUiState.Idle)
    val uiState: StateFlow<SeoUiState> = _uiState.asStateFlow()

    private val _inputType = MutableStateFlow(InputType.TEXT)
    val inputType: StateFlow<InputType> = _inputType.asStateFlow()

    // Inputs
    val textPrompt = MutableStateFlow("")
    val imageUri = MutableStateFlow<Uri?>(null)
    val imageBitmap = MutableStateFlow<Bitmap?>(null)
    val presetImageResId = MutableStateFlow<Int?>(null)
    val videoLink = MutableStateFlow("")
    val videoDescription = MutableStateFlow("")
    val videoUri = MutableStateFlow<Uri?>(null)
    val videoFileName = MutableStateFlow<String?>(null)

    // Language selection: "Auto", "English", "Bengali"
    val selectedLanguage = MutableStateFlow("Auto")

    // Generation history (in-memory)
    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val moshi = Moshi.Builder().build()
    private val seoAdapter = moshi.adapter(SeoResponse::class.java)

    fun setInputType(type: InputType) {
        _inputType.value = type
    }

    fun selectPresetImage(resId: Int?, context: Context) {
        presetImageResId.value = resId
        imageUri.value = null
        if (resId != null) {
            try {
                imageBitmap.value = BitmapFactory.decodeResource(context.resources, resId)
            } catch (e: Exception) {
                e.printStackTrace()
                imageBitmap.value = null
            }
        } else {
            imageBitmap.value = null
        }
    }

    fun setImageUri(uri: Uri?, context: Context) {
        imageUri.value = uri
        presetImageResId.value = null
        if (uri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = loadBitmapFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    imageBitmap.value = bitmap
                }
            }
        } else {
            imageBitmap.value = null
        }
    }

    fun setVideoUri(uri: Uri?, name: String?) {
        videoUri.value = uri
        videoFileName.value = name
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun generateSeo(context: Context) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _uiState.value = SeoUiState.Error("API Key is missing! Please enter your Gemini API Key in the AI Studio Secrets panel.")
            return
        }

        _uiState.value = SeoUiState.Loading

        viewModelScope.launch {
            try {
                val request = buildRequest()
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    // Extract JSON in case there is markdown enclosing (like ```json ... ```)
                    val cleanedJson = extractJson(rawText)
                    val seoResponse = withContext(Dispatchers.Default) {
                        seoAdapter.fromJson(cleanedJson)
                    }

                    if (seoResponse != null) {
                        _uiState.value = SeoUiState.Success(seoResponse)
                        // Add to history
                        val desc = getInputSummary()
                        _history.value = listOf(
                            HistoryItem(
                                inputType = _inputType.value,
                                inputDesc = desc,
                                title = seoResponse.title,
                                description = seoResponse.description,
                                keywords = seoResponse.keywords
                            )
                        ) + _history.value
                    } else {
                        _uiState.value = SeoUiState.Error("Failed to parse the response format. Let's try again!")
                    }
                } else {
                    _uiState.value = SeoUiState.Error("Gemini returned an empty response. Please check your inputs.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = SeoUiState.Error("Network error: ${e.localizedMessage ?: "Something went wrong"}")
            }
        }
    }

    private fun getInputSummary(): String {
        return when (_inputType.value) {
            InputType.TEXT -> textPrompt.value.take(50).let { if (it.length >= 50) "$it..." else it }
            InputType.IMAGE -> {
                if (presetImageResId.value != null) "Preset Image Selected" else "Custom Gallery Image"
            }
            InputType.VIDEO -> {
                val link = videoLink.value
                val desc = videoDescription.value
                val file = videoFileName.value
                listOfNotNull(
                    link.takeIf { it.isNotEmpty() }?.let { "Link: $it" },
                    desc.takeIf { it.isNotEmpty() }?.let { "Desc: $it" },
                    file?.let { "File: $it" }
                ).joinToString(", ")
            }
        }
    }

    private fun buildRequest(): GenerateContentRequest {
        val systemInstructionText = """
            You are a professional SEO expert, elite copywriter, and digital marketer. Your goal is to generate extremely high-converting, search-engine-optimized (SEO) metadata based on user inputs.
            Based on the provided input, analyze the content and automatically generate:
            1. An eye-catching, SEO-friendly Title.
            2. A high-converting, SEO-friendly Description (around 150-200 words) with proper hashtags at the end.
            3. Exactly 50 highly relevant SEO Keywords/Tags separated by commas. Do not include numbered lists or formatting inside the keywords field; return them strictly as a single comma-separated string containing exactly 50 words or phrases.

            You MUST return a single valid JSON object containing exactly three keys: 'title', 'description', and 'keywords'.
            JSON format:
            {
              "title": "SEO-friendly Title here",
              "description": "SEO description (150-200 words) with hashtags",
              "keywords": "keyword1, keyword2, keyword3, ..., keyword50"
            }

            IMPORTANT Language Rules:
            - If the user selects Bengali or writes the input in Bengali, you MUST generate all three outputs (Title, Description, and Keywords) in beautiful, fluent, native Bengali.
            - Otherwise, default to English, or match the language of the input text if it is written in another language (e.g. Spanish, Hindi).
            - Always return ONLY valid JSON. No markdown backticks or explanation outside the JSON.
        """.trimIndent()

        val langPromptModifier = when (selectedLanguage.value) {
            "Bengali" -> "\nFORCE OUTPUT LANGUAGE: Bengali (বাংলা)."
            "English" -> "\nFORCE OUTPUT LANGUAGE: English."
            else -> "\nDETECT LANGUAGE: Match the language of the user's input. If it is in Bengali, generate in Bengali."
        }

        val parts = mutableListOf<Part>()

        when (_inputType.value) {
            InputType.TEXT -> {
                val text = textPrompt.value.ifEmpty { "Write SEO tags for an all-in-one digital content creation agency" }
                parts.add(Part(text = "$text$langPromptModifier"))
            }
            InputType.IMAGE -> {
                val bitmap = imageBitmap.value
                val userText = textPrompt.value.ifEmpty { "Analyze this image and generate optimized SEO metadata for it." }
                if (bitmap != null) {
                    val base64Data = bitmap.toBase64()
                    parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data)))
                }
                parts.add(Part(text = "$userText$langPromptModifier"))
            }
            InputType.VIDEO -> {
                val link = videoLink.value
                val desc = videoDescription.value
                val fileName = videoFileName.value
                val promptText = buildString {
                    append("Analyze this video input for SEO metadata:\n")
                    if (link.isNotEmpty()) append("- Video link: $link\n")
                    if (desc.isNotEmpty()) append("- Video description/script: $desc\n")
                    if (fileName != null) append("- Local video filename: $fileName\n")
                    append("\nGenerate SEO-optimized content based on these details.")
                    append(langPromptModifier)
                }
                parts.add(Part(text = promptText))
            }
        }

        return GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )
    }

    private fun extractJson(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json")
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```")
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        return text.trim()
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun clearInputs() {
        textPrompt.value = ""
        imageUri.value = null
        imageBitmap.value = null
        presetImageResId.value = null
        videoLink.value = ""
        videoDescription.value = ""
        videoUri.value = null
        videoFileName.value = null
        _uiState.value = SeoUiState.Idle
    }
}
