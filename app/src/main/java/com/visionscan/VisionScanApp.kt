package com.visionscan

import android.app.Application
import com.visionscan.data.ScanRepository

class VisionScanApp : Application() {

    val scanRepository: ScanRepository by lazy { ScanRepository(this) }
}
