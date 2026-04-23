package com.aca56.cahiersortiecodex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import com.aca56.cahiersortiecodex.app.CahierSortieApp
import com.aca56.cahiersortiecodex.ui.theme.CahierSortieCodexTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val userInteractionEvents = MutableStateFlow(0L)
    private var defaultWindowAnimations: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultWindowAnimations = window.attributes.windowAnimations
        enableEdgeToEdge()
        setContent {
            val appPreferences by (application as CahierSortieApplication)
                .appContainer
                .appPreferencesStore
                .preferencesFlow
                .collectAsState()

            SideEffect {
                window.setWindowAnimations(
                    if (appPreferences.animationsEnabled) defaultWindowAnimations else 0,
                )
            }

            CahierSortieCodexTheme(
                themeMode = appPreferences.themeMode,
                primaryColorHex = appPreferences.primaryColorHex,
                secondaryColorHex = appPreferences.secondaryColorHex,
                tertiaryColorHex = appPreferences.tertiaryColorHex,
            ) {
                CahierSortieApp(
                    userInteractionEvents = userInteractionEvents,
                    onUserInteraction = ::recordUserInteraction,
                )
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        recordUserInteraction()
    }

    private fun recordUserInteraction() {
        userInteractionEvents.value = userInteractionEvents.value + 1L
    }
}
