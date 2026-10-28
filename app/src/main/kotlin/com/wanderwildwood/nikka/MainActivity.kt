package com.wanderwildwood.nikka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.nikka.data.Backup
import com.wanderwildwood.nikka.data.Store
import com.wanderwildwood.nikka.sit.Sitting
import java.io.File
import com.wanderwildwood.nikka.ui.PracticeApp
import com.wanderwildwood.nikka.ui.monochrome

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) debugSeed()
        Sitting.load(this)
        setContent {
            // A steady cursor: a blinking one is a repaint twice a second on an e-ink panel
            // for as long as a field has focus.
            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                ThemeMMD(colorScheme = monochrome) {
                    // A debug build names its rows for the emulator tests; a release names none.
                    Box(Modifier.semantics { testTagsAsResourceId = BuildConfig.DEBUG }) {
                        PracticeApp()
                    }
                }
            }
        }
    }

    /**
     * Debug builds only: a backup file named in the launching intent, read from the app's own
     * files and restored, so an emulator test can start from a known log. `BuildConfig.DEBUG` is
     * false in a release, so this is compiled out of it.
     */
    private fun debugSeed() {
        if (!BuildConfig.DEBUG) return
        val name = intent?.getStringExtra("seed") ?: return
        val file = File(filesDir, name)
        if (file.isFile) Store.get(this).replaceAll(Backup.read(file.readText()))
    }
}
