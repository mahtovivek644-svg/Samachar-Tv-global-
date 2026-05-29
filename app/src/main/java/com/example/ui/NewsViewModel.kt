package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NewsDatabase
import com.example.data.NewsItem
import com.example.data.NewsRepository
import com.example.data.TranslatedNews
import com.example.data.TranslationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NewsRepository
    
    // UI reactive states
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Full news list from DB
    val allNews: StateFlow<List<NewsItem>>

    // Live video simulation controls
    private val _isPlayingLive = MutableStateFlow(true)
    val isPlayingLive: StateFlow<Boolean> = _isPlayingLive.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _currentActiveNews = MutableStateFlow<NewsItem?>(null)
    val currentActiveNews: StateFlow<NewsItem?> = _currentActiveNews.asStateFlow()

    // --- Reporter Profile States with SharedPreferences Persistence ---
    private val sharedPrefs = application.getSharedPreferences("reporter_profile_prefs", android.content.Context.MODE_PRIVATE)

    private val _reporterName = MutableStateFlow(sharedPrefs.getString("reporter_name", "Vivek Mahto") ?: "Vivek Mahto")
    val reporterName: StateFlow<String> = _reporterName.asStateFlow()

    private val _reporterGmail = MutableStateFlow(sharedPrefs.getString("reporter_gmail", "mahtovivek644@gmail.com") ?: "mahtovivek644@gmail.com")
    val reporterGmail: StateFlow<String> = _reporterGmail.asStateFlow()

    private val _reporterContact = MutableStateFlow(sharedPrefs.getString("reporter_contact", "+91 94311 00223") ?: "+91 94311 00223")
    val reporterContact: StateFlow<String> = _reporterContact.asStateFlow()

    private val _reporterEmail = MutableStateFlow(sharedPrefs.getString("reporter_email", "mahtovivek644@gmail.com") ?: "mahtovivek644@gmail.com")
    val reporterEmail: StateFlow<String> = _reporterEmail.asStateFlow()

    private val _reporterPassword = MutableStateFlow(sharedPrefs.getString("reporter_password", "Admin@789") ?: "Admin@789")
    val reporterPassword: StateFlow<String> = _reporterPassword.asStateFlow()

    fun updateProfile(name: String, gmail: String, contact: String, email: String, pass: String) {
        _reporterName.value = name
        _reporterGmail.value = gmail
        _reporterContact.value = contact
        _reporterEmail.value = email
        _reporterPassword.value = pass

        sharedPrefs.edit().apply {
            putString("reporter_name", name)
            putString("reporter_gmail", gmail)
            putString("reporter_contact", contact)
            putString("reporter_email", email)
            putString("reporter_password", pass)
            apply()
        }
    }

    // --- System Language Features ---
    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Translation Cache: Language -> (NewsItemId -> TranslatedNews)
    private val _translationCache = MutableStateFlow<Map<String, Map<Int, TranslatedNews>>>(emptyMap())
    val translationCache: StateFlow<Map<String, Map<Int, TranslatedNews>>> = _translationCache.asStateFlow()

    // Track active network requests per item to show beautiful loaders: "LanguageName_NewsItemID"
    private val _translationInFlight = MutableStateFlow<Set<String>>(emptySet())
    val translationInFlight: StateFlow<Set<String>> = _translationInFlight.asStateFlow()

    init {
        val newsDao = NewsDatabase.getDatabase(application).newsDao
        repository = NewsRepository(newsDao)

        // Setup reactive news stream filtered by category combined with selected category
        allNews = repository.allNews
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed default items if empty
        viewModelScope.launch {
            if (repository.getCount() == 0) {
                seedInitialNews()
            } else {
                // If already seeded, set the first item in database as active broadcast item
                repository.allNews.firstOrNull()?.firstOrNull()?.let {
                    _currentActiveNews.value = it
                }
            }
        }
    }

    private suspend fun seedInitialNews() {
        val initialItems = listOf(
            NewsItem(
                title = "Breaking News Jharkhand",
                category = "Jharkhand",
                subtitle = "Ranchi smart city builds next-gen multi-lane corridors",
                description = "Ranchi is seeing rapid infrastructure changes with the new heavy traffic corridors and continuous 24/7 power supply grid integration. Over a dozen flyovers are being finished to reduce traffic around key intersections.",
                isLive = true
            ),
            NewsItem(
                title = "India Latest Update",
                category = "National",
                subtitle = "Global digital economies recognize India's instant payment standard",
                description = "India's real-time UPI networks are now supported globally in many sovereign digital financial zones. This advancement eliminates substantial cross-border remittance fees and empowers global travelers directly.",
                isLive = false
            ),
            NewsItem(
                title = "Viral Video News",
                category = "Viral Video",
                subtitle = "Breathtaking cinematic sunset sweeps Hundru Falls tourism",
                description = "Stunning camera work of Jharkhand's natural vistas went viral overnight. Local tourism authorities recorded a historic surge of travelers seeking high-altitude nature walks and state eco-lodges.",
                isLive = false
            ),
            NewsItem(
                title = "Sports News",
                category = "Sports",
                subtitle = "Local dynamic state academy gears up for National Games",
                description = "Jharkhand athletes are breaking tournament records in archery and hockey events. The Department of Youth Affairs announced special funding grids to acquire professional international coach-trainers.",
                isLive = false
            )
        )
        for (item in initialItems) {
            repository.insert(item)
        }
        // Set the first item as default playing
        _currentActiveNews.value = initialItems.first()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun togglePlayLive() {
        _isPlayingLive.value = !_isPlayingLive.value
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun selectNewsForBroadcast(newsItem: NewsItem) {
        _currentActiveNews.value = newsItem
        // Auto-resume play states when switching stories
        _isPlayingLive.value = true
        // Auto translate selected item if currently in a non-English language
        triggerTranslationOfItem(newsItem, _currentLanguage.value)
    }

    fun changeLanguage(language: String) {
        _currentLanguage.value = language
        // Instantly trigger translation for the active broadcast news item to make translation fluid
        _currentActiveNews.value?.let { active ->
            triggerTranslationOfItem(active, language)
        }
    }

    fun triggerTranslationOfItem(item: NewsItem, language: String) {
        if (language == "English") return // English is original, no translation required
        
        val cacheKey = "${language}_${item.id}"
        val langCache = _translationCache.value[language]
        val isCached = langCache?.containsKey(item.id) == true
        if (isCached) return // Already stored in local memory

        if (_translationInFlight.value.contains(cacheKey)) return // Transfer in-progress already

        _translationInFlight.value = _translationInFlight.value + cacheKey

        viewModelScope.launch {
            try {
                val result = TranslationService.translateNews(
                    title = item.title,
                    subtitle = item.subtitle,
                    description = item.description,
                    targetLanguage = language
                )
                if (result != null) {
                    val currentLangMap = _translationCache.value[language] ?: emptyMap()
                    val updatedLangMap = currentLangMap + (item.id to result)
                    _translationCache.value = _translationCache.value + (language to updatedLangMap)
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching dynamic AI translation to $language", e)
            } finally {
                _translationInFlight.value = _translationInFlight.value - cacheKey
            }
        }
    }

    fun addNewsItem(title: String, category: String, subtitle: String, description: String) {
        viewModelScope.launch {
            val item = NewsItem(
                title = title,
                category = category,
                subtitle = subtitle,
                description = description,
                timestamp = System.currentTimeMillis()
            )
            repository.insert(item)
            
            // Set as active playing story instantly!
            _currentActiveNews.value = item
            // Auto translate the new item if currently in a non-English language locale
            triggerTranslationOfItem(item, _currentLanguage.value)
        }
    }

    fun toggleBookmark(newsItem: NewsItem) {
        viewModelScope.launch {
            val updated = newsItem.copy(isBookmarked = !newsItem.isBookmarked)
            repository.update(updated)
            // If currently active broadcast item is updated, sync its state too
            if (_currentActiveNews.value?.id == newsItem.id) {
                _currentActiveNews.value = updated
            }
        }
    }

    fun deleteNewsItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_currentActiveNews.value?.id == id) {
                _currentActiveNews.value = allNews.value.firstOrNull { it.id != id }
            }
        }
    }
}
