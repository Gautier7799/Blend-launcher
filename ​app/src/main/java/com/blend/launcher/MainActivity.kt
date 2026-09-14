package com.example.blendlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// إعداد DataStore لحفظ ترتيب التطبيقات
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")
val HOME_APPS_KEY = stringPreferencesKey("home_apps_order")
val DOCK_APPS_KEY = stringPreferencesKey("dock_apps_order")

// نموذج البيانات
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val intent: Intent
)

// ViewModel لمعالجة البيانات والتخزين
class LauncherViewModel : ViewModel() {
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps

    private val _homeApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val homeApps: StateFlow<List<AppInfo>> = _homeApps

    private val _dockApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val dockApps: StateFlow<List<AppInfo>> = _dockApps

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    fun toggleEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun loadApps(context: Context) {
        viewModelScope.launch {
            val apps = fetchAppsFromSystem(context)
            _allApps.value = apps
            loadSavedOrder(context, apps)
        }
    }

    private suspend fun loadSavedOrder(context: Context, apps: List<AppInfo>) {
        val prefs = context.dataStore.data.first()
        val homePackagesStr = prefs[HOME_APPS_KEY]
        val dockPackagesStr = prefs[DOCK_APPS_KEY]

        if (homePackagesStr == null || dockPackagesStr == null) {
            // الإعداد الافتراضي لأول مرة
            _homeApps.value = apps.take(12)
            _dockApps.value = apps.drop(12).take(4)
            saveOrder(context)
        } else {
            // استعادة الترتيب المحفوظ
            val homePkgs = homePackagesStr.split(",").filter { it.isNotEmpty() }
            val dockPkgs = dockPackagesStr.split(",").filter { it.isNotEmpty() }

            _homeApps.value = homePkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
            _dockApps.value = dockPkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
        }
    }

    fun saveOrder(context: Context) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[HOME_APPS_KEY] = _homeApps.value.joinToString(",") { it.packageName }
                prefs[DOCK_APPS_KEY] = _dockApps.value.joinToString(",") { it.packageName }
            }
        }
    }

    fun removeAppFromHome(app: AppInfo, context: Context) {
        _homeApps.value = _homeApps.value.filter { it.packageName != app.packageName }
        saveOrder(context)
    }

    fun removeAppFromDock(app: AppInfo, context: Context) {
        _dockApps.value = _dockApps.value.filter { it.packageName != app.packageName }
        saveOrder(context)
    }

    fun addAppToHome(app: AppInfo, context: Context) {
        if (!_homeApps.value.any { it.packageName == app.packageName }) {
            _homeApps.value = _homeApps.value + app
            saveOrder(context)
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

// النشاط الرئيسي
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendLauncherScreen(viewModel: LauncherViewModel = viewModel()) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val homeApps by viewModel.homeApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showAppDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))))
            .clickable(enabled = isEditMode) { viewModel.toggleEditMode(false) } // الخروج من وضع التعديل عند النقر على الشاشة
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            // زر إنهاء التعديل (يظهر فقط في وضع التعديل)
            if (isEditMode) {
                Button(
                    onClick = { viewModel.toggleEditMode(false) },
                    modifier = Modifier.align(Alignment.End).padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f))
                ) {
                    Text("تم (Done)", color = Color.White)
                }
            } else {
                // الويدجت (تختفي في وضع التعديل لتوفير مساحة)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WidgetCard(title = "الطقس", value = "22°", modifier = Modifier.weight(1f))
                    WidgetCard(title = "البطارية", value = "85%", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // شبكة التطبيقات الرئيسية
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(homeApps) { app ->
                    RealAppIcon(
                        app = app,
                        context = context,
                        isEditMode = isEditMode,
                        onLongClick = { viewModel.toggleEditMode(true) },
                        onClick = {
                            if (isEditMode) viewModel.removeAppFromHome(app, context)
                            else { app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(app.intent) }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (!isEditMode) {
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
                dockApps.take(4).forEach { app ->
                    RealAppIcon(
                        app = app,
                        context = context,
                        showLabel = false,
                        isEditMode = isEditMode,
                        onLongClick = { viewModel.toggleEditMode(true) },
                        onClick = {
                            if (isEditMode) viewModel.removeAppFromDock(app, context)
                            else { app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(app.intent) }
                        }
                    )
                }
            }
        }
        
        // درج التطبيقات (يعمل لإضافة التطبيقات أثناء وضع التعديل)
        if (showAppDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showAppDrawer = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.95f),
            ) {
                var searchQuery by remember { mutableStateOf("") }
                val filteredApps = remember(searchQuery, allApps) {
                    if (searchQuery.isBlank()) allApps 
                    else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxHeight(0.85f)) {
                    Text(
                        text = if (isEditMode) "اختر تطبيقاً لإضافته للشاشة" else "مكتبة التطبيقات", 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث عن تطبيق...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(filteredApps) { app ->
                            RealAppIcon(
                                app = app,
                                context = context,
                                onClick = {
                                    if (isEditMode) {
                                        viewModel.addAppToHome(app, context)
                                        showAppDrawer = false // إغلاق الدرج بعد الإضافة
                                    } else {
                                        app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(app.intent)
                                    }
                                }
                            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealAppIcon(
    app: AppInfo, 
    context: Context, 
    showLabel: Boolean = true,
    isEditMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    // حركة الاهتزاز (Jiggle Animation) لوضع التعديل
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = if (isEditMode) -2f else 0f,
        targetValue = if (isEditMode) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.rotate(rotation)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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

        // أيقونة الحذف في وضع التعديل (تظهر فوق أيقونة التطبيق)
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .offset(x = (-6).dp, y = (-6).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "إزالة", tint = Color.White, modifier = Modifier.size(14.dp))
            }
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
