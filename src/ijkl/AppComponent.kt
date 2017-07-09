package ijkl

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.ui.KeyStrokeAdapter
import org.w3c.dom.Node
import java.io.InputStream
import javax.swing.KeyStroke
import javax.xml.parsers.DocumentBuilderFactory

class AppComponent: ApplicationComponent {
    private val logger = Logger.getInstance(this.javaClass.canonicalName)

    override fun initComponent() {
        val shortcutsData = javaClass.classLoader.getResource("ijkl-keymap.xml").openStream().use {
            loadShortcutsData(it)
        }

        var addShortcutsResult = AddShortcutsResult()

        registerKeymapListener(ApplicationManager.getApplication(), object: KeymapChangeListener {
            override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
                if (oldKeymap != null) oldKeymap.remove(addShortcutsResult.added)
                if (newKeymap != null) addShortcutsResult = newKeymap.add(shortcutsData)

                logger.info(
                    "Switched keymap from '$oldKeymap' to '$newKeymap'. Shortcuts:" +
                        "added - ${addShortcutsResult.added.size};" +
                        "already existed - ${addShortcutsResult.alreadyExisted.size};" +
                        "conflicts - ${addShortcutsResult.conflictsByActionId.size}"
                )
            }
        })
    }
}

fun loadShortcutsData(inputStream: InputStream): List<ShortcutData> {
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
    val document = builder.parse(inputStream)
    val keymapTag = document.children().find { it.nodeName == "keymap" } ?: error("")

    return keymapTag.children()
        .filter { it.nodeName == "action" }
        .map {
            ShortcutData(
                it.getAttribute("id") ?: "",
                it.children()
                    .filter { it.nodeName == "keyboard-shortcut" }
                    .map { it.getAttribute("first-keystroke")?.toKeyboardShortcut() }
                    .filterNotNull()
            )
        }
}

data class ShortcutData(val actionId: String, val shortcuts: List<Shortcut>)

data class AddShortcutsResult(
    val added: MutableList<ShortcutData> = ArrayList(),
    val alreadyExisted: LinkedHashSet<ShortcutData> = LinkedHashSet(),
    val conflictsByActionId: MutableMap<String, ShortcutData> = HashMap()
) {
    override fun toString() =
        "AddShortcutsResult{added=$added, alreadyExisted=$alreadyExisted, conflictsByActionId=$conflictsByActionId}"
}

fun Keymap.add(shortcutsData: List<ShortcutData>): AddShortcutsResult {
    val result = AddShortcutsResult()
    shortcutsData.forEach {
        it.shortcuts.forEach { shortcut ->
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
    return result
}

fun String?.toKeyboardShortcut(): Shortcut? {
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
    if (firstKeystroke == null) error("Invalid keystroke '${this}'")

    return KeyboardShortcut(firstKeystroke, secondKeystroke)
}

fun Keymap.remove(shortcutsData: List<ShortcutData>) {
    shortcutsData.forEach {
        it.shortcuts.forEach { shortcut ->
            removeShortcut(it.actionId, shortcut)
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
