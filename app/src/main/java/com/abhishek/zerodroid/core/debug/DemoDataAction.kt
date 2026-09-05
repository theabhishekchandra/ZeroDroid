package com.abhishek.zerodroid.core.debug

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.abhishek.zerodroid.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DemoDataViewModel @Inject constructor(private val bus: DemoDataBus) : ViewModel() {
    fun request(route: String) = bus.request(route)
}

/**
 * Top-bar action, debug builds only, that loads sample data into the current
 * screen. Rendered only for routes listed in [DemoData.supportedRoutes].
 */
@Composable
fun DemoDataAction(route: String?) {
    if (!BuildConfig.DEBUG || route == null || route !in DemoData.supportedRoutes) return
    val viewModel: DemoDataViewModel = hiltViewModel()
    IconButton(onClick = { viewModel.request(route) }) {
        Icon(
            imageVector = Icons.Default.Science,
            contentDescription = "Load demo data",
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}
