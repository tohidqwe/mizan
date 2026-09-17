package com.mizan.civilleitner.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.time.LocalDate

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val articleNumber: Int,
    val officialText: String,
    val book: String = "",
    val part: String = "",
    val chapter: String = "",
    val section: String = "",
    val topic: String = "",
    val keywords: String = "",
    val recallQuestion: String = "",
    val twoChoiceQuestion: String = "",
    val simpleExplanation: String = "",
    val analyticalPoint: String = "",
    val importantPoints: String = "",
    val relatedArticles: String = "",
    val source1: String,
    val source2: String,
    val verificationStatus: String = "VERIFIED_OFFICIAL",
    val verificationDate: String = "",
    val reviewBox: Int = 1,
    val nextReviewEpochDay: Long = LocalDate.now().toEpochDay(),
    val lastReviewEpochDay: Long? = null,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val masteryLevel: String = "New",
    val note: String = "",
    val favorite: Boolean = false,
)

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY articleNumber")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE nextReviewEpochDay <= :today ORDER BY nextReviewEpochDay ASC, articleNumber ASC")
    fun observeDue(today: Long): Flow<List<ArticleEntity>>

    @Query("SELECT COUNT(*) FROM articles")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE nextReviewEpochDay < :today")
    fun observeOverdueCount(today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE nextReviewEpochDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT * FROM articles WHERE CAST(articleNumber AS TEXT) LIKE '%' || :query || '%' OR officialText LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' OR topic LIKE '%' || :query || '%' ORDER BY articleNumber")
    fun search(query: String): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ArticleEntity>)

    @Update
    suspend fun update(item: ArticleEntity)
}

@Database(entities = [ArticleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "civil-law-leitner.db",
            ).build().also { INSTANCE = it }
        }
    }
}

object VerifiedArticleImporter {
    private val acceptedVerificationStatuses = setOf("VERIFIED_OFFICIAL", "VERIFIED_RRK")

    suspend fun importBundledSeedIfEmpty(context: Context, db: AppDatabase) {
        val dao = db.articleDao()
        if (dao.totalCount() > 0) return

        val raw = context.assets.open("civil_seed.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        require(array.length() == 1335) {
            "Civil Code release gate failed: expected exactly 1335 main articles, got ${array.length()}"
        }

        val records = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val expectedArticleNumber = i + 1
                val articleNumber = o.getInt("articleNumber")
                val status = o.getString("verificationStatus")
                val source1 = o.getString("source1")
                val source2 = o.getString("source2")
                val officialText = o.getString("officialText")

                require(articleNumber == expectedArticleNumber) {
                    "Civil Code release gate failed at index $i: expected article $expectedArticleNumber, got $articleNumber"
                }
                require(status in acceptedVerificationStatuses) {
                    "Only independently verified official legal text may be bundled: article $articleNumber has $status"
                }
                require(officialText.isNotBlank()) { "Official text is blank for article $articleNumber" }
                require(source1.isNotBlank() && source2.isNotBlank()) {
                    "Two provenance references are required for article $articleNumber"
                }

                add(
                    ArticleEntity(
                        articleNumber = articleNumber,
                        officialText = officialText,
                        book = o.optString("book"),
                        part = o.optString("part"),
                        chapter = o.optString("chapter"),
                        section = o.optString("section"),
                        topic = o.optString("topic"),
                        keywords = o.optJSONArray("keywords")?.let { a -> (0 until a.length()).joinToString("|") { a.getString(it) } }.orEmpty(),
                        recallQuestion = o.optString("recallQuestion"),
                        twoChoiceQuestion = o.optString("twoChoiceQuestion"),
                        simpleExplanation = o.optString("simpleExplanation"),
                        analyticalPoint = o.optString("analyticalPoint"),
                        importantPoints = o.optString("importantPoints"),
                        relatedArticles = o.optJSONArray("relatedArticles")?.let { a -> (0 until a.length()).joinToString(",") { a.getInt(it).toString() } }.orEmpty(),
                        source1 = source1,
                        source2 = source2,
                        verificationStatus = status,
                        verificationDate = o.optString("verificationDate"),
                    )
                )
            }
        }
        dao.insertAll(records)
    }
}
