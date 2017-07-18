package ijkl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.util.Disposer
import java.io.InputStream

fun initCurrentKeymapModifier(
    keymapInputStream: InputStream,
    shouldLogConflicts: Boolean,
    application: Application,
    logger: Logger
) {
    var shortcuts = IjklShortcuts(all = keymapInputStream.readShortcutsData())

    registerKeymapListener(application, object: KeymapChangeListener {
        override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
            if (oldKeymap != null) shortcuts.removeFrom(oldKeymap)
            if (newKeymap != null) shortcuts = shortcuts.addTo(newKeymap)

            if (shouldLogConflicts) {
                logger.info(
                    "Switched keymap from '$oldKeymap' to '$newKeymap'. Shortcuts: " +
                    "added - ${shortcuts.added.size}; " +
                    "already existed - ${shortcuts.alreadyExisted.size}; " +
                    "conflicts - ${shortcuts.conflictsByActionId.size}"
                )
                shortcuts.conflictsByActionId.forEach { logger.info(it.toString()) }
            }
        }
    })
}

data class IjklShortcuts(
    val all: List<ShortcutData>,
    val added: List<ShortcutData> = ArrayList(),
    val alreadyExisted: Set<ShortcutData> = LinkedHashSet(),
    val conflictsByActionId: Map<String, ShortcutData> = HashMap()
) {
    fun addTo(keymap: Keymap): IjklShortcuts {
        val added = ArrayList<ShortcutData>()
        val alreadyExisted = LinkedHashSet<ShortcutData>()
        val conflictsByActionId = HashMap<String, ShortcutData>()
        all.forEach { shortcutData ->
            shortcutData.shortcuts.forEach { shortcut ->
                val boundActionIds = keymap.getActionIds(shortcut).toList()
                val conflictingActionIds = boundActionIds - shortcutData.actionId

                if (boundActionIds.contains(shortcutData.actionId)) {
                    alreadyExisted.add(shortcutData)
                } else if (conflictingActionIds.isNotEmpty()) {
                    conflictingActionIds.forEach { conflictsByActionId.put(it, shortcutData) }
                } else {
                    added.add(shortcutData)
                    keymap.addShortcut(shortcutData.actionId, shortcut)
                }
            }
        }
        return IjklShortcuts(all, added, alreadyExisted, conflictsByActionId)
    }

    fun removeFrom(keymap: Keymap) {
        added.forEach { (actionId, shortcuts) ->
            shortcuts.forEach { shortcut ->
                keymap.removeShortcut(actionId, shortcut)
            }
        }
    }

    override fun toString() =
        "IjklShortcuts{added=$added, alreadyExisted=$alreadyExisted, conflictsByActionId=$conflictsByActionId}"
}

interface KeymapChangeListener {
    fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?)
}

private fun registerKeymapListener(parentDisposable: Disposable, listener: KeymapChangeListener) {
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