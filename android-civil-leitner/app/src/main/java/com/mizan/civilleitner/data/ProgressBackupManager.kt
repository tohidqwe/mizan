package com.mizan.civilleitner.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class BackupResult(
    val articles: Int,
    val cards: Int,
    val days: Int,
)

object ProgressBackupManager {
    private const val SCHEMA_VERSION = 1

    suspend fun exportTo(context: Context, db: AppDatabase, uri: Uri): BackupResult {
        val articles = db.articleDao().snapshot()
        val cards = db.studyCardDao().snapshot()
        val days = db.planDao().snapshot()

        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("app", "Mizan-PhD1406")
            .put("exportedAt", Instant.now().toString())

        val articleArray = JSONArray()
        articles.forEach { a ->
            articleArray.put(
                JSONObject()
                    .put("articleNumber", a.articleNumber)
                    .put("reviewEnabled", a.reviewEnabled)
                    .put("strictReviewStage", a.strictReviewStage)
                    .put("nextReviewEpochDay", a.nextReviewEpochDay)
                    .putNullable("lastReviewEpochDay", a.lastReviewEpochDay)
                    .put("reviewCount", a.reviewCount)
                    .put("correctCount", a.correctCount)
                    .put("incorrectCount", a.incorrectCount)
                    .put("masteryLevel", a.masteryLevel)
                    .put("note", a.note)
                    .put("favorite", a.favorite)
                    .put("explicitMastered", a.explicitMastered)
                    .putNullable("firstStudiedEpochDay", a.firstStudiedEpochDay)
            )
        }
        root.put("articles", articleArray)

        val cardArray = JSONArray()
        cards.forEach { card ->
            cardArray.put(
                JSONObject()
                    .put("id", card.id)
                    .put("reviewEnabled", card.reviewEnabled)
                    .put("strictReviewStage", card.strictReviewStage)
                    .put("nextReviewEpochDay", card.nextReviewEpochDay)
                    .putNullable("lastReviewEpochDay", card.lastReviewEpochDay)
                    .put("reviewCount", card.reviewCount)
                    .put("explicitMastered", card.explicitMastered)
                    .putNullable("firstStudiedEpochDay", card.firstStudiedEpochDay)
                    .put("note", card.note)
                    .put("favorite", card.favorite)
            )
        }
        root.put("cards", cardArray)

        val dayArray = JSONArray()
        days.forEach { day ->
            dayArray.put(
                JSONObject()
                    .put("dayNumber", day.dayNumber)
                    .put("completedMask", day.completedMask)
                    .put("dayCompleted", day.dayCompleted)
                    .put("updatedEpochDay", day.updatedEpochDay)
            )
        }
        root.put("dailyProgress", dayArray)

        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(root.toString(2))
        } ?: error("Backup output stream could not be opened")

        return BackupResult(articles.size, cards.size, days.size)
    }

    /**
     * Restores progress only. Official legal text and bundled educational content are never read
     * from the backup and therefore can never be overwritten by a stale/user-edited backup.
     */
    suspend fun importFrom(context: Context, db: AppDatabase, uri: Uri): BackupResult {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Backup input stream could not be opened")
        val root = JSONObject(raw)
        require(root.getInt("schemaVersion") == SCHEMA_VERSION) { "Unsupported backup schema" }

        var articleCount = 0
        val articles = root.optJSONArray("articles") ?: JSONArray()
        for (i in 0 until articles.length()) {
            val o = articles.getJSONObject(i)
            val number = o.getInt("articleNumber")
            val current = db.articleDao().getByNumber(number) ?: continue
            db.articleDao().update(
                current.copy(
                    reviewEnabled = o.optBoolean("reviewEnabled", current.reviewEnabled),
                    strictReviewStage = o.optInt("strictReviewStage", current.strictReviewStage).coerceAtLeast(0),
                    nextReviewEpochDay = o.optLong("nextReviewEpochDay", current.nextReviewEpochDay),
                    lastReviewEpochDay = o.optNullableLong("lastReviewEpochDay"),
                    reviewCount = o.optInt("reviewCount", current.reviewCount).coerceAtLeast(0),
                    correctCount = o.optInt("correctCount", current.correctCount).coerceAtLeast(0),
                    incorrectCount = o.optInt("incorrectCount", current.incorrectCount).coerceAtLeast(0),
                    masteryLevel = o.optString("masteryLevel", current.masteryLevel),
                    note = o.optString("note", current.note),
                    favorite = o.optBoolean("favorite", current.favorite),
                    explicitMastered = o.optBoolean("explicitMastered", current.explicitMastered),
                    firstStudiedEpochDay = o.optNullableLong("firstStudiedEpochDay"),
                )
            )
            articleCount++
        }

        var cardCount = 0
        val cards = root.optJSONArray("cards") ?: JSONArray()
        for (i in 0 until cards.length()) {
            val o = cards.getJSONObject(i)
            val id = o.getString("id")
            val current = db.studyCardDao().getById(id) ?: continue
            db.studyCardDao().update(
                current.copy(
                    reviewEnabled = o.optBoolean("reviewEnabled", current.reviewEnabled),
                    strictReviewStage = o.optInt("strictReviewStage", current.strictReviewStage).coerceAtLeast(0),
                    nextReviewEpochDay = o.optLong("nextReviewEpochDay", current.nextReviewEpochDay),
                    lastReviewEpochDay = o.optNullableLong("lastReviewEpochDay"),
                    reviewCount = o.optInt("reviewCount", current.reviewCount).coerceAtLeast(0),
                    explicitMastered = o.optBoolean("explicitMastered", current.explicitMastered),
                    firstStudiedEpochDay = o.optNullableLong("firstStudiedEpochDay"),
                    note = o.optString("note", current.note),
                    favorite = o.optBoolean("favorite", current.favorite),
                )
            )
            cardCount++
        }

        var dayCount = 0
        val days = root.optJSONArray("dailyProgress") ?: JSONArray()
        for (i in 0 until days.length()) {
            val o = days.getJSONObject(i)
            val day = o.getInt("dayNumber")
            if (day !in 1..140) continue
            db.planDao().upsert(
                DailyProgressEntity(
                    dayNumber = day,
                    completedMask = o.optInt("completedMask", 0).coerceAtLeast(0),
                    dayCompleted = o.optBoolean("dayCompleted", false),
                    updatedEpochDay = o.optLong("updatedEpochDay", 0L),
                )
            )
            dayCount++
        }

        return BackupResult(articleCount, cardCount, dayCount)
    }

    private fun JSONObject.putNullable(name: String, value: Long?): JSONObject =
        put(name, value ?: JSONObject.NULL)

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (!has(name) || isNull(name)) null else getLong(name)
}
