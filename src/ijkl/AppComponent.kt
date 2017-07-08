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
import java.io.File
import javax.swing.KeyStroke

class AppComponent: ApplicationComponent {
    override fun initComponent() {
        val shortcutsData = loadShortcutsData("/Users/dima/IdeaProjects/ijkl-keymaps/intellij/ijkl-osx.xml")
//        show("Loaded shortcuts size: " + shortcutsData.size)

        var mergeResult = MergeShortcutsResult()

        registerKeymapListener(ApplicationManager.getApplication(), object : KeymapChangeListener {
            override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
                //show("Before ${oldKeymap}; After ${newKeymap}")
                if (oldKeymap != newKeymap) {
                    if (oldKeymap != null) removeFrom(oldKeymap, mergeResult.added)
                    if (newKeymap != null) mergeResult = addTo(newKeymap, shortcutsData)
                }
            }
        })
    }
}

fun loadShortcutsData(fileName: String): List<ShortcutData> {
    val file = File(fileName)
//    val rootNode = XmlParser().parse(file)
//    val result = rootNode.children().collect {
//        new ShortcutData(
//            it.@id as String,
//        it.children().collect { it.@"first-keystroke" as String }
//        )
//    }
//    return result
    TODO()
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

fun addTo(keymap: Keymap, shortcutsData: List<ShortcutData>): MergeShortcutsResult {
    val result = MergeShortcutsResult()
    shortcutsData.forEach {
        it.keystrokes
            .map { keystroke -> asKeyboardShortcut(keystroke) }
            .filterNotNull()
            .forEach { shortcut: KeyboardShortcut ->
                val boundActionIds = keymap.getActionIds(shortcut).toList()
                if (boundActionIds.contains(it.actionId)) {
                    result.alreadyExisted.add(it)
                } else {
                    result.added.add(it)
                    keymap.addShortcut(it.actionId, shortcut)
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

fun asKeyboardShortcut(keyStroke: String?): KeyboardShortcut? {
    if (keyStroke == null || keyStroke.trim().isEmpty()) return null

    var firstKeystroke: KeyStroke? = null
    var secondKeystroke: KeyStroke? = null
    if (keyStroke.contains(",")) {
        val i = keyStroke.indexOf(",")
        firstKeystroke = KeyStrokeAdapter.getKeyStroke(keyStroke.substring(0, i).trim())
        secondKeystroke = KeyStrokeAdapter.getKeyStroke(keyStroke.substring(i + 1).trim())
    } else {
        firstKeystroke = KeyStrokeAdapter.getKeyStroke(keyStroke)
    }
    if (firstKeystroke == null) throw error("Invalid keystroke '$keyStroke'")
    return KeyboardShortcut(firstKeystroke, secondKeystroke)
}

fun removeFrom(keymap: Keymap, shortcutsData: List<ShortcutData>) {
    shortcutsData.forEach {
        it.keystrokes.forEach { keystroke ->
            val shortcut = asKeyboardShortcut(keystroke)
            if (shortcut != null) {
                keymap.removeShortcut(it.actionId, shortcut)
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

        listener.onChange(oldKeymap, newKeymap)
    }, parentDisposable)

    Disposer.register(parentDisposable, Disposable {
        listener.onChange(keymap, null) }
    )

    listener.onChange(null, keymap)
}
