package com.visionscan.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.visionscan.R
import com.visionscan.model.ScanDocument

private const val PDF_MIME_TYPE = "application/pdf"

private fun ScanDocument.contentUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

fun Context.openDocument(document: ScanDocument): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(document.contentUri(this), PDF_MIME_TYPE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun Context.shareDocument(document: ScanDocument) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType(PDF_MIME_TYPE)
        .putExtra(Intent.EXTRA_STREAM, document.contentUri(this))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
}
