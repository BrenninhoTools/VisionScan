package com.visionscan.model

import java.io.File

data class ScanDocument(
    val file: File,
    val name: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val modifiedAt: Long
)
