package org.liganddraw.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Load Android RDKit shared libs
        try {
            System.loadLibrary("c++_shared")
            System.loadLibrary("RDKitChemDraw")
            System.loadLibrary("GraphMolWrap")
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}