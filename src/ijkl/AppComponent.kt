package ijkl

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.ui.KeyStrokeAdapter
import org.w3c.dom.Node
import java.io.File
import javax.swing.KeyStroke
import javax.xml.parsers.DocumentBuilderFactory

class AppComponent: ApplicationComponent {
    override fun initComponent() {
        val shortcutsData = loadShortcutsData("/Users/dima/IdeaProjects/ijkl-keymaps/intellij/ijkl-osx.xml")
//        show("Loaded shortcuts size: " + shortcutsData.size)

        var mergeResult = MergeShortcutsResult()

        registerKeymapListener(ApplicationManager.getApplication(), object: KeymapChangeListener {
            override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
                //show("Before ${oldKeymap}; After ${newKeymap}")
                if (oldKeymap != null) oldKeymap.remove(mergeResult.added)
                if (newKeymap != null) mergeResult = newKeymap.add(shortcutsData)
            }
        })
    }
}

fun loadShortcutsData(fileName: String): List<ShortcutData> {
    fun Node.getAttribute(name: String): String? =
        attributes.getNamedItem(name)?.nodeValue

    fun Node.children(): List<Node> {
        val result = ArrayList<Node>()
        0.until(childNodes.length).forEach { i ->
            result.add(childNodes.item(i))
        }
        return result
    }

    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = builder.parse(File(fileName))
    val keymapTag = document.children().find { it.nodeName == "keymap" } ?: error("")

    return keymapTag.children()
        .filter { it.nodeName == "action" }
        .map {
            ShortcutData(
                it.getAttribute("id") ?: "",
                it.children()
                    .filter { it.nodeName == "keyboard-shortcut" }
                    .map { it.getAttribute("first-keystroke") ?: "" }
            )
        }
}

data class ShortcutData(val actionId: String, val keystrokes: List<String>)

data class MergeShortcutsResult(
    val added: MutableList<ShortcutData> = ArrayList(),
    val alreadyExisted: LinkedHashSet<ShortcutData> = LinkedHashSet(),
    val conflictsByActionId: MutableMap<String, ShortcutData> = HashMap()
) {
    override fun toString() =
        "MergeShortcutsResult{added=$added, alreadyExisted=$alreadyExisted, conflictsByActionId=$conflictsByActionId}"
}

fun Keymap.add(shortcutsData: List<ShortcutData>): MergeShortcutsResult {
    val result = MergeShortcutsResult()
    shortcutsData.forEach {
        it.keystrokes
            .map { keystroke -> keystroke.toKeyboardShortcut() }
            .filterNotNull()
            .forEach { shortcut: KeyboardShortcut ->
                val boundActionIds = getActionIds(shortcut).toList()
                if (boundActionIds.contains(it.actionId)) {
                    result.alreadyExisted.add(it)
                } else {
                    result.added.add(it)
                    addShortcut(it.actionId, shortcut)
                }

                (boundActionIds - it.actionId).forEach { boundActionId ->
                    result.conflictsByActionId.put(boundActionId, it)
                }
            }
    }
//    show("Added: " + result.added.size())
//    show("Existed: " + result.alreadyExisted.size())
//    show("Conflicts: " + result.conflictsByActionId.size())
    return result
}

fun String?.toKeyboardShortcut(): KeyboardShortcut? {
    if (this == null || trim().isEmpty()) return null

    val firstKeystroke: KeyStroke?
    var secondKeystroke: KeyStroke? = null
    if (contains(",")) {
        val i = indexOf(",")
        firstKeystroke = KeyStrokeAdapter.getKeyStroke(substring(0, i).trim())
        secondKeystroke = KeyStrokeAdapter.getKeyStroke(substring(i + 1).trim())
    } else {
        firstKeystroke = KeyStrokeAdapter.getKeyStroke(this)
    }
    if (firstKeystroke == null) throw error("Invalid keystroke '${this}'")
    return KeyboardShortcut(firstKeystroke, secondKeystroke)
}

fun Keymap.remove(shortcutsData: List<ShortcutData>) {
    shortcutsData.forEach {
        it.keystrokes.forEach { keystroke ->
            val shortcut = keystroke.toKeyboardShortcut()
            if (shortcut != null) {
                removeShortcut(it.actionId, shortcut)
            }
        }
    }
}

interface KeymapChangeListener {
    fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?)
}

fun registerKeymapListener(parentDisposable: Disposable, listener: KeymapChangeListener) {
    val keymapManager = KeymapManager.getInstance()
    var keymap = keymapManager.activeKeymap
    keymapManager.addKeymapManagerListener(KeymapManagerListener { newKeymap ->
        val oldKeymap = keymap
        keymap = newKeymap

        if (oldKeymap != newKeymap) {
            listener.onChange(oldKeymap, newKeymap)
        }
    }, parentDisposable)

    Disposer.register(parentDisposable, Disposable {
        listener.onChange(keymap, null)
    })

    listener.onChange(null, keymap)
}
