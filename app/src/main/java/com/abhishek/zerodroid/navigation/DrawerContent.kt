package com.abhishek.zerodroid.navigation

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDim
import com.abhishek.zerodroid.ui.theme.TextDim

@Composable
fun DrawerContent(
    currentRoute: String?,
    onScreenSelected: (ZeroDroidScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp)
        ) {
            // Header with terminal prompt style
            Text(
                text = "$ zerodroid_",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = TerminalGreen
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Text(
                text = "  // hardware toolkit v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Dashboard at top
            DrawerItem(
                screen = ZeroDroidScreen.Dashboard,
                isSelected = currentRoute == ZeroDroidScreen.Dashboard.route,
                onClick = { onScreenSelected(ZeroDroidScreen.Dashboard) }
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Grouped by category
            val orderedCategories = listOf(
                ScreenCategory.SENSORS,
                ScreenCategory.WIRELESS,
                ScreenCategory.RF,
                ScreenCategory.NETWORK
            )

            orderedCategories.forEach { category ->
                val screens = (ZeroDroidScreen.byCategory[category] ?: return@forEach)
                    .filter { it != ZeroDroidScreen.Dashboard }

                Text(
                    text = "/* ${category.label.uppercase()} */",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    letterSpacing = 2.sp
                )

                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    DrawerItem(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = { onScreenSelected(screen) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Terminal-styled footer
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "> ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
            Text(
                text = "> Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }
}

@Composable
private fun DrawerItem(
    screen: ZeroDroidScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            TerminalGreen.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "drawerItemBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) TerminalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "drawerItemContent"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        // Active indicator bar on left edge
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(TerminalGreen, TerminalGreenDim)
                            ),
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
