package com.seyitali.epubreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seyitali.epubreader.ui.LibraryScreen
import com.seyitali.epubreader.ui.ReaderScreen
import com.seyitali.epubreader.ui.theme.EpubReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ReaderViewModel = viewModel()
            EpubReaderTheme(themeMode = vm.themeMode) {
                App(vm)
            }
        }
    }
}

@Composable
private fun App(vm: ReaderViewModel) {
    if (vm.book == null) {
        LibraryScreen(vm)
    } else {
        ReaderScreen(vm)
        // Geri tuşu okuyucudan kütüphaneye dönsün, uygulamayı kapatmasın
        BackHandler { vm.closeBook() }
    }
}
