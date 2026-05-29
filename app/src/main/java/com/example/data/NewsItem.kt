package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "news_items")
data class NewsItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // e.g., "Jharkhand", "National", "Viral Video", "Sports"
    val subtitle: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = false,
    val isBookmarked: Boolean = false
)

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_items ORDER BY timestamp DESC")
    fun getAllNews(): Flow<List<NewsItem>>

    @Query("SELECT * FROM news_items WHERE category = :category ORDER BY timestamp DESC")
    fun getNewsByCategory(category: String): Flow<List<NewsItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(newsItem: NewsItem)

    @Update
    suspend fun updateNews(newsItem: NewsItem)

    @Delete
    suspend fun deleteNews(newsItem: NewsItem)

    @Query("DELETE FROM news_items WHERE id = :id")
    suspend fun deleteNewsById(id: Int)

    @Query("SELECT COUNT(*) FROM news_items")
    suspend fun getCount(): Int
}

@Database(entities = [NewsItem::class], version = 1, exportSchema = false)
abstract class NewsDatabase : RoomDatabase() {
    abstract val newsDao: NewsDao

    companion object {
        @Volatile
        private var INSTANCE: NewsDatabase? = null

        fun getDatabase(context: Context): NewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    "samachar_news_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class NewsRepository(private val newsDao: NewsDao) {
    val allNews: Flow<List<NewsItem>> = newsDao.getAllNews()

    fun getNewsByCategory(category: String): Flow<List<NewsItem>> = newsDao.getNewsByCategory(category)

    suspend fun insert(newsItem: NewsItem) = newsDao.insertNews(newsItem)

    suspend fun update(newsItem: NewsItem) = newsDao.updateNews(newsItem)

    suspend fun delete(newsItem: NewsItem) = newsDao.deleteNews(newsItem)

    suspend fun deleteById(id: Int) = newsDao.deleteNewsById(id)

    suspend fun getCount(): Int = newsDao.getCount()
}
