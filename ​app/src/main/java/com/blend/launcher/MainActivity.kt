package com.example.blendlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BlendLauncherScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendLauncherScreen() {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showAppDrawer by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)) // خلفية متدرجة جذابة
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            // قسم الويدجت (Widgets) - على طريقة iOS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f)) // تأثير زجاجي
                ) {
                    Text("الطقس: 22°", color = Color.White, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Text("البطارية: 85%", color = Color.White, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // شبكة التطبيقات الرئيسية (Home Grid)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(12) { index ->
                    AppIconPlaceholder(name = "تطبيق ${index + 1}")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // مؤشر السحب للأعلى لفتح درج التطبيقات (App Drawer) - قوة أندرويد
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppDrawer = true }
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "App Drawer", tint = Color.White)
                Text("اسحب للأعلى", color = Color.White, fontSize = 12.sp)
            }
        }

        // شريط المهام السفلي (Dock) - على طريقة iOS
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.25f)) // تأثير زجاجي قوي (Glassmorphism)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AppIconPlaceholder(name = "هاتف", showLabel = false)
                AppIconPlaceholder(name = "رسائل", showLabel = false)
                AppIconPlaceholder(name = "متصفح", showLabel = false)
                AppIconPlaceholder(name = "كاميرا", showLabel = false)
            }
        }
        
        // درج التطبيقات (App Drawer)
        if (showAppDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showAppDrawer = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.95f),
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.85f)) {
                    Text("مكتبة التطبيقات", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(40) { index ->
                            AppIconPlaceholder(name = "تطبيق ${index + 1}")
                        }
                    }
                }
            }
        }
    }
}

// مكون مساعد لرسم شكل الأيقونة (Placeholder)
@Composable
fun AppIconPlaceholder(name: String, showLabel: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp)) // شكل مربع بحواف دائرية (Squircle - iOS Style)
                .background(Color.White)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = name, color = Color.White, fontSize = 12.sp, maxLines = 1)
        }
    }
}
