package ijkl

import com.intellij.openapi.util.io.FileUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Test
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.io.File
import java.util.*
import javax.swing.JPanel

class ReadShortcutsTests {
    @Test fun `win, linux keymap xml`() {
        resourceInputStream("ijkl-keymap.xml").readShortcutsData()
            .validate(amountOfActions = 49, amountOfShortcuts = 49)
    }

    @Test fun `macos keymap xml`() {
        resourceInputStream("ijkl-macos-keymap.xml").readShortcutsData()
            .validate(amountOfActions = 49, amountOfShortcuts = 51)
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
}

class CopyKeyLayoutTests {
    @Test fun `copy layout from resources to a folder`() {
        val tempDir = FileUtil.createTempDirectory("", "", true)

        copyKeyLayout(fromResource = "ijkl-keys.bundle", toDir = tempDir.absolutePath)

        tempDir.allFiles() shouldEqual File("resources/ijkl-keys.bundle").allFiles()
    }

    private fun File.allFiles() = walkTopDown()
        .mapTo(TreeSet()) { it.toRelativeString(this) }
}

class KeyEventRedispatchTests {
    // The events recorded by printing them in mapIfIjkl() function.
    private val altIJKL = """
        KEY_PRESSED,123,512,18,￿,2
        KEY_PRESSED,123,512,73,i,1
        KEY_TYPED,123,512,0,i,0
        KEY_RELEASED,123,512,73,i,1
        KEY_PRESSED,123,512,75,k,1
        KEY_TYPED,123,512,0,k,0
        KEY_RELEASED,123,512,75,k,1
        KEY_PRESSED,123,512,74,j,1
        KEY_TYPED,123,512,0,j,0
        KEY_RELEASED,123,512,74,j,1
        KEY_PRESSED,123,512,76,l,1
        KEY_TYPED,123,512,0,l,0
        KEY_RELEASED,123,512,76,l,1
    """.trimIndent().lines().map { it.parseKeyEvent() }

    @Test fun `don't map events in the text editor because IDE handles them correctly`() {
        val context = keyEventContext()
        altIJKL.map { it.mapIfIjkl(context)?.toCsv() }.joinToString("\n")
            .shouldEqual("""
                null
                null
                null
                null
                null
                null
                null
                null
                null
                null
                null
                null
                null
            """.trimIndent())
    }

    @Test fun `map events when popup is active because editor shortcuts don't work there`() {
        val context = keyEventContext(isPopupActive = true)
        altIJKL.map { it.mapIfIjkl(context)?.toCsv() }.joinToString("\n")
            .shouldEqual("""
                null
                KEY_PRESSED,123,0,38, ,1
                KEY_TYPED,123,0,0, ,0
                KEY_RELEASED,123,0,0, ,1
                KEY_PRESSED,123,0,40, ,1
                KEY_TYPED,123,0,0, ,0
                KEY_RELEASED,123,0,0, ,1
                KEY_PRESSED,123,0,37, ,1
                KEY_TYPED,123,0,0, ,0
                KEY_RELEASED,123,0,0, ,1
                KEY_PRESSED,123,0,39, ,1
                KEY_TYPED,123,0,0, ,0
                KEY_RELEASED,123,0,0, ,1
            """.trimIndent())
    }

    @Test fun `map events in componentes with tree`() {
        val context = keyEventContext(hasParentTree = true)
        altIJKL.map { it.mapIfIjkl(context)?.toCsv() }.joinToString("\n")
            .shouldEqual("""
                null
                KEY_PRESSED,123,0,38, ,1
                null
                KEY_RELEASED,123,0,0, ,1
                KEY_PRESSED,123,0,40, ,1
                null
                KEY_RELEASED,123,0,0, ,1
                KEY_PRESSED,123,0,37, ,1
                null
                KEY_RELEASED,123,0,0, ,1
                KEY_PRESSED,123,0,39, ,1
                null
                KEY_RELEASED,123,0,0, ,1
            """.trimIndent())
    }

    private fun keyEventContext(
        hasParentSearchTextArea: Boolean = false,
        hasParentTree: Boolean = false,
        isPopupActive: Boolean = false,
        findActionsByShortcut: Boolean = false,
    ) = object : KeyEventContext {
        override val component = null
        override val hasParentSearchTextArea = hasParentSearchTextArea
        override val hasParentTree = hasParentTree
        override val isPopupActive = isPopupActive
        override val findActionsByShortcut = findActionsByShortcut
    }

    private fun String.parseKeyEvent(): KeyEvent {
        val split = split(",")
        val id = when (split[0]) {
            "KEY_TYPED"    -> KEY_TYPED
            "KEY_PRESSED"  -> KEY_PRESSED
            "KEY_RELEASED" -> KEY_RELEASED
            else         -> split[0].toInt()
        }
        return KeyEvent(
            /* source = */ JPanel(),
            /* id = */ id,
            /* when = */ split[1].toLong(),
            /* modifiers = */ split[2].toInt(),
            /* keyCode = */ split[3].toInt(),
            /* keyChar = */ split[4].first(),
            /* keyLocation = */ split[5].toInt()
        )
    }

    private fun KeyEvent.toCsv(): String {
        val idString = when (id) {
            KEY_TYPED    -> "KEY_TYPED"
            KEY_PRESSED  -> "KEY_PRESSED"
            KEY_RELEASED -> "KEY_RELEASED"
            else         -> id.toString()
        }
        return "$idString,$`when`,$modifiersEx,$keyCode,$keyChar,$keyLocation"
    }
}

private infix fun <T> T.shouldEqual(that: T) {
    assertThat(this, equalTo(that))
}
