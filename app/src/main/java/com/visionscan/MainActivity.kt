package com.visionscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.visionscan.ui.home.HomeScreen
import com.visionscan.ui.theme.VisionScanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisionScanTheme {
                HomeScreen()
            }
        }
    }
}
