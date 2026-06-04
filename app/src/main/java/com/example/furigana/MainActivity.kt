package com.example.furigana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.furigana.ui.theme.FuriganaTheme
import com.example.furigana.ui.theme.composable.CapturedImage
import com.example.furigana.ui.theme.composable.Greeting

private data object RouteA
data object Home
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val backStack = remember { mutableStateListOf<Any>(RouteA) }
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { key ->
                        when (key) {
//                            backStack.add(Home)
                            is RouteA -> NavEntry(key) { FuriganaTheme {Greeting(Modifier.padding(innerPadding),
                                { backStack.add(Home) }) } }

                            is Home -> NavEntry(key) { CapturedImage() }

                            else -> NavEntry(key) { Text("wot") }
                        }
                    }
                )
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    FuriganaTheme {
//        Greeting("Android")
//    }
//}