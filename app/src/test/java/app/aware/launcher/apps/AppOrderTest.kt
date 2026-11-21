package app.aware.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderTest {

    private val apps = listOf("Camera", "Maps", "Messages", "Photos", "Weather")

    private fun arrange(query: String = "", pinned: Set<String> = emptySet(), hidden: Set<String> = emptySet()) =
        AppOrder.arrange(apps, query, label = { it }, isPinned = { it in pinned }, isHidden = { it in hidden })

    @Test
    fun `pinned apps come first in alphabetical order`() {
        assertEquals(
            listOf("Messages", "Weather", "Camera", "Maps", "Photos"),
            arrange(pinned = setOf("Weather", "Messages"))
        )
    }

    @Test
    fun `hidden apps are left out of the list`() {
        assertEquals(listOf("Camera", "Messages", "Weather"), arrange(hidden = setOf("Maps", "Photos")))
    }

    @Test
    fun `search finds hidden apps and ignores pins`() {
        assertEquals(
            listOf("Camera", "Maps", "Messages"),
            arrange(query = " m", pinned = setOf("Messages"), hidden = setOf("Maps"))
        )
    }

    @Test
    fun `a pinned app that is also hidden stays hidden`() {
        assertEquals(
            listOf("Camera", "Messages", "Photos", "Weather"),
            arrange(pinned = setOf("Maps"), hidden = setOf("Maps"))
        )
    }
}
