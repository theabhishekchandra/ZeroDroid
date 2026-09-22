package com.abhishek.zerodroid.core.ui

import android.app.Activity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.R
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import androidx.core.content.edit

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
                    style = ZdType.Title,
                    color = ZdColors.Text
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.ethical_message),
                    style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)),
                    color = ZdColors.Text2
                )
            },
            confirmButton = {
                ZdButton(
                    text = stringResource(R.string.ethical_accept),
                    onClick = {
                        prefs.edit { putBoolean(KEY_ETHICAL_ACCEPTED, true) }
                        accepted = true
                    }
                )
            },
            dismissButton = {
                ZdButton(
                    text = stringResource(R.string.ethical_decline),
                    onClick = { (context as? Activity)?.finishAffinity() },
                    variant = ZdButtonVariant.Ghost
                )
            },
            containerColor = ZdColors.Surface,
            shape = RoundedCornerShape(22.dp)
        )
    }
}
