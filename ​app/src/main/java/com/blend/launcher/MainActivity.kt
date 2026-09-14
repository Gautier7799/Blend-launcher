package com.example.blendlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. نموذج البيانات للتطبيق
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val intent: Intent
)

// 2. ViewModel للتعامل مع البيانات في الخلفية
class LauncherViewModel : ViewModel() {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps

    fun loadApps(context: Context) {
        viewModelScope.launch {
            val apps = fetchAppsFromSystem(context)
            _installedApps.value = apps
        }
    }

    private suspend fun fetchAppsFromSystem(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)

        resolveInfoList.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                AppInfo(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager),
                    intent = launchIntent
                )
            } else null
        }.sortedBy { it.label.lowercase() }
    }
}

// 3. النشاط الرئيسي
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BlendLauncherScreen()
            }
        }
    }
}

// 4. واجهة المستخدم الرئيسية
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendLauncherScreen(viewModel: LauncherViewModel = viewModel()) {
    val context = LocalContext.current
    val apps by viewModel.installedApps.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showAppDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WidgetCard(title = "الطقس", value = "22°", modifier = Modifier.weight(1f))
                WidgetCard(title = "البطارية", value = "85%", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(apps.take(12)) { app ->
                    RealAppIcon(app = app, context = context)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
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

        // شريط المهام السفلي (Dock)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // نعرض أول 4 تطبيقات في شريط المهام كمثال مبدئي
                apps.take(4).forEach { app ->
                    RealAppIcon(app = app, context = context, showLabel = false)
                }
            }
        }
        
        // درج التطبيقات مع ميزة البحث الذكي (Spotlight Search)
        if (showAppDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showAppDrawer = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.95f),
            ) {
                var searchQuery by remember { mutableStateOf("") }
                
                // فلترة التطبيقات لحظياً بناءً على نص البحث
                val filteredApps = remember(searchQuery, apps) {
                    if (searchQuery.isBlank()) apps 
                    else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxHeight(0.85f)) {
                    Text("مكتبة التطبيقات", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // شريط البحث الأنيق (Glassmorphism Search Bar)
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث عن تطبيق...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // عرض التطبيقات المفلترة
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(filteredApps) { app ->
                            RealAppIcon(app = app, context = context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RealAppIcon(app: AppInfo, context: Context, showLabel: Boolean = true) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { 
                app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(app.intent) 
            }
    ) {
        val bitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label, 
                color = Color.White, 
                fontSize = 12.sp, 
                maxLines = 1
            )
        }
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 150
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 150
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
