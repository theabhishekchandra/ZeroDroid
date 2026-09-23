package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.nfc.domain.NfcTagInfo
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun NfcTagCard(
    tag: NfcTagInfo,
    modifier: Modifier = Modifier
) {
    ZdCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tag.tagType, style = ZdType.Heading, color = ZdColors.Text, modifier = Modifier.weight(1f))
            if (tag.ndefMessages.isNotEmpty()) ZdTag("NDEF", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
        }
        ZdStat("UID", tag.uid)
        Row {
            ZdStat("ATQA · SAK", "${tag.atqa ?: "—"} · ${tag.sak ?: "—"}", Modifier.weight(1f))
            ZdStat("Tech", tag.techList.joinToString(" · ") { it.substringAfterLast('.') }, Modifier.weight(1.4f))
        }
        if (tag.ndefMessages.isNotEmpty()) {
            ZdDivider()
            Text("Records · ${tag.ndefMessages.size}", style = ZdType.Label, color = ZdColors.Text2)
            tag.ndefMessages.forEach { content ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZdTag(content.type.name)
                    Text(content.payload, style = ZdType.BodySmall, color = ZdColors.Text, maxLines = 3, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
