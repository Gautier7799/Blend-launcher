package com.blend.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blend.launcher.data.model.AppModel

@Composable
fun DockBar(
    dockApps: List<AppModel>,
    onAppClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(86.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.22f)) // تأثير Glassmorphism شفاف
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            dockApps.take(4).forEach { app ->
                AppIconView(
                    app = app,
                    onClick = { onAppClick(app.packageName) },
                    showLabel = false
                )
            }
        }
    }
}
