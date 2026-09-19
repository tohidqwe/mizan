package com.mizan.civilleitner.product

import androidx.compose.runtime.Composable

@Composable
fun ProductScreenHost(
    screen: ProductScreen,
    vm: ProductViewModel,
    navigate: (ProductScreen) -> Unit,
) {
    when (screen) {
        ProductScreen.HOME -> ProductHomeScreen(vm, navigate)
        ProductScreen.TODAY -> ProductTodayScreen(vm, navigate)
        ProductScreen.EDUCATION -> EducationScreen(vm, navigate)
        ProductScreen.PHD -> PhdCourseScreen(vm, navigate)
        ProductScreen.STUDY_PLANNING -> StudyPlanningScreen(vm)
        ProductScreen.REVIEWS -> ProductReviewScreen(vm)
        ProductScreen.PLANNER -> ProductPlannerScreen(vm)
        ProductScreen.SUBSCRIPTION -> SubscriptionScreen()
        ProductScreen.CLIENT_PORTAL -> ClientPortalScreen(vm)
        ProductScreen.ADMIN -> DoctorAdminScreen(vm)
        ProductScreen.INBOX -> InboxScreen(vm)
        ProductScreen.UPDATES -> UpdateScreen()
        ProductScreen.SETTINGS -> ProductSettingsScreen(vm)
        ProductScreen.CIVIL -> CivilLawScreen(vm)
        ProductScreen.TRADE -> TradeScreen(vm)
        ProductScreen.FIQH -> FiqhScreen(vm)
        ProductScreen.ENGLISH -> EnglishScreen(vm)
        ProductScreen.ADMIN_PROVISION -> AdminProvisionScreen(vm, navigate)
    }
}
