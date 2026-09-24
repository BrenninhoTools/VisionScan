package com.visionscan.ui.scanner

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.visionscan.util.findActivity

class DocumentScannerState(private val start: () -> Unit) {
    fun launch() = start()
}

@Composable
fun rememberDocumentScanner(
    onResult: (Uri) -> Unit,
    onError: () -> Unit
): DocumentScannerState {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pdf = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pdf
        if (pdf != null) currentOnResult(pdf.uri) else currentOnError()
    }

    val client = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .build()
        )
    }

    return remember(context, client, launcher) {
        DocumentScannerState {
            val activity = context.findActivity()
            if (activity == null) {
                currentOnError()
                return@DocumentScannerState
            }
            client.getStartScanIntent(activity)
                .addOnSuccessListener { sender ->
                    launcher.launch(IntentSenderRequest.Builder(sender).build())
                }
                .addOnFailureListener { currentOnError() }
        }
    }
}
