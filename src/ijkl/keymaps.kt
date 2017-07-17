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
    application: Application,
    logger: Logger,
    shouldLogConflicts: Boolean
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
        all.forEach {
            it.shortcuts.forEach { shortcut ->
                val boundActionIds = keymap.getActionIds(shortcut).toList()
                if (boundActionIds.contains(it.actionId)) {
                    alreadyExisted.add(it)
                } else {
                    added.add(it)
                    keymap.addShortcut(it.actionId, shortcut)
                }

                (boundActionIds - it.actionId).forEach { boundActionId ->
                    conflictsByActionId.put(boundActionId, it)
                }
            }
        }
        return IjklShortcuts(all, added, alreadyExisted, conflictsByActionId)
    }

    fun removeFrom(keymap: Keymap) {
        added.forEach {
            it.shortcuts.forEach { shortcut ->
                keymap.removeShortcut(it.actionId, shortcut)
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