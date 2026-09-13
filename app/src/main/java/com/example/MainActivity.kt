package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.AppItem
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.LauncherViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // للوصول للشاشة الكاملة بكل سلاسة
        setContent {
            MyApplicationTheme {
                BlendLauncherScreen(viewModel)
            }
        }
    }
    
    // منع زر الرجوع من إغلاق الـ Launcher
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing
    }
}

@Composable
fun BlendLauncherScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.installedApps.collectAsState()

    // فصل التطبيقات الأساسية (للـ Dock) وباقي التطبيقات
    val dockApps = apps.take(4) 
    val gridApps = if(apps.size > 4) apps.drop(4) else emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // خلفية شفافة مؤقتة لأن الـ Launcher يأخذ خلفية النظام
            .background(Color.Transparent) 
    ) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            // شبكة التطبيقات (Grid System)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 120.dp), // مساحة للـ Dock
                modifier = Modifier.weight(1f)
            ) {
                items(gridApps) { app ->
                    AppIconItem(app = app, onClick = { viewModel.launchApp(app.packageName) })
                }
            }
        }

        // شريط الـ Dock السفلي (بأسلوب iOS)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.3f)) // تأثير Glassmorphism
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.forEach { app ->
                    AppIconItem(
                        app = app, 
                        showLabel = false, // إخفاء النص في الـ Dock كطريقة آبل
                        onClick = { viewModel.launchApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppIconItem(app: AppItem, showLabel: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(60.dp)
                // جعل الأيقونات تأخذ شكل Squircle ناعم
                .clip(RoundedCornerShape(16.dp)), 
            contentScale = ContentScale.Crop
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.label,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                // تأثير الظل الخفيف على النص ليقرأ بوضوح فوق أي خلفية
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}
