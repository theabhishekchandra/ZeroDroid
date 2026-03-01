package com.abhishek.zerodroid.core.ui

import android.app.Activity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.abhishek.zerodroid.R

private const val PREFS_NAME = "zerodroid_prefs"
private const val KEY_ETHICAL_ACCEPTED = "ethical_use_accepted"

@Composable
fun EthicalUseDialog() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    var accepted by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_ETHICAL_ACCEPTED, false)) }

    if (!accepted) {
        AlertDialog(
            onDismissRequest = { /* non-dismissable */ },
            title = {
                Text(
                    text = stringResource(R.string.ethical_title),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.ethical_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean(KEY_ETHICAL_ACCEPTED, true).apply()
                    accepted = true
                }) {
                    Text(
                        text = stringResource(R.string.ethical_accept),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    (context as? Activity)?.finishAffinity()
                }) {
                    Text(
                        text = stringResource(R.string.ethical_decline),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
