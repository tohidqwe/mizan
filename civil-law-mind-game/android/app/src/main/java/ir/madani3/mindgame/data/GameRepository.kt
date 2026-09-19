package ir.madani3.mindgame.data

import android.content.Context
import ir.madani3.mindgame.model.GameLevel
import ir.madani3.mindgame.model.GamePiece
import ir.madani3.mindgame.model.GameTarget
import ir.madani3.mindgame.model.LinkEdge
import ir.madani3.mindgame.model.LinkNode
import org.json.JSONArray
import org.json.JSONObject

object GameRepository {
    fun load(context: Context): List<GameLevel> {
        val raw = context.assets.open("madani3_levels.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) add(parse(arr.getJSONObject(i)))
        }.sortedBy { it.articleNumber }
    }

    private fun parse(o: JSONObject): GameLevel = GameLevel(
        articleNumber = o.getInt("articleNumber"),
        title = o.getString("title"),
        world = o.getString("world"),
        worldIndex = o.getInt("worldIndex"),
        mechanic = o.getString("mechanic"),
        instruction = o.getString("instruction"),
        concept = o.getString("concept"),
        officialText = o.getString("officialText"),
        memoryLock = o.getString("memoryLock"),
        pieces = o.optJSONArray("pieces").pieces(),
        targets = o.optJSONArray("targets").targets(),
        nodes = o.optJSONArray("nodes").nodes(),
        edges = o.optJSONArray("edges").edges(),
    )

    private fun JSONArray?.pieces(): List<GamePiece> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val x = getJSONObject(i)
                add(GamePiece(x.getString("id"), x.getString("label"), x.getString("targetId")))
            }
        }
    }

    private fun JSONArray?.targets(): List<GameTarget> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val x = getJSONObject(i)
                add(GameTarget(x.getString("id"), x.getString("label")))
            }
        }
    }

    private fun JSONArray?.nodes(): List<LinkNode> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val x = getJSONObject(i)
                add(
                    LinkNode(
                        id = x.getString("id"),
                        label = x.getString("label"),
                        x = x.getDouble("x").toFloat(),
                        y = x.getDouble("y").toFloat(),
                    )
                )
            }
        }
    }

    private fun JSONArray?.edges(): List<LinkEdge> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val x = getJSONObject(i)
                add(LinkEdge(x.getString("from"), x.getString("to")))
            }
        }
    }
}
