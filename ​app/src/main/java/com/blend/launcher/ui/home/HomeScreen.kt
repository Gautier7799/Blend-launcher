package com.blend.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blend.launcher.ui.components.AppIconView
import com.blend.launcher.ui.components.DockBar
import com.blend.launcher.viewmodel.LauncherViewModel

@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.apps.collectAsState()

    val dockApps = apps.take(4)
    val mainGridApps = apps.drop(4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        // شبكة التطبيقات الرئيسية
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 36.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            items(mainGridApps) { app ->
                AppIconView(
                    app = app,
                    onClick = { viewModel.launchApp(app.packageName) }
                )
            }
        }

        // الـ Dock السفلي العائم
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            DockBar(
                dockApps = dockApps,
                onAppClick = { packageName -> viewModel.launchApp(packageName) }
            )
        }
    }
}
