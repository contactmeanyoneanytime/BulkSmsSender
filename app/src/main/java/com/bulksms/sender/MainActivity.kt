// Save at: app/src/main/java/com/bulksms/sender/MainActivity.kt

package com.bulksms.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bulksms.sender.presentation.components.PermissionsHandler
import com.bulksms.sender.presentation.screens.MainScreen
import com.bulksms.sender.presentation.theme.BulkSmsAppTheme
import com.bulksms.sender.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BulkSmsAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(applicationContext)
                    )

                    PermissionsHandler(
                        onPermissionsGranted = {
                            MainScreen(viewModel = viewModel)
                        }
                    )
                }
            }
        }
    }
}

// Add this ViewModel Factory
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}