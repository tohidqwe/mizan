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
    // Legacy Leitner fields retained so an installed v0.1 database migrates without losing progress.
    val reviewBox: Int = 1,
    val nextReviewEpochDay: Long = Long.MAX_VALUE,
    val lastReviewEpochDay: Long? = null,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val masteryLevel: String = "New",
    val note: String = "",
    val favorite: Boolean = false,
    // Strict 140-day exam mode. A single user action activates the review cycle.
    val reviewEnabled: Boolean = false,
    val strictReviewStage: Int = 0,
    val explicitMastered: Boolean = false,
    val firstStudiedEpochDay: Long? = null,
)

@Entity(tableName = "study_cards")
data class StudyCardEntity(
    @PrimaryKey val id: String,
    val domain: String, // TRADE, FIQH, VOCAB, MOCK, ERROR
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

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val dayNumber: Int,
    val completedMask: Int = 0,
    val dayCompleted: Boolean = false,
    val updatedEpochDay: Long,
)

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE topic != 'ماده منسوخ' ORDER BY articleNumber")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY articleNumber")
    fun observeAllIncludingRepealed(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE topic != 'ماده منسوخ' AND reviewEnabled = 1 AND explicitMastered = 0 AND nextReviewEpochDay <= :today ORDER BY nextReviewEpochDay ASC, articleNumber ASC")
    fun observeDue(today: Long): Flow<List<ArticleEntity>>

    @Query("SELECT COUNT(*) FROM articles WHERE topic != 'ماده منسوخ'")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE topic != 'ماده منسوخ' AND reviewEnabled = 1 AND explicitMastered = 0 AND nextReviewEpochDay < :today")
    fun observeOverdueCount(today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun totalCount(): Int

    @Query("SELECT articleNumber FROM articles ORDER BY articleNumber")
    suspend fun allArticleNumbers(): List<Int>

    @Query("SELECT * FROM articles ORDER BY articleNumber")
    suspend fun snapshot(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE articleNumber = :number LIMIT 1")
    suspend fun getByNumber(number: Int): ArticleEntity?

    @Query("SELECT COUNT(*) FROM articles WHERE topic != 'ماده منسوخ' AND reviewEnabled = 1 AND explicitMastered = 0 AND nextReviewEpochDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT * FROM articles WHERE topic != 'ماده منسوخ' AND (CAST(articleNumber AS TEXT) LIKE '%' || :query || '%' OR officialText LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' OR topic LIKE '%' || :query || '%') ORDER BY articleNumber")
    fun search(query: String): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ArticleEntity>)

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

    @Query("SELECT * FROM study_cards WHERE reviewEnabled = 1 AND explicitMastered = 0 AND nextReviewEpochDay <= :today ORDER BY nextReviewEpochDay ASC, domain ASC, ordinal ASC")
    fun observeDue(today: Long): Flow<List<StudyCardEntity>>

    @Query("SELECT COUNT(*) FROM study_cards WHERE reviewEnabled = 1 AND explicitMastered = 0 AND nextReviewEpochDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT COUNT(*) FROM study_cards")
    suspend fun totalCount(): Int

    @Query("SELECT * FROM study_cards ORDER BY domain, ordinal")
    suspend fun snapshot(): List<StudyCardEntity>

    @Query("SELECT * FROM study_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudyCardEntity?

    @Query("SELECT COUNT(*) FROM study_cards WHERE domain = :domain")
    suspend fun domainCount(domain: String): Int

    @Query("SELECT * FROM study_cards WHERE title LIKE '%' || :query || '%' OR prompt LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%' OR explanation LIKE '%' || :query || '%' ORDER BY domain, ordinal")
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

@Database(
    entities = [
        ArticleEntity::class,
        StudyCardEntity::class,
        DailyProgressEntity::class,
        LegalDocumentEntity::class,
        LegalArticleEntity::class,
        VocabItemEntity::class,
        ReminderEntity::class,
        PlannerTaskEntity::class,
        AppSettingEntity::class,
        LocalEntitlementEntity::class,
        InboxMessageEntity::class,
        ClientCaseEntity::class,
        CaseTimelineEntity::class,
        StudySpeedEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun studyCardDao(): StudyCardDao
    abstract fun planDao(): PlanDao
    abstract fun legalContentDao(): LegalContentDao
    abstract fun vocabDao(): VocabDao
    abstract fun reminderDao(): ReminderDao
    abstract fun plannerDao(): PlannerDao
    abstract fun settingsDao(): SettingsDao
    abstract fun entitlementDao(): EntitlementDao
    abstract fun inboxDao(): InboxDao
    abstract fun clientPortalDao(): ClientPortalDao
    abstract fun studySpeedDao(): StudySpeedDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN reviewEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN strictReviewStage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN explicitMastered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN firstStudiedEpochDay INTEGER")
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_progress (
                        dayNumber INTEGER NOT NULL PRIMARY KEY,
                        completedMask INTEGER NOT NULL,
                        dayCompleted INTEGER NOT NULL,
                        updatedEpochDay INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS legal_documents (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    sourceUrl TEXT NOT NULL,
                    verificationStatus TEXT NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS legal_articles (
                    id TEXT NOT NULL PRIMARY KEY,
                    documentId TEXT NOT NULL,
                    articleNumber INTEGER NOT NULL,
                    articleSuffix TEXT NOT NULL,
                    officialText TEXT NOT NULL,
                    noteText TEXT NOT NULL,
                    legalStatus TEXT NOT NULL,
                    sourceUrl TEXT NOT NULL,
                    verificationStatus TEXT NOT NULL,
                    importantKeywords TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS vocab_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    language TEXT NOT NULL,
                    ordinal INTEGER NOT NULL,
                    term TEXT NOT NULL,
                    meaningFa TEXT NOT NULL,
                    category TEXT NOT NULL,
                    sourceName TEXT NOT NULL,
                    sourceUrl TEXT NOT NULL,
                    verificationStatus TEXT NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS reminders (
                    id TEXT NOT NULL PRIMARY KEY,
                    contentType TEXT NOT NULL,
                    contentId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    scheduledAtMillis INTEGER NOT NULL,
                    intervalCode TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    lastTriggeredAtMillis INTEGER,
                    reviewCount INTEGER NOT NULL,
                    soundUri TEXT,
                    vibrationEnabled INTEGER NOT NULL,
                    notificationEnabled INTEGER NOT NULL,
                    alarmEnabled INTEGER NOT NULL,
                    status TEXT NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS planner_tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    scheduledAtMillis INTEGER NOT NULL,
                    remindAtMillis INTEGER,
                    soundUri TEXT,
                    alarmEnabled INTEGER NOT NULL,
                    notificationEnabled INTEGER NOT NULL,
                    completed INTEGER NOT NULL,
                    recurrence TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS app_settings (
                    key TEXT NOT NULL PRIMARY KEY,
                    value TEXT NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS local_entitlements (
                    scope TEXT NOT NULL PRIMARY KEY,
                    planType TEXT NOT NULL,
                    startsAtMillis INTEGER NOT NULL,
                    endsAtMillis INTEGER,
                    enabled INTEGER NOT NULL,
                    lastSyncedAtMillis INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS inbox_messages (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    body TEXT NOT NULL,
                    receivedAtMillis INTEGER NOT NULL,
                    openedAtMillis INTEGER,
                    source TEXT NOT NULL,
                    deliveryState TEXT NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS client_cases (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    referenceNo TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    status TEXT NOT NULL,
                    syncedAtMillis INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS case_timeline (
                    id TEXT NOT NULL PRIMARY KEY,
                    caseId TEXT NOT NULL,
                    occurredAtMillis INTEGER NOT NULL,
                    actionTitle TEXT NOT NULL,
                    details TEXT NOT NULL,
                    visibleToClient INTEGER NOT NULL,
                    syncedAtMillis INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS study_speed (
                    contentType TEXT NOT NULL PRIMARY KEY,
                    averageSeconds REAL NOT NULL,
                    sampleCount INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )""".trimIndent())
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

    /**
     * Validates the bundled 1335-article dataset every startup. Missing rows are repaired with IGNORE
     * semantics so a repair never overwrites the user's review state, notes, favorites, or mastery.
     */
    suspend fun importBundledSeedAndRepair(context: Context, db: AppDatabase) {
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
                    "Only verified official legal text may be bundled: article $articleNumber has $status"
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
                        keywords = o.optJSONArray("keywords")?.let { a ->
                            (0 until a.length()).joinToString("|") { a.getString(it) }
                        }.orEmpty(),
                        recallQuestion = o.optString("recallQuestion"),
                        twoChoiceQuestion = o.optString("twoChoiceQuestion"),
                        simpleExplanation = o.optString("simpleExplanation"),
                        analyticalPoint = o.optString("analyticalPoint"),
                        importantPoints = o.optString("importantPoints"),
                        relatedArticles = o.optJSONArray("relatedArticles")?.let { a ->
                            (0 until a.length()).joinToString(",") { a.getInt(it).toString() }
                        }.orEmpty(),
                        source1 = source1,
                        source2 = source2,
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
                recallQuestion = it.recallQuestion,
                twoChoiceQuestion = it.twoChoiceQuestion,
                simpleExplanation = it.simpleExplanation,
                analyticalPoint = it.analyticalPoint,
                importantPoints = it.importantPoints,
                relatedArticles = it.relatedArticles,
                source1 = it.source1,
                source2 = it.source2,
                verificationStatus = it.verificationStatus,
                verificationDate = it.verificationDate,
            )
        })
        require(db.articleDao().totalCount() == 1335) {
            "Civil Code startup integrity check failed after non-destructive repair"
        }
    }
}

object StudyCardImporter {
    private val bundledAssets = listOf(
        "study_cards.json",
        "vocab_cards.json",
        "trade_cards.json",
        "fiqh_cards.json",
    )

    /**
     * Imports every bundled curriculum bank non-destructively. Existing review state is never
     * overwritten; newly generated cards are repaired/added on every application start.
     */
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
                    val domain = o.getString("domain")
                    val ordinal = o.getInt("ordinal")
                    val title = o.getString("title").trim()
                    val prompt = o.getString("prompt").trim()
                    val answer = o.getString("answer").trim()

                    require(id.isNotBlank() && seenIds.add(id)) {
                        "Duplicate/blank study-card id across bundled assets: $id"
                    }
                    require(domain in setOf("TRADE", "FIQH", "VOCAB", "MOCK", "ERROR")) {
                        "Unsupported study-card domain $domain for $id"
                    }
                    require(ordinal > 0 && title.isNotBlank() && prompt.isNotBlank() && answer.isNotBlank()) {
                        "Incomplete bundled study card $id in $assetName"
                    }

                    add(
                        StudyCardEntity(
                            id = id,
                            domain = domain,
                            ordinal = ordinal,
                            title = title,
                            prompt = prompt,
                            answer = answer,
                            explanation = o.optString("explanation").trim(),
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

        require(seenIds.isNotEmpty()) { "No bundled curriculum cards found" }
        db.studyCardDao().deleteNotBundled(seenIds.toList())
    }
}

