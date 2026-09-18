package ir.derakhtmadani.app.model

data class MindNode(
    val id: String,
    val label: String,
    val type: String,
    val parentId: String?,
)

data class CivilArticle(
    val articleKey: String,
    val articleNumber: Int,
    val suffix: String,
    val legalStatus: String,
    val officialText: String,
    val sourceUrl: String,
    val sourceLabel: String,
    val courseId: Int,
    val courseName: String,
    val topic: String,
    val subtopic: String,
    val title: String,
    val approvalStatus: String,
    val classificationStatus: String,
    val simpleExplanation: String,
    val academicAnalysis: String,
    val philosophy: String,
    val elements: List<String>,
    val conditions: List<String>,
    val effects: List<String>,
    val exceptions: List<String>,
    val relatedArticleKeys: List<String>,
    val memoryCue: String,
    val examTrap: String,
    val activeRecall: List<String>,
    val mindNodes: List<MindNode>,
)

data class CivilCourse(
    val id: Int,
    val name: String,
    val description: String,
)
