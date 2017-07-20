package ijkl

import com.intellij.openapi.util.io.FileUtil
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class Tests {
    @Test fun `read keymap xml`() {
        val shortcutsData = resourceInputStream("ijkl-keymap.xml").readShortcutsData()

        shortcutsData.size shouldEqual 56
        shortcutsData.sumBy { it.shortcuts.size } shouldEqual 62
        shortcutsData.first().apply {
            actionId shouldEqual "\$Delete"
            shortcuts shouldEqual listOf("alt semicolon").map{ it.toKeyboardShortcut() }
        }
        shortcutsData.last().apply {
            actionId shouldEqual "Unwrap"
            shortcuts shouldEqual listOf("shift ctrl back_space").map{ it.toKeyboardShortcut() }
        }
    }

    @Test fun `copy layout from resources to a folder`() {
        val tempDir = FileUtil.createTempDirectory("", "", true)

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