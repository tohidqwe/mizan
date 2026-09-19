package com.mizan.civilleitner.product

data class CourseItem(
    val id: String,
    val title: String,
    val available: Boolean,
    val subscriptionRequired: Boolean = true,
)

object CourseCatalog {
    const val PHD_PRIVATE_LAW = "PHD_PRIVATE_LAW"

    val courses = listOf(
        CourseItem("BACHELOR_ENTRANCE", "آمادگی برای کنکور حقوق ورود به مقطع لیسانس", false),
        CourseItem("BACHELOR_COURSES", "دروس دوره لیسانس حقوق", false),
        CourseItem("BAR_EXAM", "آمادگی برای آزمون وکالت", false),
        CourseItem("JUDICIARY_EXAM", "آمادگی برای آزمون قضاوت", false),
        CourseItem("NOTARY_EXAM", "آمادگی برای آزمون سردفتری", false),
        CourseItem("MASTER_ENTRANCE", "آمادگی برای کنکور کارشناسی ارشد", false),
        CourseItem("MASTER_COURSES", "دروس کارشناسی ارشد", false),
        CourseItem(PHD_PRIVATE_LAW, "آمادگی برای کنکور دکتری حقوق خصوصی", true),
        CourseItem("PHD_COURSES", "دروس دکتری حقوق خصوصی", false),
    )

    val phdSections = listOf(
        "حقوق مدنی",
        "حقوق تجارت",
        "متون فقه — معاملات",
        "زبان انگلیسی",
    )
}
