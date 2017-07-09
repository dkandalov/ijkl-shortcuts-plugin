package ijkl

import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class Tests {
    @Test fun `read keymap xml`() {
        val shortcutsData = readShortcutsData("ijkl-keymap.xml")

        shortcutsData.size shouldEqual 70
        shortcutsData.sumBy { it.shortcuts.size } shouldEqual 88
        shortcutsData.first().apply {
            actionId shouldEqual "\$Delete"
            shortcuts shouldEqual listOf("alt semicolon").map{ it.toKeyboardShortcut() }
        }
        shortcutsData.last().apply {
            actionId shouldEqual "Unwrap"
            shortcuts shouldEqual listOf("shift ctrl back_space").map{ it.toKeyboardShortcut() }
        }
    }

    private infix fun <T> T.shouldEqual(that: T) {
        assertThat(this, equalTo(that))
    }
}