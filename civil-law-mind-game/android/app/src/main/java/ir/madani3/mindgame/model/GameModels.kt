package ir.madani3.mindgame.model

data class GamePiece(
    val id: String,
    val label: String,
    val targetId: String,
)

data class GameTarget(
    val id: String,
    val label: String,
)

data class LinkNode(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
)

data class LinkEdge(
    val from: String,
    val to: String,
)

data class GameLevel(
    val articleNumber: Int,
    val title: String,
    val world: String,
    val worldIndex: Int,
    val mechanic: String,
    val instruction: String,
    val concept: String,
    val officialText: String,
    val memoryLock: String,
    val pieces: List<GamePiece>,
    val targets: List<GameTarget>,
    val nodes: List<LinkNode>,
    val edges: List<LinkEdge>,
)

data class WorldInfo(
    val index: Int,
    val title: String,
    val subtitle: String,
    val articleFrom: Int,
    val articleTo: Int,
)
