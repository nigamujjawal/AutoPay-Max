package com.autopaymax.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

// On-device OCR (no network round trip, no server) for the "import from screenshot"
// add-subscription flow. Feeds ScreenshotOcrParser.
object ScreenshotTextRecognizer {
    suspend fun recognize(context: Context, imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return recognizer.process(image).await().text
    }
}
