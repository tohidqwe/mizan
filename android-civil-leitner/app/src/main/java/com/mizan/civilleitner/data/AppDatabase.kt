package com.mizan.civilleitner.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

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
    val nextReviewEpochDay: Long = Long.MAX_VALUE,
    val lastReviewEpochDay: Long? = null,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val masteryLevel: String = "New",
    val note: String = "",
    val favorite: Boolean = false,
    val reviewEnabled: Boolean = false,
    val strictReviewStage: Int = 0,
    val explicitMastered: Boolean = false,
    val firstStudiedEpochDay: Long? = null,
)

@Entity(tableName = "study_cards")
data class StudyCardEntity(
    @PrimaryKey val id: String,
    val domain: String,
    val ordinal: Int,
    val title: String,
    val prompt: String,
    val answer: String,
    val explanation: String = "",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val verificationStatus: String = "CURATED",
    val reviewEnabled: Boolean = false,
    val strictReviewStage: Int = 0,
    val nextReviewEpochDay: Long = Long.MAX_VALUE,
    val lastReviewEpochDay: Long? = null,
    val reviewCount: Int = 0,
    val explicitMastered: Boolean = false,
    val firstStudiedEpochDay: Long? = null,
    val note: String = "",
    val favorite: Boolean = false,
)

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val dayNumber: Int,
    val completedMask: Int = 0,
    val dayCompleted: Boolean = false,
    val updatedEpochDay: Long,
)

@Entity(
    tableName = "reminders",
    indices = [Index(value = ["targetType", "targetId"]), Index(value = ["dueAtMillis"])],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetType: String,
    val targetId: String,
    val title: String,
    val preview: String,
    val dueAtMillis: Long,
    val intervalHours: Int,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastFiredAtMillis: Long? = null,
    val fireCount: Int = 0,
    val soundUri: String = "",
)

@Entity(
    tableName = "planner_tasks",
    indices = [Index(value = ["dueAtMillis"])],
)
data class PlannerTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val details: String = "",
    val persianDate: String,
    val hour: Int,
    val minute: Int,
    val dueAtMillis: Long,
    val alarmEnabled: Boolean = true,
    val completed: Boolean = false,
    val soundUri: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

data class ArticleContentPatch(
    val articleNumber: Int,
    val officialText: String,
    val book: String,
    val part: String,
    val chapter: String,
    val section: String,
    val topic: String,
    val keywords: String,
    val recallQuestion: String,
    val twoChoiceQuestion: String,
    val simpleExplanation: String,
    val analyticalPoint: String,
    val importantPoints: String,
    val relatedArticles: String,
    val source1: String,
    val source2: String,
    val verificationStatus: String,
    val verificationDate: String,
)

data class StudyCardContentPatch(
    val id: String,
    val domain: String,
    val ordinal: Int,
    val title: String,
    val prompt: String,
    val answer: String,
    val explanation: String,
    val sourceName: String,
    val sourceUrl: String,
    val verificationStatus: String,
)

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY articleNumber")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE articleNumber = :number LIMIT 1")
    suspend fun getByNumber(number: Int): ArticleEntity?

    @Query("SELECT COUNT(*) FROM articles")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun totalCount(): Int

    @Query("SELECT articleNumber FROM articles ORDER BY articleNumber")
    suspend fun allArticleNumbers(): List<Int>

    @Query("SELECT * FROM articles ORDER BY articleNumber")
    suspend fun snapshot(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE CAST(articleNumber AS TEXT) LIKE '%' || :query || '%' OR officialText LIKE '%' || :query || '%' OR topic LIKE '%' || :query || '%' ORDER BY articleNumber")
    fun search(query: String): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(items: List<ArticleEntity>)

    @Update
    suspend fun update(item: ArticleEntity)

    @Update(entity = ArticleEntity::class)
    suspend fun updateContent(items: List<ArticleContentPatch>)
}

@Dao
interface StudyCardDao {
    @Query("SELECT * FROM study_cards ORDER BY domain, ordinal")
    fun observeAll(): Flow<List<StudyCardEntity>>

    @Query("SELECT * FROM study_cards WHERE domain = :domain ORDER BY ordinal")
    fun observeDomain(domain: String): Flow<List<StudyCardEntity>>

    @Query("SELECT COUNT(*) FROM study_cards WHERE domain = :domain")
    fun observeDomainCount(domain: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_cards WHERE domain = :domain")
    suspend fun domainCount(domain: String): Int

    @Query("SELECT * FROM study_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudyCardEntity?

    @Query("SELECT * FROM study_cards ORDER BY domain, ordinal")
    suspend fun snapshot(): List<StudyCardEntity>

    @Query("SELECT * FROM study_cards WHERE title LIKE '%' || :query || '%' OR prompt LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%' ORDER BY domain, ordinal")
    fun search(query: String): Flow<List<StudyCardEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(items: List<StudyCardEntity>)

    @Update(entity = StudyCardEntity::class)
    suspend fun updateContent(items: List<StudyCardContentPatch>)

    @Query("DELETE FROM study_cards WHERE id NOT IN (:ids)")
    suspend fun deleteNotBundled(ids: List<String>)

    @Update
    suspend fun update(item: StudyCardEntity)
}

@Dao
interface PlanDao {
    @Query("SELECT dayNumber FROM daily_progress WHERE dayCompleted = 1 ORDER BY dayNumber")
    fun observeCompletedDays(): Flow<List<Int>>

    @Query("SELECT * FROM daily_progress WHERE dayNumber = :day LIMIT 1")
    fun observeDay(day: Int): Flow<DailyProgressEntity?>

    @Query("SELECT * FROM daily_progress ORDER BY dayNumber")
    suspend fun snapshot(): List<DailyProgressEntity>

    @Query("SELECT dayNumber FROM daily_progress WHERE dayCompleted = 1 ORDER BY dayNumber")
    suspend fun completedDaysSnapshot(): List<Int>

    @Query("SELECT * FROM daily_progress WHERE dayNumber = :day LIMIT 1")
    suspend fun getDay(day: Int): DailyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyProgressEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE active = 1 ORDER BY dueAtMillis")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE active = 1 AND dueAtMillis <= :now ORDER BY dueAtMillis")
    fun observeDue(now: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE active = 1 AND dueAtMillis <= :now ORDER BY dueAtMillis")
    suspend fun dueSnapshot(now: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE active = 1 ORDER BY dueAtMillis")
    suspend fun activeSnapshot(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReminderEntity): Long

    @Update
    suspend fun update(item: ReminderEntity)

    @Query("UPDATE reminders SET active = 0, lastFiredAtMillis = :firedAt, fireCount = fireCount + 1 WHERE id = :id")
    suspend fun markFired(id: Long, firedAt: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlannerTaskDao {
    @Query("SELECT * FROM planner_tasks ORDER BY completed, dueAtMillis")
    fun observeAll(): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE completed = 0 AND alarmEnabled = 1 AND dueAtMillis > :now ORDER BY dueAtMillis")
    suspend fun activeFutureSnapshot(now: Long): List<PlannerTaskEntity>

    @Query("SELECT * FROM planner_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PlannerTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlannerTaskEntity): Long

    @Update
    suspend fun update(item: PlannerTaskEntity)

    @Query("DELETE FROM planner_tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [
        ArticleEntity::class,
        StudyCardEntity::class,
        DailyProgressEntity::class,
        ReminderEntity::class,
        PlannerTaskEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun studyCardDao(): StudyCardDao
    abstract fun planDao(): PlanDao
    abstract fun reminderDao(): ReminderDao
    abstract fun plannerTaskDao(): PlannerTaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN reviewEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN strictReviewStage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN explicitMastered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN firstStudiedEpochDay INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS study_cards (
                        id TEXT NOT NULL PRIMARY KEY,
                        domain TEXT NOT NULL,
                        ordinal INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        prompt TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        verificationStatus TEXT NOT NULL,
                        reviewEnabled INTEGER NOT NULL,
                        strictReviewStage INTEGER NOT NULL,
                        nextReviewEpochDay INTEGER NOT NULL,
                        lastReviewEpochDay INTEGER,
                        reviewCount INTEGER NOT NULL,
                        explicitMastered INTEGER NOT NULL,
                        firstStudiedEpochDay INTEGER,
                        note TEXT NOT NULL,
                        favorite INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_progress (
                        dayNumber INTEGER NOT NULL PRIMARY KEY,
                        completedMask INTEGER NOT NULL,
                        dayCompleted INTEGER NOT NULL,
                        updatedEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        targetType TEXT NOT NULL,
                        targetId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        preview TEXT NOT NULL,
                        dueAtMillis INTEGER NOT NULL,
                        intervalHours INTEGER NOT NULL,
                        active INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        lastFiredAtMillis INTEGER,
                        fireCount INTEGER NOT NULL,
                        soundUri TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_targetType_targetId ON reminders(targetType, targetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_dueAtMillis ON reminders(dueAtMillis)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS planner_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        details TEXT NOT NULL,
                        persianDate TEXT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        dueAtMillis INTEGER NOT NULL,
                        alarmEnabled INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        soundUri TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_planner_tasks_dueAtMillis ON planner_tasks(dueAtMillis)")
            }
        }

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "civil-law-leitner.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
        }
    }
}

object VerifiedArticleImporter {
    private val acceptedVerificationStatuses = setOf("VERIFIED_OFFICIAL", "VERIFIED_RRK")

    suspend fun importBundledSeedAndRepair(context: Context, db: AppDatabase) {
        val raw = context.assets.open("civil_seed.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        require(array.length() == 1335) {
            "Civil Code release gate failed: expected exactly 1335 main articles, got ${array.length()}"
        }
        val records = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val expected = i + 1
                val number = o.getInt("articleNumber")
                val status = o.getString("verificationStatus")
                val text = o.getString("officialText")
                require(number == expected) { "Civil numbering mismatch: expected $expected got $number" }
                require(status in acceptedVerificationStatuses) { "Unverified official Civil Code text: $number" }
                require(text.isNotBlank()) { "Blank Civil Code text: $number" }
                add(
                    ArticleEntity(
                        articleNumber = number,
                        officialText = text,
                        book = o.optString("book"),
                        part = o.optString("part"),
                        chapter = o.optString("chapter"),
                        section = o.optString("section"),
                        topic = o.optString("topic"),
                        keywords = o.optJSONArray("keywords")?.let { a ->
                            (0 until a.length()).joinToString("|") { a.getString(it) }
                        }.orEmpty(),
                        recallQuestion = "",
                        twoChoiceQuestion = "",
                        simpleExplanation = "",
                        analyticalPoint = "",
                        importantPoints = "",
                        relatedArticles = "",
                        source1 = o.optString("source1"),
                        source2 = o.optString("source2"),
                        verificationStatus = status,
                        verificationDate = o.optString("verificationDate"),
                    )
                )
            }
        }
        db.articleDao().insertMissing(records)
        db.articleDao().updateContent(records.map {
            ArticleContentPatch(
                articleNumber = it.articleNumber,
                officialText = it.officialText,
                book = it.book,
                part = it.part,
                chapter = it.chapter,
                section = it.section,
                topic = it.topic,
                keywords = it.keywords,
                recallQuestion = "",
                twoChoiceQuestion = "",
                simpleExplanation = "",
                analyticalPoint = "",
                importantPoints = "",
                relatedArticles = "",
                source1 = it.source1,
                source2 = it.source2,
                verificationStatus = it.verificationStatus,
                verificationDate = it.verificationDate,
            )
        })
        require(db.articleDao().totalCount() == 1335) { "Civil Code integrity check failed" }
    }
}

object StudyCardImporter {
    private val bundledAssets = listOf(
        "study_cards.json",
        "vocab_cards.json",
        "trade_cards.json",
        "fiqh_cards.json",
        "arabic_vocab_cards.json",
    )
    private val acceptedDomains = setOf("TRADE", "FIQH", "VOCAB", "ENGLISH", "ARABIC", "MOCK", "ERROR")

    suspend fun importBundledCardsIfPresent(context: Context, db: AppDatabase) {
        val available = context.assets.list("")?.toSet().orEmpty()
        val seenIds = mutableSetOf<String>()
        for (assetName in bundledAssets) {
            if (assetName !in available) continue
            val raw = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val array = JSONArray(raw)
            val records = buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val id = o.getString("id")
                    val rawDomain = o.getString("domain")
                    val domain = if (rawDomain == "VOCAB" && assetName == "vocab_cards.json") "ENGLISH" else rawDomain
                    val prompt = o.optString("prompt").trim()
                    val answer = o.optString("answer").trim()
                    require(id.isNotBlank() && seenIds.add(id)) { "Duplicate study-card id: $id" }
                    require(domain in acceptedDomains) { "Unsupported study-card domain $domain" }
                    require(prompt.isNotBlank() && answer.isNotBlank()) { "Incomplete study card $id" }
                    add(
                        StudyCardEntity(
                            id = id,
                            domain = domain,
                            ordinal = o.optInt("ordinal", i + 1),
                            title = o.optString("title").trim().ifBlank { prompt },
                            prompt = prompt,
                            answer = answer,
                            explanation = if (domain == "FIQH") o.optString("explanation").trim() else "",
                            sourceName = o.optString("sourceName").trim(),
                            sourceUrl = o.optString("sourceUrl").trim(),
                            verificationStatus = o.optString("verificationStatus", "CURATED").trim(),
                        )
                    )
                }
            }
            db.studyCardDao().insertMissing(records)
            db.studyCardDao().updateContent(records.map {
                StudyCardContentPatch(
                    id = it.id,
                    domain = it.domain,
                    ordinal = it.ordinal,
                    title = it.title,
                    prompt = it.prompt,
                    answer = it.answer,
                    explanation = it.explanation,
                    sourceName = it.sourceName,
                    sourceUrl = it.sourceUrl,
                    verificationStatus = it.verificationStatus,
                )
            })
        }
        require(seenIds.isNotEmpty()) { "No bundled study curriculum found" }
        db.studyCardDao().deleteNotBundled(seenIds.toList())
    }
}
