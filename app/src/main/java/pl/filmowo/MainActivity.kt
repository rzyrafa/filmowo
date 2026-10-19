package pl.filmowo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.withAppLocale
import pl.filmowo.navigation.FilmowoAppRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilmowoTheme {
                FilmowoAppRoot()
            }
        }
    }
}
