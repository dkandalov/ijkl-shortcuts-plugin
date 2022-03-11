package ijkl

import com.intellij.openapi.util.io.FileUtil
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.util.TreeSet

class Tests {
    @Test fun `win, linux keymap xml`() {
        resourceInputStream("ijkl-keymap.xml").readShortcutsData().validate(
            amountOfActions = 49,
            amountOfShortcuts = 50
        )
    }

    @Test fun `osx keymap xml`() {
        resourceInputStream("ijkl-osx-keymap.xml").readShortcutsData().validate(
            amountOfActions = 49,
            amountOfShortcuts = 51
        )
    }

    private fun List<ShortcutData>.validate(amountOfActions: Int, amountOfShortcuts: Int) {
        size shouldEqual amountOfActions
        sumOf { it.shortcuts.size } shouldEqual amountOfShortcuts
        first().apply {
            actionId shouldEqual "\$Delete"
            shortcuts shouldEqual listOf("alt semicolon").map { it.toKeyboardShortcut() }
        }
        last().apply {
            actionId shouldEqual "Unwrap"
            shortcuts shouldEqual listOf("shift ctrl back_space").map { it.toKeyboardShortcut() }
        }
    }

    @Test fun `copy layout from resources to a folder`() {
        val tempDir = FileUtil.createTempDirectory("", "", true)

        copyKeyLayout(fromResource = "ijkl-keys.bundle", toDir = tempDir.absolutePath)

        tempDir.allFiles() shouldEqual File("resources/ijkl-keys.bundle").allFiles()
    }

    private fun File.allFiles() = walkTopDown()
        .mapTo(TreeSet()) { it.toRelativeString(this) }

    private infix fun <T> T.shouldEqual(that: T) {
        assertThat(this, equalTo(that))
    }
}