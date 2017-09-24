package ijkl

import com.intellij.openapi.util.io.FileUtil
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class Tests {
    @Test fun `win, linux keymap xml`() {
        resourceInputStream("ijkl-keymap.xml").readShortcutsData().validate(
            amountOfActions = 48,
            amountOfShortcuts = 49
        )
    }

    @Test fun `osx keymap xml`() {
        resourceInputStream("ijkl-osx-keymap.xml").readShortcutsData().validate(
            amountOfActions = 48,
            amountOfShortcuts = 50
        )
    }

    private fun List<ShortcutData>.validate(amountOfActions: Int, amountOfShortcuts: Int) {
        size shouldEqual amountOfActions
        sumBy { it.shortcuts.size } shouldEqual amountOfShortcuts
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
        println("tempDir: ${tempDir.absolutePath}")

        copyKeyLayoutTo(fromResource = "ijkl-keys.bundle", toDir = tempDir.absolutePath)

        allFilesIn(tempDir) shouldEqual allFilesIn(File("resources/ijkl-keys.bundle"))
    }

    private fun allFilesIn(dir: File) = FileUtil
        .findFilesOrDirsByMask(Pattern.compile(".*"), dir)
        .map { it.toRelativeString(dir) }

    private infix fun <T> T.shouldEqual(that: T) {
        assertThat(this, equalTo(that))
    }
}