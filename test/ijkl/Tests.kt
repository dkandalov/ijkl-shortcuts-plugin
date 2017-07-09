package ijkl

import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class Tests {
    @Test fun `read keymap xml`() {
        val shortcutsData = javaClass.classLoader.getResource("ijkl-keymap.xml").openStream().use {
            loadShortcutsData(it)
        }

        shortcutsData.size shouldEqual 130
        shortcutsData.first().apply {
            actionId shouldEqual "\$Delete"
            shortcuts shouldEqual listOf("delete", "back_space", "meta back_space", "alt semicolon").map{ it.toKeyboardShortcut() }
        }
        shortcutsData.last().apply {
            actionId shouldEqual "splitAndMoveRight"
            shortcuts shouldEqual listOf("shift ctrl alt close_bracket").map{ it.toKeyboardShortcut() }
        }
    }

    private infix fun <T> T.shouldEqual(that: T) {
        assertThat(this, equalTo(that))
    }
}