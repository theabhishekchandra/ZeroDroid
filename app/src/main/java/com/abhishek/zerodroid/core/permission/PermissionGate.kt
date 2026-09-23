package com.abhishek.zerodroid.core.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/** A user-facing group of Android permissions, explained in plain words. */
internal enum class PermissionKind(val label: String, val why: String, val icon: ImageVector) {
    NEARBY("Nearby devices", "Lets the tool find and connect to Bluetooth, WiFi and UWB devices near you.", ZdIcons.Bluetooth),
    LOCATION("Location", "Android requires it for WiFi, Bluetooth and cell results. Your location is never stored or sent anywhere.", ZdIcons.Tracker),
    CAMERA("Camera", "Used for the live preview only. Nothing is recorded unless you choose to.", ZdIcons.Camera),
    MICROPHONE("Microphone", "Used to measure sound frequencies while the tool runs. Audio is never saved.", ZdIcons.Waveform),
    PHONE("Phone", "Reads cell network details such as tower IDs and signal. It can’t see calls or messages.", ZdIcons.CellTower),
    NOTIFICATIONS("Notifications", "Shows a notification while the tool keeps working in the background.", ZdIcons.Bell);

    companion object {
        fun of(permission: String): PermissionKind = when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> LOCATION
            Manifest.permission.CAMERA -> CAMERA
            Manifest.permission.RECORD_AUDIO -> MICROPHONE
            Manifest.permission.READ_PHONE_STATE -> PHONE
            Manifest.permission.POST_NOTIFICATIONS -> NOTIFICATIONS
            else -> NEARBY
        }
    }
}

/**
 * Just-in-time permission screen: explains each permission before Android asks, says what
 * happens on "no", and offers Settings once Android stops showing the prompt.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    permissions: List<String>,
    rationale: String,
    content: @Composable () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(permissions)

    if (permissionState.allPermissionsGranted) {
        content()
    } else {
        PermissionExplainer(permissionState, rationale)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionExplainer(state: MultiplePermissionsState, rationale: String) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var asked by rememberSaveable { mutableStateOf(false) }

    val kinds = state.permissions
        .groupBy { PermissionKind.of(it.permission) }
        .map { (kind, perms) -> kind to perms.all { it.status is PermissionStatus.Granted } }
        .sortedBy { it.first.ordinal }
    val needed = kinds.count { !it.second }
    // After a denial Android may stop showing its prompt; the only way forward is Settings.
    val blocked = asked && !state.shouldShowRationale && !state.allPermissionsGranted

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ZdIconTile(ZdIcons.ShieldCheck, size = 52.dp, iconSize = 26.dp)
                Text(
                    text = when (needed) {
                        1 -> "One permission to start"
                        2 -> "Two permissions to start"
                        else -> "$needed permissions to start"
                    },
                    style = ZdType.Title,
                    color = ZdColors.Text,
                    modifier = Modifier.semantics { heading() }
                )
                Text("$rationale Nothing is used while the tool is stopped.", style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)), color = ZdColors.Text2)
            }

            kinds.forEach { (kind, granted) ->
                ZdCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ZdIconTile(kind.icon, tint = ZdColors.Text, background = ZdColors.Surface3, size = 38.dp, iconSize = 19.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(kind.label, style = ZdType.Label.copy(fontSize = ZdType.Button.fontSize), color = ZdColors.Text)
                            Text(kind.why, style = ZdType.BodySmall, color = ZdColors.Text2)
                        }
                        if (granted) {
                            ZdTag("GRANTED", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
                        } else {
                            ZdTag("NEEDED", color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBorder)
                        }
                    }
                }
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = ZdColors.Text, fontWeight = FontWeight.SemiBold)) {
                        append(if (blocked) "Android won’t ask again: " else "If you say no: ")
                    }
                    append(
                        if (blocked) "turn the permission on in ZeroDroid’s app settings, then come back."
                        else "this tool can’t start. Every tool that doesn’t need these keeps working."
                    )
                },
                style = ZdType.BodySmall,
                color = ZdColors.Text2,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ZdColors.BorderStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (blocked) {
                ZdButton(
                    "Open app settings",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    icon = ZdIcons.Settings,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ZdButton(
                    "Continue",
                    onClick = {
                        asked = true
                        state.launchMultiplePermissionRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ZdButton(
                "Not now",
                onClick = { backDispatcher?.onBackPressed() },
                variant = ZdButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
