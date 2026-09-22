package com.abhishek.zerodroid.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdCountBadge
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    /** The primary action tab, drawn as a filled pill (Sweep). */
    val emphasized: Boolean = false,
    val showsAlertBadge: Boolean = false
)

@Composable
fun ZdBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    alertCount: Int,
    onSelect: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ZdColors.Surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        ZdDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .selectableGroup()
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == currentRoute
                val badge = if (tab.showsAlertBadge) alertCount else 0
                val color = if (selected) ZdColors.Accent else ZdColors.Text3
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.route) })
                        .semantics {
                            contentDescription = if (badge > 0) "${tab.label}, $badge alerts" else tab.label
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .width(28.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(ZdColors.Accent)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (tab.emphasized) {
                            Box(
                                Modifier
                                    .width(40.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ZdColors.AccentBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(tab.icon, contentDescription = null, tint = ZdColors.Accent, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Box {
                                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                                if (badge > 0) {
                                    ZdCountBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-6).dp))
                                }
                            }
                        }
                        Text(
                            tab.label,
                            style = ZdType.Path.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, letterSpacing = ZdType.Mono.letterSpacing),
                            color = color
                        )
                    }
                }
            }
        }
    }
}
