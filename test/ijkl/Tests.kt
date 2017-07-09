package ijkl

import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class Tests {
    @Test fun `read keymap xml`() {
        val shortcutsData = loadShortcutsData("test/ijkl/ijkl-osx.xml")

        shortcutsData.size shouldEqual 130
        shortcutsData.first().apply {
            actionId shouldEqual "\$Delete"
            keystrokes shouldEqual listOf("delete", "back_space", "meta back_space", "alt semicolon")
        }
        shortcutsData.last().apply {
            actionId shouldEqual "splitAndMoveRight"
            keystrokes shouldEqual listOf("shift ctrl alt close_bracket")
        }
    }

    private infix fun <T> T.shouldEqual(that: T) {
        assertThat(this, equalTo(that))
    }
}