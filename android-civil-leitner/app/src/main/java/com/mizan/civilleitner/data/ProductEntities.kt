package com.mizan.civilleitner.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "legal_documents")
data class LegalDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val verificationStatus: String = "VERIFIED_OFFICIAL",
    val updatedAtMillis: Long = 0L,
)

@Entity(tableName = "legal_articles")
data class LegalArticleEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val articleNumber: Int,
    val articleSuffix: String = "",
    val officialText: String,
    val noteText: String = "",
    val legalStatus: String = "ACTIVE",
    val sourceUrl: String,
    val verificationStatus: String = "VERIFIED_OFFICIAL",
    val importantKeywords: String = "",
    val sortOrder: Int,
)

@Entity(tableName = "vocab_items")
data class VocabItemEntity(
    @PrimaryKey val id: String,
    val language: String,
    val ordinal: Int,
    val term: String,
    val meaningFa: String,
    val category: String = "",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val verificationStatus: String = "CURATED",
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val scheduledAtMillis: Long,
    val intervalCode: String,
    val createdAtMillis: Long,
    val lastTriggeredAtMillis: Long? = null,
    val reviewCount: Int = 0,
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val alarmEnabled: Boolean = true,
    val status: String = "ACTIVE",
)

@Entity(tableName = "planner_tasks")
data class PlannerTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val scheduledAtMillis: Long,
    val remindAtMillis: Long? = null,
    val soundUri: String? = null,
    val alarmEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val completed: Boolean = false,
    val recurrence: String = "NONE",
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "local_entitlements")
data class LocalEntitlementEntity(
    @PrimaryKey val scope: String,
    val planType: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long? = null,
    val enabled: Boolean = true,
    val lastSyncedAtMillis: Long = 0L,
)

@Entity(tableName = "inbox_messages")
data class InboxMessageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val receivedAtMillis: Long,
    val openedAtMillis: Long? = null,
    val source: String = "ADMIN",
    val deliveryState: String = "DELIVERED",
)

@Entity(tableName = "client_cases")
data class ClientCaseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val referenceNo: String = "",
    val summary: String = "",
    val status: String = "ACTIVE",
    val syncedAtMillis: Long = 0L,
)

@Entity(tableName = "case_timeline")
data class CaseTimelineEntity(
    @PrimaryKey val id: String,
    val caseId: String,
    val occurredAtMillis: Long,
    val actionTitle: String,
    val details: String = "",
    val visibleToClient: Boolean = true,
    val syncedAtMillis: Long = 0L,
)

@Entity(tableName = "study_speed")
data class StudySpeedEntity(
    @PrimaryKey val contentType: String,
    val averageSeconds: Double,
    val sampleCount: Int,
    val updatedAtMillis: Long,
)

@Dao
interface LegalContentDao {
    @Query("SELECT * FROM legal_documents ORDER BY title")
    fun observeDocuments(): Flow<List<LegalDocumentEntity>>

    @Query("SELECT * FROM legal_articles WHERE documentId = :documentId ORDER BY sortOrder")
    fun observeArticles(documentId: String): Flow<List<LegalArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(items: List<LegalDocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArticles(items: List<LegalArticleEntity>)
}

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab_items WHERE language = :language ORDER BY ordinal")
    fun observeLanguage(language: String): Flow<List<VocabItemEntity>>

    @Query("SELECT COUNT(*) FROM vocab_items WHERE language = :language")
    suspend fun count(language: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<VocabItemEntity>)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY scheduledAtMillis")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND scheduledAtMillis <= :nowMillis ORDER BY scheduledAtMillis")
    fun observeDue(nowMillis: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY scheduledAtMillis")
    suspend fun activeSnapshot(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlannerDao {
    @Query("SELECT * FROM planner_tasks ORDER BY completed, scheduledAtMillis")
    fun observeAll(): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE scheduledAtMillis BETWEEN :startMillis AND :endMillis ORDER BY scheduledAtMillis")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE completed = 0 ORDER BY scheduledAtMillis")
    suspend fun pendingSnapshot(): List<PlannerTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlannerTaskEntity)

    @Query("DELETE FROM planner_tasks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AppSettingEntity)
}

@Dao
interface EntitlementDao {
    @Query("SELECT * FROM local_entitlements ORDER BY scope")
    fun observeAll(): Flow<List<LocalEntitlementEntity>>

    @Query("SELECT * FROM local_entitlements WHERE scope = :scope AND enabled = 1 LIMIT 1")
    suspend fun getEnabled(scope: String): LocalEntitlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<LocalEntitlementEntity>)
}

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_messages ORDER BY receivedAtMillis DESC")
    fun observeAll(): Flow<List<InboxMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InboxMessageEntity)

    @Query("UPDATE inbox_messages SET openedAtMillis = :openedAtMillis WHERE id = :id")
    suspend fun markOpened(id: String, openedAtMillis: Long)
}

@Dao
interface ClientPortalDao {
    @Query("SELECT * FROM client_cases ORDER BY syncedAtMillis DESC")
    fun observeCases(): Flow<List<ClientCaseEntity>>

    @Query("SELECT * FROM case_timeline WHERE caseId = :caseId AND visibleToClient = 1 ORDER BY occurredAtMillis DESC")
    fun observeTimeline(caseId: String): Flow<List<CaseTimelineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCases(items: List<ClientCaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeline(items: List<CaseTimelineEntity>)
}

@Dao
interface StudySpeedDao {
    @Query("SELECT * FROM study_speed WHERE contentType = :contentType LIMIT 1")
    suspend fun get(contentType: String): StudySpeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: StudySpeedEntity)
}
