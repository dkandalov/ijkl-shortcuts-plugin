package ijkl

import com.intellij.openapi.Disposable
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.util.Disposer

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