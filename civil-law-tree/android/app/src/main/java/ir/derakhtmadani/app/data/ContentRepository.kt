package ir.derakhtmadani.app.data

import android.content.Context
import ir.derakhtmadani.app.model.CivilArticle
import ir.derakhtmadani.app.model.MindNode
import org.json.JSONArray
import org.json.JSONObject

object ContentRepository {
    fun load(context: Context): List<CivilArticle> {
        val raw = context.assets.open("civil_content.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) add(parseArticle(array.getJSONObject(i)))
        }
    }

    private fun parseArticle(o: JSONObject): CivilArticle = CivilArticle(
        articleKey = o.getString("articleKey"),
        articleNumber = o.getInt("articleNumber"),
        suffix = o.optString("suffix"),
        legalStatus = o.getString("legalStatus"),
        officialText = o.getString("officialText"),
        sourceUrl = o.getString("sourceUrl"),
        sourceLabel = o.getString("sourceLabel"),
        courseId = o.getInt("courseId"),
        courseName = o.getString("courseName"),
        topic = o.getString("topic"),
        subtopic = o.optString("subtopic"),
        title = o.getString("title"),
        approvalStatus = o.getString("approvalStatus"),
        classificationStatus = o.getString("classificationStatus"),
        simpleExplanation = o.optString("simpleExplanation"),
        academicAnalysis = o.optString("academicAnalysis"),
        philosophy = o.optString("philosophy"),
        elements = o.optJSONArray("elements").strings(),
        conditions = o.optJSONArray("conditions").strings(),
        effects = o.optJSONArray("effects").strings(),
        exceptions = o.optJSONArray("exceptions").strings(),
        relatedArticleKeys = o.optJSONArray("relatedArticleKeys").strings(),
        memoryCue = o.optString("memoryCue"),
        examTrap = o.optString("examTrap"),
        activeRecall = o.optJSONArray("activeRecall").strings(),
        mindNodes = o.optJSONArray("mindNodes").mindNodes(),
    )

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return buildList { for (i in 0 until length()) add(getString(i)) }
    }

    private fun JSONArray?.mindNodes(): List<MindNode> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val n = getJSONObject(i)
                add(
                    MindNode(
                        id = n.getString("id"),
                        label = n.getString("label"),
                        type = n.getString("type"),
                        parentId = if (n.isNull("parentId")) null else n.optString("parentId").ifBlank { null },
                    )
                )
            }
        }
    }
}

class StudyStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("civil_law_tree_progress", Context.MODE_PRIVATE)

    fun status(key: String): String = prefs.getString("status:$key", "UNSEEN") ?: "UNSEEN"
    fun setStatus(key: String, value: String) { prefs.edit().putString("status:$key", value).apply() }

    fun isStarred(key: String): Boolean = prefs.getBoolean("star:$key", false)
    fun toggleStar(key: String): Boolean {
        val next = !isStarred(key)
        prefs.edit().putBoolean("star:$key", next).apply()
        return next
    }

    fun studiedCount(keys: List<String>): Int = keys.count { status(it) != "UNSEEN" }
}
