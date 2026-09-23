package com.abhishek.zerodroid.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.prefs.AppSettings
import com.abhishek.zerodroid.core.prefs.ToolPreferences
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** What someone wants ZeroDroid for; each goal pins the tools that serve it. */
enum class Goal(val title: String, val detail: String, val tools: List<String>) {
    ROOM("Check a room", "Hotels, rentals, a new office", listOf("hidden_camera", "rf_bug_sweeper", "bluetooth_tracker")),
    TRACKED("Am I being tracked?", "AirTags, Tiles and other trackers", listOf("bluetooth_tracker", "ble")),
    WIFI("Is this WiFi safe?", "Evil twins, deauth attacks, open networks", listOf("wifi", "rogue_ap", "deauth_detector", "network_scanner")),
    LEARN("Learn how it works", "Radios and sensors inside your phone", listOf("sensors", "ble", "wifi", "nfc"));

    companion object {
        const val MAX_PINS = 6

        /** Tools to pin for the chosen goals, in goal order, without duplicates. */
        fun pinsFor(goals: Collection<Goal>): List<String> =
            entries.filter { it in goals }.flatMap { it.tools }.distinct().take(MAX_PINS)
    }
}

internal val Goal.icon: ImageVector
    get() = when (this) {
        Goal.ROOM -> ZdIcons.Sweep
        Goal.TRACKED -> ZdIcons.Tracker
        Goal.WIFI -> ZdIcons.Wifi
        Goal.LEARN -> ZdIcons.Sensors
    }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: AppSettings,
    private val toolPreferences: ToolPreferences
) : ViewModel() {
    val done = settings.onboardingDone

    fun finish(goals: Set<Goal>) {
        if (goals.isNotEmpty()) toolPreferences.setPinned(Goal.pinsFor(goals))
        settings.setOnboardingDone(true)
    }
}

/** First-run goal picker, shown once after the responsible-use agreement. */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    var picked by rememberSaveable { mutableStateOf(setOf<Goal>()) }
    // Full-bleed background; the content keeps a readable width on tablets.
    Box(Modifier.fillMaxSize().background(ZdColors.Bg), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("zd:~/welcome", style = ZdType.Path, color = ZdColors.Accent)
                Text("What brings you here?", style = ZdType.Title, color = ZdColors.Text)
                Text(
                    "Pick any that apply. We’ll pin the right tools to Home; you can change them any time.",
                    style = ZdType.BodySmall,
                    color = ZdColors.Text2
                )
                Goal.entries.forEach { goal ->
                    val on = goal in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(ZdCardShape)
                            .background(if (on) ZdColors.AccentBg else ZdColors.Surface)
                            .border(1.dp, if (on) ZdColors.Accent else ZdColors.Border, ZdCardShape)
                            .toggleable(value = on, role = Role.Checkbox, onValueChange = { picked = if (on) picked - goal else picked + goal })
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ZdIconTile(goal.icon, tint = if (on) ZdColors.Accent else ZdColors.Text2, background = if (on) ZdColors.Surface else ZdColors.Surface2)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(goal.title, style = ZdType.Label, color = ZdColors.Text)
                            Text(goal.detail, style = ZdType.Caption, color = ZdColors.Text3)
                            Text(
                                goal.tools.mapNotNull { ToolCatalog.forRoute(it)?.name }.joinToString(" · "),
                                style = ZdType.Path,
                                color = ZdColors.Text3
                            )
                        }
                        if (on) Icon(ZdIcons.Check, contentDescription = null, tint = ZdColors.Accent)
                    }
                }
            }
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ZdButton(
                    if (picked.isEmpty()) "Pick at least one" else "Continue",
                    onClick = { viewModel.finish(picked) },
                    enabled = picked.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                ZdButton("Skip for now", onClick = { viewModel.finish(emptySet()) }, variant = ZdButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
