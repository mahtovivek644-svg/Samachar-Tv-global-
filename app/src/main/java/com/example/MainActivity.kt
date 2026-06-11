package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppLocalizer
import com.example.data.NewsItem
import com.example.data.TranslatedNews
import com.example.ui.NewsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NewsAccent
import com.example.ui.theme.NewsRed

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    SamacharMainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Helper to resolve translation for a news item given selected language and VM cache
fun getTranslatedTexts(
    item: NewsItem,
    language: String,
    cache: Map<String, Map<Int, TranslatedNews>>
): TranslatedNews {
    if (language == "English") {
        return TranslatedNews(item.title, item.subtitle, item.description)
    }
    val langMap = cache[language]
    val translated = langMap?.get(item.id)
    return translated ?: TranslatedNews(
        title = item.title,
        subtitle = item.subtitle,
        description = item.description
    )
}

enum class SamacharTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIVE("Live TV", Icons.Default.PlayArrow),
    UPLOAD_PLUS("Post Media", Icons.Default.AddCircle),
    COLONY_PROFILE("Colony & ID", Icons.Default.Person),
    CAMERA_TEMPLATE("On Camera", Icons.Default.Share)
}

@Composable
fun SamacharMainScreen(
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = viewModel()
) {
    val newsList by viewModel.allNews.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isPlayingLive by viewModel.isPlayingLive.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val currentActiveNews by viewModel.currentActiveNews.collectAsStateWithLifecycle()

    // Language state flows
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val translationCache by viewModel.translationCache.collectAsStateWithLifecycle()
    val translationInFlight by viewModel.translationInFlight.collectAsStateWithLifecycle()

    // Reporter profile state flows
    val reporterName by viewModel.reporterName.collectAsStateWithLifecycle()
    val reporterGmail by viewModel.reporterGmail.collectAsStateWithLifecycle()
    val reporterContact by viewModel.reporterContact.collectAsStateWithLifecycle()
    val reporterEmail by viewModel.reporterEmail.collectAsStateWithLifecycle()
    val reporterPassword by viewModel.reporterPassword.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(SamacharTab.HOME) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var expandedCardId by remember { mutableStateOf<Int?>(null) }

    // Categories available for selection
    val categories = listOf("All", "Jharkhand", "National", "Viral Video", "Sports", "Bookmarks")

    // Filter news based on category selection
    val filteredNews = remember(newsList, selectedCategory) {
        when (selectedCategory) {
            "All" -> newsList
            "Bookmarks" -> newsList.filter { it.isBookmarked }
            else -> newsList.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                tonalElevation = 8.dp
            ) {
                SamacharTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFD700),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            selectedTextColor = Color(0xFFFFD700),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                            indicatorColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Always show custom themed Broadcast Header
                BroadcastHeader(
                    currentLanguage = currentLanguage,
                    onLanguageClick = { showLanguagePicker = true },
                    onProfileClick = { activeTab = SamacharTab.COLONY_PROFILE }
                )

                Crossfade(
                    targetState = activeTab,
                    modifier = Modifier.weight(1f),
                    animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
                ) { tab ->
                    when (tab) {
                        SamacharTab.HOME -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Dynamic Marquee Ticker
                                BreakingTicker(
                                    newsList = newsList,
                                    currentLanguage = currentLanguage,
                                    translationCache = translationCache
                                )

                                // Horizontal Categories Chips Scroller
                                CategorySelectionRow(
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    currentLanguage = currentLanguage,
                                    onCategorySelect = { viewModel.selectCategory(it) }
                                )

                                // Main News Stream Feed
                                if (filteredNews.isEmpty()) {
                                    EmptyNewsFeedState(
                                        category = selectedCategory,
                                        currentLanguage = currentLanguage
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .testTag("news_list"),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(filteredNews, key = { it.id }) { newsItem ->
                                            LaunchedEffect(newsItem.id, currentLanguage) {
                                                if (currentLanguage != "English") {
                                                    viewModel.triggerTranslationOfItem(newsItem, currentLanguage)
                                                }
                                            }

                                            NewsCard(
                                                newsItem = newsItem,
                                                isExpanded = expandedCardId == newsItem.id,
                                                isActiveOnScreen = currentActiveNews?.id == newsItem.id,
                                                currentLanguage = currentLanguage,
                                                translationCache = translationCache,
                                                translationInFlight = translationInFlight,
                                                onToggleExpand = {
                                                    expandedCardId = if (expandedCardId == newsItem.id) null else newsItem.id
                                                },
                                                onCastToLive = { viewModel.selectNewsForBroadcast(newsItem) },
                                                onToggleBookmark = { viewModel.toggleBookmark(newsItem) },
                                                onDelete = { viewModel.deleteNewsItem(newsItem.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SamacharTab.LIVE -> {
                            LiveTvTabContent(
                                isPlayingLive = isPlayingLive,
                                isMuted = isMuted,
                                currentActiveNews = currentActiveNews,
                                currentLanguage = currentLanguage,
                                translationCache = translationCache,
                                translationInFlight = translationInFlight,
                                newsList = newsList,
                                onTogglePlay = { viewModel.togglePlayLive() },
                                onToggleMute = { viewModel.toggleMute() }
                            )
                        }

                        SamacharTab.UPLOAD_PLUS -> {
                            MediaUploadTabContent(
                                currentLanguage = currentLanguage,
                                onSubmitReport = { title, category, subtitle, description ->
                                    viewModel.addNewsItem(title, category, subtitle, description)
                                }
                            )
                        }

                        SamacharTab.COLONY_PROFILE -> {
                            ColonyAndProfileTabContent(
                                currentName = reporterName,
                                currentGmail = reporterGmail,
                                currentContact = reporterContact,
                                currentEmail = reporterEmail,
                                currentPassword = reporterPassword,
                                currentLanguage = currentLanguage,
                                onSaveProfile = { name, gmail, contact, email, pass ->
                                    viewModel.updateProfile(name, gmail, contact, email, pass)
                                },
                                onLanguageChanged = { viewModel.changeLanguage(it) }
                            )
                        }

                        SamacharTab.CAMERA_TEMPLATE -> {
                            CameraTemplateTabContent(
                                currentActiveNews = currentActiveNews,
                                currentLanguage = currentLanguage,
                                translationCache = translationCache
                            )
                        }
                    }
                }
            }

            // Floating Quick-Add button visible on HOME screen to push to Post Media
            if (activeTab == SamacharTab.HOME) {
                FloatingActionButton(
                    onClick = { activeTab = SamacharTab.UPLOAD_PLUS },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("add_news_button"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = AppLocalizer.getString("report_news", currentLanguage)
                        )
                        Text(
                            text = AppLocalizer.getString("report_news", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Globe Language Picker Dialog
        if (showLanguagePicker) {
            LanguagePickerDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { viewModel.changeLanguage(it) },
                onDismiss = { showLanguagePicker = false }
            )
        }
    }
}

@Composable
fun BroadcastHeader(
    currentLanguage: String,
    onLanguageClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Custom Signal Tower Icon
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
            Text(
                text = AppLocalizer.getString("app_title", currentLanguage),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Profile / Settings Button
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .testTag("profile_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Reporter Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Globe language translation selector button with glowing index badge
            IconButton(
                onClick = onLanguageClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .testTag("language_selector_button")
            ) {
                CustomLanguageIcon(
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Live Counter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE50914))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = AppLocalizer.getString("hd_live", currentLanguage),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun BroadcastLivePlayer(
    isPlaying: Boolean,
    isMuted: Boolean,
    activeItem: NewsItem?,
    currentLanguage: String,
    translationCache: Map<String, Map<Int, TranslatedNews>>,
    translationInFlight: Set<String>,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit
) {
    val context = LocalContext.current
    // Pulse animation setup
    val infiniteTransition = rememberInfiniteTransition(label = "player_infinite")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Player Screen Graphic
            if (activeItem == null) {
                // Static Offline Feed Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                          drawRect(
                              Brush.verticalGradient(
                                  colors = listOf(Color(0xFF14141A), Color(0xFF09090C))
                              )
                          )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No Stream Feed",
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = AppLocalizer.getString("trans_offline", currentLanguage),
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                // Main Interactive Broadcast Display Canvas
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Custom dynamic background representing broadcasting air waves
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF3B0508), Color(0xFF070709)),
                                        radius = size.maxDimension / 1.5f
                                    )
                                )
                            }
                    ) {
                        val strokeWidth = 3.dp.toPx()
                        val barCount = 18
                        val spacing = size.width / (barCount + 1)
                        
                        // We draw audio stream wave bars pulsing
                        for (i in 0 until barCount) {
                            val xPos = spacing * (i + 1)
                            // Height changes based on play state and wave scale index factor
                            val multiplier = if (isPlaying) {
                                val sinFactor = kotlin.math.sin(i.toDouble() + waveScale * 10f)
                                (sinFactor + 1.2f) / 2.2f
                            } else {
                                0.1f // frozen flat wave bars
                            }
                            
                            val barHeight = (size.height / 2.5f) * multiplier.toFloat()
                            val topY = (size.height / 2f) - (barHeight / 2f)
                            val bottomY = (size.height / 2f) + (barHeight / 2f)
                            
                            drawLine(
                                color = if (isPlaying) Color(0xFFE50914) else Color(0xFF531114),
                                start = androidx.compose.ui.geometry.Offset(xPos, topY),
                                end = androidx.compose.ui.geometry.Offset(xPos, bottomY),
                                strokeWidth = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    // Watermark & Live Overlays
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Flashing Live Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Color.Green else Color.Gray)
                            )
                            Text(
                                text = AppLocalizer.getString("station_ch", currentLanguage),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Logo Graphic
                        Text(
                            text = AppLocalizer.getString("global_hd", currentLanguage),
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    // Resolve news translations dynamically as needed
                    val activeTexts = remember(activeItem, currentLanguage, translationCache) {
                        getTranslatedTexts(activeItem, currentLanguage, translationCache)
                    }
                    val inFlightKey = "${currentLanguage}_${activeItem.id}"
                    val isTranslatingActive = currentLanguage != "English" && 
                            translationInFlight.contains(inFlightKey) &&
                            (translationCache[currentLanguage]?.containsKey(activeItem.id) != true)

                    // Broadcast Ticker bar displaying story content
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 50.dp)
                            .fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE50914), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = AppLocalizer.getString(activeItem.category, currentLanguage).uppercase(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = activeTexts.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isTranslatingActive) {
                                    CircularProgressIndicator(
                                        color = Color.Yellow,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isTranslatingActive) "Translating to ${AppLocalizer.supportedLanguages.find { it.name == currentLanguage }?.localizedName ?: currentLanguage}..." else activeTexts.subtitle,
                                color = if (isTranslatingActive) Color.LightGray else Color.Yellow,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom Player Action Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Play Toggle
                            IconButton(onClick = onTogglePlay) {
                                if (isPlaying) {
                                    // Custom high-fidelity drawn Pause Icon
                                    Canvas(modifier = Modifier.size(16.dp).testTag("pause_icon")) {
                                        val barWidth = size.width * 0.3f
                                        val spacing = size.width * 0.15f
                                        drawRect(
                                            color = Color.White,
                                            topLeft = androidx.compose.ui.geometry.Offset(spacing, 0f),
                                            size = androidx.compose.ui.geometry.Size(barWidth, size.height)
                                        )
                                        drawRect(
                                            color = Color.White,
                                            topLeft = androidx.compose.ui.geometry.Offset(spacing + barWidth + spacing, 0f),
                                            size = androidx.compose.ui.geometry.Size(barWidth, size.height)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Simulate Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp).testTag("play_icon")
                                    )
                                }
                            }
                            // Mute Toggle
                            IconButton(onClick = onToggleMute) {
                                Canvas(modifier = Modifier.size(16.dp).testTag("mute_unmute_icon")) {
                                    // Draw a custom speaker cone box
                                    val path = Path().apply {
                                        moveTo(size.width * 0.1f, size.height * 0.3f)
                                        lineTo(size.width * 0.35f, size.height * 0.3f)
                                        lineTo(size.width * 0.65f, size.height * 0.05f)
                                        lineTo(size.width * 0.65f, size.height * 0.95f)
                                        lineTo(size.width * 0.35f, size.height * 0.7f)
                                        lineTo(size.width * 0.1f, size.height * 0.7f)
                                        close()
                                    }
                                    drawPath(path = path, color = Color.White)
                                    
                                    if (isMuted) {
                                        // Draw close 'X' mute line indicator
                                        drawLine(
                                            color = Color.Red,
                                            start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.35f),
                                            end = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.65f),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                        drawLine(
                                            color = Color.Red,
                                            start = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.35f),
                                            end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.65f),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    } else {
                                        // Draw sound arcs
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -45f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.2f),
                                            size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.6f),
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // Playback duration status with localization and Share action
                        val playLabel = if (isPlaying) {
                            "00:23 | " + AppLocalizer.getString("l_broadcast", currentLanguage)
                        } else {
                            AppLocalizer.getString("p_broadcast", currentLanguage)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val shareText = if (activeItem != null) {
                                        """
                                            🔥 SAMACHAR TV LIVE VIDEOCAST 🔥
                                            Direct Broadcast: ${activeItem.title}
                                            Category: ${activeItem.category.uppercase()}
                                            
                                            🎥 Tuning into regional Jharkhand transmission. Watch with hundreds of other residents live!
                                        """.trimIndent()
                                    } else {
                                        """
                                            🎥 SAMACHAR TV RANCHI BROADCAST 🎥
                                            Tuning live to our primary community news satellite feed!
                                            
                                            👉 Tune in to see your colony logs, public welfare alerts, and live residential broadcasts.
                                        """.trimIndent()
                                    }
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Video Broadcast via")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.size(28.dp).testTag("share_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share live video feed",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(2.dp))

                            Text(
                                text = playLabel,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakingTicker(
    newsList: List<NewsItem>,
    currentLanguage: String,
    translationCache: Map<String, Map<Int, TranslatedNews>>
) {
    // Generate clean news labels for breaking news rolling bar
    val tickerText = remember(newsList, currentLanguage, translationCache) {
        if (newsList.isEmpty()) {
            "★ " + AppLocalizer.getString("app_title", currentLanguage) + " ★ " + AppLocalizer.getString("broadcasting", currentLanguage) + " ★"
        } else {
            val storiesLine = newsList.joinToString(" • ") { item ->
                getTranslatedTexts(item, currentLanguage, translationCache).title.uppercase()
            }
            "★ BREAKING: $storiesLine ★ " + AppLocalizer.getString("app_title", currentLanguage) + " LIVE STATION ★"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE50914))
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = tickerText,
            color = Color.Yellow,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE)
        )
    }
}

@Composable
fun CategorySelectionRow(
    categories: List<String>,
    selectedCategory: String,
    currentLanguage: String,
    onCategorySelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category
            val localizedName = AppLocalizer.getString(category, currentLanguage)
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCategorySelect(category) }
                    .testTag("category_chip_$category"),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp, 
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Text(
                    text = localizedName,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyNewsFeedState(
    category: String,
    currentLanguage: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 40.dp)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        
        val emptyMessage = if (category == "Bookmarks") {
            AppLocalizer.getString("empty_bookmarks", currentLanguage)
        } else {
            val rawEmpty = AppLocalizer.getString("empty_category", currentLanguage)
            if (rawEmpty.contains("this category")) {
                rawEmpty.replace("this category", AppLocalizer.getString(category, currentLanguage))
            } else {
                rawEmpty
            }
        }

        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = AppLocalizer.getString("empty_tip", currentLanguage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
fun NewsCard(
    newsItem: NewsItem,
    isExpanded: Boolean,
    isActiveOnScreen: Boolean,
    currentLanguage: String,
    translationCache: Map<String, Map<Int, TranslatedNews>>,
    translationInFlight: Set<String>,
    onToggleExpand: () -> Unit,
    onCastToLive: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val cardBg = if (isActiveOnScreen) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val outlineBorderColor = if (isActiveOnScreen) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    // Dynamic localization values for active card item
    val texts = remember(newsItem, currentLanguage, translationCache) {
        getTranslatedTexts(newsItem, currentLanguage, translationCache)
    }
    
    val cacheKey = "${currentLanguage}_${newsItem.id}"
    val isTranslating = currentLanguage != "English" && 
            translationInFlight.contains(cacheKey) &&
            (translationCache[currentLanguage]?.containsKey(newsItem.id) != true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("news_card_${newsItem.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, outlineBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TV Icon representing video libraries (As in the original Flutter app's video_library icon!)
                ImageVideoBadge(
                    category = newsItem.category,
                    isActive = isActiveOnScreen
                )

                // Copying Flutter ListTile layouts
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = AppLocalizer.getString(newsItem.category, currentLanguage),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isTranslating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(Color.Yellow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFD4AF37),
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(8.dp)
                                )
                                Text(
                                    text = "AI Translating",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isTranslating) "Generating translation..." else texts.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (isTranslating) "Please hold on..." else texts.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chevron indicating expansion
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse Report" else "Read Story Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Expanded detail section
            if (isExpanded) {
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Full Story Text area
                    Text(
                        text = if (isTranslating) "Reading original story feed from database. Dynamic translated text will load immediately once OpenAI/Gemini processes it..." else texts.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )

                    // Control Ribbon (Cast, Bookmark, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cast Projection Button
                            Button(
                                onClick = onCastToLive,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActiveOnScreen) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isActiveOnScreen) {
                                            AppLocalizer.getString("broadcasting", currentLanguage)
                                        } else {
                                            AppLocalizer.getString("watch_stream", currentLanguage)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Bookmark toggle Button
                            IconButton(onClick = onToggleBookmark) {
                                Icon(
                                    imageVector = if (newsItem.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Bookmark story",
                                    tint = if (newsItem.isBookmarked) NewsRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Share news dispatch Button
                            IconButton(onClick = {
                                val shareText = """
                                    📰 SAMACHAR TV DISPATCH 📰
                                    [${newsItem.category.uppercase()}] ${texts.title}
                                    ${texts.subtitle}
                                    
                                    👉 Read more about this breaking story on our Ranchi residential division channels!
                                """.trimIndent()
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share News via")
                                context.startActivity(shareIntent)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share news story",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Trash Bin delete for removing records
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete broadcast news",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom-drawn high contrast video station play badge
 */
@Composable
fun ImageVideoBadge(
    category: String,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            // Draw a high fidelity video cassette / play folder layout
            val rectWidth = size.width
            val rectHeight = size.height * 0.8f
            
            // Draw video tape rounded rectangle background
            drawRoundRect(
                color = if (isActive) Color.White else NewsRed,
                size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw clean play symbol right triangle in negative space
            val path = Path().apply {
                moveTo(rectWidth * 0.4f, rectHeight * 0.3f)
                lineTo(rectWidth * 0.7f, rectHeight * 0.5f)
                lineTo(rectWidth * 0.4f, rectHeight * 0.7f)
                close()
            }
            drawPath(
                path = path,
                color = if (isActive) NewsRed else Color.White
            )
        }
    }
}

@Composable
fun ReportNewsDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, subtitle: String, description: String) -> Unit
) {
    val context = LocalContext.current

    var headline by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var fullStory by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Jharkhand") }

    val categoriesList = listOf("Jharkhand", "National", "Viral Video", "Sports")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_news_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppLocalizer.getString("new_portal", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close form")
                    }
                }

                Text(
                    text = "Broadcast new dynamic stories straight to the main ticker feed and active live terminal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Inputs
                OutlinedTextField(
                    value = headline,
                    onValueChange = { headline = it },
                    label = { Text(AppLocalizer.getString("headline_label", currentLanguage)) },
                    placeholder = { Text(AppLocalizer.getString("headline_placeholder", currentLanguage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("headline_input")
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text(AppLocalizer.getString("short_summary_label", currentLanguage)) },
                    placeholder = { Text(AppLocalizer.getString("short_summary_placeholder", currentLanguage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection Room
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = AppLocalizer.getString("category_room", currentLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesList.forEach { category ->
                            val isSelected = categorySelection == category
                            val localizedCat = AppLocalizer.getString(category, currentLanguage)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                    .clickable { categorySelection = category }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = localizedCat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = fullStory,
                    onValueChange = { fullStory = it },
                    label = { Text(AppLocalizer.getString("full_story_label", currentLanguage)) },
                    placeholder = { Text(AppLocalizer.getString("full_story_placeholder", currentLanguage)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(AppLocalizer.getString("cancel", currentLanguage))
                    }

                    Button(
                        onClick = {
                            if (headline.isBlank() || fullStory.isBlank()) {
                                Toast.makeText(context, AppLocalizer.getString("err_mandatory", currentLanguage), Toast.LENGTH_SHORT).show()
                            } else {
                                val finalSubtitle = subtitle.ifBlank { AppLocalizer.getString("latest_update", currentLanguage) }
                                onSubmit(headline, categorySelection, finalSubtitle, fullStory)
                                Toast.makeText(context, AppLocalizer.getString("success_published", currentLanguage), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(AppLocalizer.getString("publish", currentLanguage), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("language_picker_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomLanguageIcon(
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "SYSTEM LANGUAGE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Language Picker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select any regional or global language. Gemini translations will run automatically in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val supported = AppLocalizer.supportedLanguages
                    items(supported) { language ->
                        val isSelected = language.name == currentLanguage
                        val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        val containerCol = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                .background(containerCol)
                                .clickable {
                                    onLanguageSelected(language.name)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Monogram badge/icon
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = language.codeOverride,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column {
                                    Text(
                                        text = language.localizedName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (language.name != language.localizedName) {
                                        Text(
                                            text = language.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Language",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomLanguageIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        // Draw outer ring representing world globe bounds
        drawCircle(
            color = tint,
            radius = size.minDimension / 2f,
            style = Stroke(width = strokeWidth)
        )
        // Draw vertical centerline axis
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
            strokeWidth = strokeWidth
        )
        // Draw horizontal centerline axis
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = strokeWidth
        )
        // Draw side-curve lines representing globe longitude arcs
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.25f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun SamacharLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(3.dp, Brush.verticalGradient(colors = listOf(Color(0xFFE50914), Color(0xFF8B0000))), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Draw the globe background with dynamic grids
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            
            // Draw radial glow for the blue globe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF042E3D), Color(0xFF071424), Color(0xFF020408)),
                    center = centerOffset,
                    radius = radius * 0.95f
                ),
                radius = radius * 0.95f
            )

            // Draw longitude rings
            val strokeWidth = 1f.dp.toPx()
            for (i in 1..4) {
                val horizontalWidth = (size.width * 0.95f) * (i / 5f)
                drawArc(
                    color = Color(0x2A3CC1E4),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset((size.width - horizontalWidth) / 2f, size.height * 0.025f),
                    size = androidx.compose.ui.geometry.Size(horizontalWidth, size.height * 0.95f),
                    style = Stroke(width = strokeWidth)
                )
            }
            
            // Draw latitude lines
            for (i in 1..4) {
                val verticalHeight = (size.height * 0.95f) * (i / 5f)
                drawArc(
                    color = Color(0x2A3CC1E4),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.025f, (size.height - verticalHeight) / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.95f, verticalHeight),
                    style = Stroke(width = strokeWidth)
                )
            }

            // Draw stylized white continent shapes representing America and Eurasia
            val pathNorthAmerica = Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.25f)
                lineTo(size.width * 0.45f, size.height * 0.2f)
                lineTo(size.width * 0.52f, size.height * 0.35f)
                lineTo(size.width * 0.4f, size.height * 0.45f)
                lineTo(size.width * 0.3f, size.height * 0.4f)
                close()
            }
            drawPath(path = pathNorthAmerica, color = Color.White.copy(alpha = 0.5f))

            val pathSouthAmerica = Path().apply {
                moveTo(size.width * 0.42f, size.height * 0.48f)
                lineTo(size.width * 0.55f, size.height * 0.52f)
                lineTo(size.width * 0.48f, size.height * 0.75f)
                lineTo(size.width * 0.4f, size.height * 0.58f)
                close()
            }
            drawPath(path = pathSouthAmerica, color = Color.White.copy(alpha = 0.5f))
        }

        // Metallic Red Horizontal Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE50914), 
                            Color(0xFF8B0000)
                        )
                    )
                )
                .border(1.5.dp, Color(0xFFFFD700)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "खबरें हर पल की..",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "समाचार ",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    
                    Text(
                        text = "TV",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Text(
                    text = "Global",
                    color = Color(0xFFFFD700),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporterProfileDialog(
    currentName: String,
    currentGmail: String,
    currentContact: String,
    currentEmail: String,
    currentPassword: String,
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onSave: (name: String, gmail: String, contact: String, email: String, pass: String) -> Unit
) {
    val context = LocalContext.current
    
    var nameInput by remember { mutableStateOf(currentName) }
    var gmailInput by remember { mutableStateOf(currentGmail) }
    var contactInput by remember { mutableStateOf(currentContact) }
    var emailInput by remember { mutableStateOf(currentEmail) }
    var passwordInput by remember { mutableStateOf(currentPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag("reporter_profile_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REPORTER ID PORTAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Portal")
                    }
                }

                // 1. App Logo visual branding
                SamacharLogo()

                Text(
                    text = "समाचार TV Global • Broadcasting Panel Profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Form Input fields
                // Name Field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Reporter Name") },
                    placeholder = { Text("e.g. Vivek Mahto") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Gmail account
                OutlinedTextField(
                    value = gmailInput,
                    onValueChange = { gmailInput = it },
                    label = { Text("Gmail Account") },
                    placeholder = { Text("e.g. yourname@gmail.com") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_gmail_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Contact number
                OutlinedTextField(
                    value = contactInput,
                    onValueChange = { contactInput = it },
                    label = { Text("Contact Phone") },
                    placeholder = { Text("e.g. +91 94311 00223") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_contact_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Standard Contact Email Address
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Contact Email ID") },
                    placeholder = { Text("e.g. reporter@samachartv.com") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_email_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Profile Password
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Portal Access Password") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "HIDE" else "SHOW",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_password_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // 3. Language Selector Row directly inside profile
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Preferred Language",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLocalizer.supportedLanguages.forEach { lang ->
                            val isSelected = currentLanguage == lang.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { onLanguageChanged(lang.name) },
                                label = { Text(lang.localizedName) },
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (nameInput.isBlank() || gmailInput.isBlank() || contactInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                Toast.makeText(context, "All credentials and contact fields are required!", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(nameInput, gmailInput, contactInput, emailInput, passwordInput)
                                Toast.makeText(context, "Profile details configured & saved!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Details")
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. LIVE TV BROADCAST WITH COMMUNITY CHAT CHANNELS
// ==========================================
@Composable
fun LiveTvTabContent(
    isPlayingLive: Boolean,
    isMuted: Boolean,
    currentActiveNews: NewsItem?,
    currentLanguage: String,
    translationCache: Map<String, Map<Int, TranslatedNews>>,
    translationInFlight: Set<String>,
    newsList: List<NewsItem>,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit
) {
    val context = LocalContext.current
    var liveViewerCount by remember { mutableStateOf(142508) }
    
    // Smooth random viewer fluctuations
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            liveViewerCount += (-12..15).random()
        }
    }

    // Interactive rolling chats stream
    val initialComments = remember {
        mutableStateListOf(
            "Vivek Mahto: Johar Jharkhand! Reporting live from Ranchi Station Hub!",
            "Ramesh Oraon: The rain reports near Khelgaon smart highway are highly helpful.",
            "Savitri Devi: Really nice Santali visual translations on top of the broadcast!",
            "Dr. R. K. Mahto: Extremely neat, low latency video feeds in Ashok Nagar too.",
            "Amit Das: Watching live from steel city Jamshedpur!"
        )
    }
    
    val randomUserNames = listOf("Sunita", "Rajesh", "Kanti", "Sora", "Praveen", "Anjali", "Dhiraj", "Sanjay")
    val randomComments = listOf(
        "Excellent high-fidelity streaming!",
        "Jharkhand regional updates have highest speed on Samachar TV! ✨",
        "The smart city drone captures of Ranchi are gorgeous.",
        "Beautiful audio visual wave bar pulse meters.",
        "Sharing these important colony reports immediately with local groups.",
        "Real-time localized translation is magic!",
        "Warm greetings from Dhanbad Press Guild!",
        "Dynamic news ticker looks extremely clean."
    )
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            val user = randomUserNames.random()
            val text = randomComments.random()
            if (initialComments.size > 20) {
                initialComments.removeAt(0)
            }
            initialComments.add("$user: $text")
        }
    }

    var typedComment by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High fidelity simulated media player
        BroadcastLivePlayer(
            isPlaying = isPlayingLive,
            isMuted = isMuted,
            activeItem = currentActiveNews,
            currentLanguage = currentLanguage,
            translationCache = translationCache,
            translationInFlight = translationInFlight,
            onTogglePlay = onTogglePlay,
            onToggleMute = onToggleMute
        )

        // Live Feed indicators and viewer numbers
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text(
                        text = "LIVE BROADCAST SECURE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                Text(
                    text = "👥 $liveViewerCount watching live",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Digital newsroom feedback title
        Text(
            text = "Digital Press Room Chat Box",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        // Frame wrapper card for Scrollable Community Chat
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Comments logs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    initialComments.forEach { chat ->
                        val parts = chat.split(":", limit = 2)
                        val user = parts.getOrNull(0) ?: "Contributor"
                        val msg = parts.getOrNull(1) ?: ""
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "$user:",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (user.contains("Vivek") || user.contains("You")) Color(0xFFE50914) else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            Text(
                                text = msg.trim(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat sender field integration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = typedComment,
                        onValueChange = { typedComment = it },
                        placeholder = { Text("Enter comment to transmission desk...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (typedComment.isNotBlank()) {
                                initialComments.add("You (Press): $typedComment")
                                typedComment = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Submit Feedback",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. STUDIO NEWS DESK & VIDEO TAPE UPLOAD
// ==========================================
@Composable
fun MediaUploadTabContent(
    currentLanguage: String,
    onSubmitReport: (title: String, category: String, subtitle: String, description: String) -> Unit
) {
    val context = LocalContext.current
    var uploadTitle by remember { mutableStateOf("") }
    var uploadSubtitle by remember { mutableStateOf("") }
    var uploadDescription by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Jharkhand") }
    
    // Upload simulating system variables
    var selectedVideoFile by remember { mutableStateOf<String?>(null) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadSuccess by remember { mutableStateOf(false) }

    val mockFiles = listOf(
        "RANCHI_DRONE_SMART_ROAD_GROUND.MP4 (85.2 MB)",
        "DUMKA_SOHRAI_FESTIVAL_CELEBRATION.MOV (34.8 MB)",
        "VIRAL_JOHAR_WATERFALLS_MONSOON_RUSH.MP4 (110 MB)",
        "SPORTS_KHELGAON_STADIUM_FINALS.AVI (76 MB)"
    )

    LaunchedEffect(isUploadingMedia) {
        if (isUploadingMedia) {
            uploadProgress = 0f
            while (uploadProgress < 1.0f) {
                kotlinx.coroutines.delay(120)
                uploadProgress += 0.05f
            }
            isUploadingMedia = false
            uploadSuccess = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SAMACHAR DIGITAL PRESS DESK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Press Ground Report Submission Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uploadTitle,
                    onValueChange = { uploadTitle = it },
                    label = { Text("News Title / Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upload_headline_box"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category selection chipscroller
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Division Topic", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Jharkhand", "National", "Viral Video", "Sports").forEach { division ->
                            val isChosen = division == selectedCat
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedCat = division },
                                label = { Text(division) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uploadSubtitle,
                    onValueChange = { uploadSubtitle = it },
                    label = { Text("Short Subtitle summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upload_subtitle_box"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uploadDescription,
                    onValueChange = { uploadDescription = it },
                    label = { Text("Full Ground Truth Insights / Report Details") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("upload_desc_box"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Live recorded clip attachment drop segment
        Text(
            text = "Camera Roll Recording Upload Card",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(2.dp, Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFFFD700)))),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectedVideoFile = mockFiles.random()
                    uploadSuccess = false
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach Media File",
                    tint = if (selectedVideoFile != null) Color.Green else Color(0xFFFFD700),
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = if (selectedVideoFile != null) "MEDIA TAPE LINKED" else "TAP TO ATTACH RECORDED NEWS VIDEO",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = selectedVideoFile ?: "Trigger camera roll picker (.MP4 / .MOV / .AVI files)",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Media upload status card loader
        if (selectedVideoFile != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "File Attachment: ${selectedVideoFile?.split(" ")?.firstOrNull()}", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    if (isUploadingMedia) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulating server socket transfer...", fontSize = 11.sp, color = Color.Gray)
                            Text("${(uploadProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = uploadProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFFE50914)
                        )
                    } else if (uploadSuccess) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check, 
                                contentDescription = "Ready", 
                                tint = Color.Green, 
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "HD Video successfully stored & indexed!", 
                                color = Color.Green, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { isUploadingMedia = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Initiate Cloud media optimization", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Save submission Dispatch Button
        Button(
            onClick = {
                if (uploadTitle.isBlank() || uploadSubtitle.isBlank() || uploadDescription.isBlank()) {
                    Toast.makeText(context, "All ground truth briefing boxes required!", Toast.LENGTH_SHORT).show()
                } else {
                    var finalHeadline = uploadTitle
                    if (selectedVideoFile != null && uploadSuccess) {
                        finalHeadline = "📹 [VIDEO DISPATCH] $uploadTitle"
                    }
                    onSubmitReport(finalHeadline, selectedCat, uploadSubtitle, uploadDescription)
                    Toast.makeText(context, "Ground truth report published successfully!", Toast.LENGTH_LONG).show()
                    
                    // Reset forms
                    uploadTitle = ""
                    uploadSubtitle = ""
                    uploadDescription = ""
                    selectedVideoFile = null
                    uploadSuccess = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_dispatch_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "COMMISSION DISPATCH INTERNALLY",
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ==========================================
// 3. COLONY FORUM & ANCHOR IDENTIFICATION
// ==========================================
@Composable
fun ColonyAndProfileTabContent(
    currentName: String,
    currentGmail: String,
    currentContact: String,
    currentEmail: String,
    currentPassword: String,
    currentLanguage: String,
    onSaveProfile: (name: String, gmail: String, contact: String, email: String, pass: String) -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Configurable form states
    var nameInput by remember { mutableStateOf(currentName) }
    var gmailInput by remember { mutableStateOf(currentGmail) }
    var contactInput by remember { mutableStateOf(currentContact) }
    var emailInput by remember { mutableStateOf(currentEmail) }
    var passwordInput by remember { mutableStateOf(currentPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Colony Selector states
    val colonies = listOf(
        "Khelgaon Sports Colony, Ranchi",
        "Harmu Housing Press Cooperative",
        "Ashok Nagar Officers Guild",
        "Dhurwa Sector 2 Press Enclave",
        "Bariatu Housing Colony"
    )
    var selectedColony by remember { mutableStateOf(colonies[0]) }

    val colonyBulletins = mapOf(
        "Khelgaon Sports Colony, Ranchi" to listOf(
            "🏘️ Local resident body requested regular park pruning this Sunday.",
            "💡 Smart streetlights installation project reached Block D-12.",
            "💧 Clean drinking water valve checkup scheduled for Wednesday morning."
        ),
        "Harmu Housing Press Cooperative" to listOf(
            "🏘️ Monthly press meet on colony premises will address journalist housing allocations.",
            "🔌 High tension transformer maintenance scheduled between 2PM and 5PM on Saturday.",
            "Special monsoon sanitation operations initiated across all sectors."
        ),
        "Ashok Nagar Officers Guild" to listOf(
            "🏘️ High-speed fiber cabling upgrade successfully completed for Ashok Road #4.",
            "👮 Community guard vigilance booths deployed at main entrance gate.",
            "🌳 Independence tree planting celebration planned by Resident Welfare Association."
        ),
        "Dhurwa Sector 2 Press Enclave" to listOf(
            "🏘️ Road hot-mixing works initiated around the press colony main loop.",
            "💡 Backup community diesel generators verified ahead of rainy season.",
            "🏥 Weekly health check camp scheduled at the community tower."
        ),
        "Bariatu Housing Colony" to listOf(
            "🏘️ Cooperative drainage desilt works completed near the Sector 3 reservoir.",
            "🚨 CCTV smart cameras successfully integrated at entry points.",
            "🏓 Local kids table tennis club inauguration this Friday at 6PM."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic gold identity badge card representing (colony mein profile)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF091424)),
            border = BorderStroke(2.dp, Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFE50914)))),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header badge credentials
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SAMACHAR TV PRESS ID",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "REPORTER CREDENTIAL IDENTITY CARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE50914))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "RANCHI HUB", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile portrait circle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(2.dp, Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person, 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Anchors text listings values mapping
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = currentName,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        
                        Text(
                            text = "Senior Jharkhand Journalist",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "📞 $currentContact",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )

                        Text(
                            text = "📧 $currentEmail",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS : VERIFIED GLOBAL AGENT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                    
                    Text(
                        text = "AFFILIATED NEV: ${selectedColony.split(" ").getOrNull(0) ?: "Ranchi"}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val shareText = """
                            📰 SAMACHAR TV PRESS ID CARD 📰
                            Reporter Name: $currentName
                            Designation: Senior Jharkhand Journalist
                            Contact Hotline: $currentContact
                            Access Email: $currentGmail (RANCHI HUB)
                            Residential Colony Affiliation: $selectedColony
                            Status: VERIFIED PRESS SQUAD ON-DUTY
                        """.trimIndent()
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Profile Via")
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth().testTag("share_profile_id_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Profile Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SHARE PRESS ID PROFILE",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // 🏘️ Colony Selector advisory segment
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "🏘️ DIGITAL SAMACHAR COLONY REGISTRY",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select active residential colony to check regional updates, welfare notices, and community files:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Simulating Horizontal Colony buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colonies.forEach { colonySpot ->
                        val isChosen = selectedColony == colonySpot
                        FilterChip(
                            selected = isChosen,
                            onClick = { selectedColony = colonySpot },
                            label = { Text(colonySpot.split(",").firstOrNull() ?: colonySpot) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Bullet feeds mapping
                colonyBulletins[selectedColony]?.forEach { bullet ->
                    Text(
                        text = bullet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        // Editable Details Configurator Form Inside the colony and profile page (colony mein profile!)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "⚙️ UPDATE PORTAL BIO DETAILS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("colony_profile_name")
                )

                OutlinedTextField(
                    value = gmailInput,
                    onValueChange = { gmailInput = it },
                    label = { Text("Gmail Access Node") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("colony_profile_gmail")
                )

                OutlinedTextField(
                    value = contactInput,
                    onValueChange = { contactInput = it },
                    label = { Text("Press Contact Hotline") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("colony_profile_contact")
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Internal Press Backup Mail") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("colony_profile_email")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Portal Code") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(text = if (passwordVisible) "HIDE" else "SHOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("colony_profile_password")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (nameInput.isBlank() || gmailInput.isBlank() || contactInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                            Toast.makeText(context, "All biography information inputs are required!", Toast.LENGTH_SHORT).show()
                        } else {
                            onSaveProfile(nameInput, gmailInput, contactInput, emailInput, passwordInput)
                            Toast.makeText(context, "Holographic ID and biography profile stored!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_colony_settings"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE PRESS BIO PROFILE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 4. ON CAMERA VIEWFINDER DYNAMIC PRODUCTION TEMPLATE
// ==========================================
@Composable
fun CameraTemplateTabContent(
    currentActiveNews: NewsItem?,
    currentLanguage: String,
    translationCache: Map<String, Map<Int, TranslatedNews>>
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(15) }
    var activeFilterName by remember { mutableStateOf("Standard Studio HD") }
    var scaleGuidelinesOn by remember { mutableStateOf(true) }
    var recordingTimeOffset by remember { mutableStateOf(0f) }

    // Automated HUD recording increments
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds += 1
            }
        }
    }

    // Blinking REC flasher dot helper
    var showRedDot by remember { mutableStateOf(true) }
    LaunchedEffect(isRecording) {
        while (true) {
            kotlinx.coroutines.delay(500)
            showRedDot = !showRedDot
        }
    }

    // Teleprompter automated slow-scroll helper
    LaunchedEffect(isRecording) {
        while (true) {
            kotlinx.coroutines.delay(90)
            recordingTimeOffset = (recordingTimeOffset - 1f)
            if (recordingTimeOffset < -350f) {
                recordingTimeOffset = 140f
            }
        }
    }

    val counterString = remember(recordingSeconds) {
        val mins = recordingSeconds / 60
        val secs = recordingSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    val activeTranslationItem = remember(currentActiveNews, currentLanguage, translationCache) {
        if (currentActiveNews != null) {
            getTranslatedTexts(currentActiveNews, currentLanguage, translationCache)
        } else {
            TranslatedNews(
                "झारखंड समाचार: रांची विकास प्राधिकरण की नई जन कल्याण योजनाएं शुरू..",
                "LIVE SAMACHAR PRODUCTION VIEW: Anchoring from Ranchi desk for colonies",
                "Anchoring high fidelity studio parameters smoothly"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STUDIO CAMERA DYNAMIC VIEW",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRecording) Color.Red else Color.DarkGray)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isRecording) "LIVE REC" else "STBY CAP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Camera viewfinder overlay screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when (activeFilterName) {
                        "Night Tracker" -> Color(0xFF041E0F)
                        "Warm Cinematic" -> Color(0xFF24150D)
                        "Ranchi Sunset" -> Color(0xFF22091A)
                        else -> Color(0xFF0A101A)
                    }
                )
                .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            // Viewfinder canvas markings guidelines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPixels = 1.dp.toPx()
                val outlineColor = Color.White.copy(alpha = 0.35f)
                
                if (scaleGuidelinesOn) {
                    // Drawing third lines grids
                    drawLine(color = Color.White.copy(alpha = 0.08f), start = androidx.compose.ui.geometry.Offset(size.width * 0.33f, 0f), end = androidx.compose.ui.geometry.Offset(size.width * 0.33f, size.height), strokeWidth = strokeWidthPixels)
                    drawLine(color = Color.White.copy(alpha = 0.08f), start = androidx.compose.ui.geometry.Offset(size.width * 0.66f, 0f), end = androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height), strokeWidth = strokeWidthPixels)
                    drawLine(color = Color.White.copy(alpha = 0.08f), start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.33f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.33f), strokeWidth = strokeWidthPixels)
                    drawLine(color = Color.White.copy(alpha = 0.08f), start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.66f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.66f), strokeWidth = strokeWidthPixels)
                }

                // Drawing solid boundary corner lines
                val markLength = 18.dp.toPx()
                // Top Left
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(8.dp.toPx() + markLength, 8.dp.toPx()), strokeWidth = strokeWidthPixels * 2)
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx() + markLength), strokeWidth = strokeWidthPixels * 2)
                // Top Right
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx() - markLength, 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), 8.dp.toPx()), strokeWidth = strokeWidthPixels * 2)
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), 8.dp.toPx() + markLength), strokeWidth = strokeWidthPixels * 2)
                // Bottom Left
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(8.dp.toPx(), size.height - 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(8.dp.toPx() + markLength, size.height - 8.dp.toPx()), strokeWidth = strokeWidthPixels * 2)
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(8.dp.toPx(), size.height - 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(8.dp.toPx(), size.height - 8.dp.toPx() - markLength), strokeWidth = strokeWidthPixels * 2)
                // Bottom Right
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx() - markLength, size.height - 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), strokeWidth = strokeWidthPixels * 2)
                drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), size.height - 8.dp.toPx() - markLength), strokeWidth = strokeWidthPixels * 2)
            }

            // HUD Details overlay labels
            // REC timer indicators Top Left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRecording) {
                    if (showRedDot) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(text = "REC $counterString", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Red, fontFamily = FontFamily.Monospace)
                } else {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Yellow))
                    Text(text = "STBY MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Yellow, fontFamily = FontFamily.Monospace)
                }
            }

            // Technical HUD metrics Top Right
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "1080P 60FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                Text(text = "H.265 CO-HEVC", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
            }

            // High priority digital watermark in the middle of viewfinder
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SAMACHAR TV GLOBAL",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.12f),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "LIVE ANCHOR PRODUCTION HUD",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.12f)
                )
            }

            // Teleprompter reader overlay scrolling bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(0.85f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .offset(y = recordingTimeOffset.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📜 SMART TELEPROMPTER READ TEXT :",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = activeTranslationItem.title,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeTranslationItem.subtitle,
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Audio track wave meter indicators left side
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .width(16.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(5) { barIndex ->
                    val waveHeight = remember(recordingSeconds, barIndex) { (2..12).random() }
                    Box(
                        modifier = Modifier
                            .height(waveHeight.dp)
                            .width(3.dp)
                            .background(Color.Green)
                    )
                }
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.Green, modifier = Modifier.size(10.dp))
            }

            // Camera Viewfinder Power Metric Bottom Right
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "CAP: [🔋 92%]", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
            }
        }

        // Configuration panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp), 
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Viewfinder Overlay Adjustments", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Standard Studio HD", "Night Tracker", "Warm Cinematic", "Ranchi Sunset").forEach { filterKey ->
                        val isChosen = activeFilterName == filterKey
                        ElevatedFilterChip(
                            selected = isChosen,
                            onClick = { activeFilterName = filterKey },
                            label = { Text(filterKey) }
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Draw Rule of Thirds Guidelines", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = scaleGuidelinesOn,
                        onCheckedChange = { scaleGuidelinesOn = it }
                    )
                }
            }
        }

        // Camera recording action buttons
        Button(
            onClick = {
                if (isRecording) {
                    isRecording = false
                    Toast.makeText(context, "Anchoring mockup recording stored to roll!", Toast.LENGTH_LONG).show()
                    recordingSeconds = 15
                } else {
                    isRecording = true
                    Toast.makeText(context, "Initiated local high-definition record tracker!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("anchor_recorder_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else Color(0xFFE50914)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color.White else Color.Black)
                )
                Text(
                    text = if (isRecording) "COMPLETE STUDY CAPTURE" else "ENGAGE ON-CAMERA RECORDING MOCKOUT",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

