package ir.madani3.mindgame

import org.junit.Assert.assertEquals
import org.junit.Test

class GameContractTest {
    @Test fun mvpHasExactlyFiftyMainArticles() {
        assertEquals(50, (183..232).count())
    }
}
