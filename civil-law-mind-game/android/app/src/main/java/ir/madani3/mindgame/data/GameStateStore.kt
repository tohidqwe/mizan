package ir.madani3.mindgame.data

import android.content.Context

class GameStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("madani3_game_state", Context.MODE_PRIVATE)

    fun isCompleted(article: Int): Boolean = prefs.getBoolean("done:$article", false)
    fun stars(article: Int): Int = prefs.getInt("stars:$article", 0)
    fun mistakes(article: Int): Int = prefs.getInt("mistakes:$article", 0)

    fun complete(article: Int, mistakes: Int) {
        val stars = when {
            mistakes == 0 -> 3
            mistakes <= 2 -> 2
            else -> 1
        }
        prefs.edit()
            .putBoolean("done:$article", true)
            .putInt("stars:$article", maxOf(stars(article), stars))
            .putInt("mistakes:$article", minOf(mistakes(article).takeIf { it > 0 } ?: mistakes, mistakes))
            .apply()
    }

    fun unlocked(article: Int): Boolean =
        article == 183 || isCompleted(article - 1)

    fun completedCount(): Int = (183..232).count { isCompleted(it) }
    fun totalStars(): Int = (183..232).sumOf { stars(it) }
    fun xp(): Int = (183..232).sumOf { if (isCompleted(it)) 100 + stars(it) * 20 else 0 }
}
