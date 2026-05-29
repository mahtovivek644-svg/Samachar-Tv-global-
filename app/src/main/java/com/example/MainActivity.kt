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

    var showAddDialog by remember { mutableStateOf(false) }
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

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Dynamic localized Boradcast Header with language pick capabilities
            BroadcastHeader(
                currentLanguage = currentLanguage,
                onLanguageClick = { showLanguagePicker = true }
            )

            // 2. Simulated Live Streaming Video Screen with real-time translation overlays
            BroadcastLivePlayer(
                isPlaying = isPlayingLive,
                isMuted = isMuted,
                activeItem = currentActiveNews,
                currentLanguage = currentLanguage,
                translationCache = translationCache,
                translationInFlight = translationInFlight,
                onTogglePlay = { viewModel.togglePlayLive() },
                onToggleMute = { viewModel.toggleMute() }
            )

            // 3. Indian Style Marquee Breaking News Ticker (fully localized)
            BreakingTicker(
                newsList = newsList,
                currentLanguage = currentLanguage,
                translationCache = translationCache
            )

            // 4. Horizontal Categories Chips Scroller (fully localized)
            CategorySelectionRow(
                categories = categories,
                selectedCategory = selectedCategory,
                currentLanguage = currentLanguage,
                onCategorySelect = { viewModel.selectCategory(it) }
            )

            // 5. Main News Stream Feed
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
                        // Automatically trigger back-end Gemini translation when item is rendered
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

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
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

        // Modal Form Dialog
        if (showAddDialog) {
            ReportNewsDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showAddDialog = false },
                onSubmit = { title, category, subtitle, description ->
                    viewModel.addNewsItem(title, category, subtitle, description)
                    showAddDialog = false
                }
            )
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
    onLanguageClick: () -> Unit
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

                        // Playback duration status with localization
                        val playLabel = if (isPlaying) {
                            "00:23 | " + AppLocalizer.getString("l_broadcast", currentLanguage)
                        } else {
                            AppLocalizer.getString("p_broadcast", currentLanguage)
                        }

                        Text(
                            text = playLabel,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
