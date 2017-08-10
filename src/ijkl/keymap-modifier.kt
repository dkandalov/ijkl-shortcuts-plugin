package ijkl

import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.notification.NotificationListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.keymap.KeymapUtil.getShortcutText
import com.intellij.openapi.util.Disposer
import java.io.File
import java.io.InputStream

fun initCurrentKeymapModifier(
    keymapInputStream: InputStream,
    application: Application,
    logger: Logger,
    actionManager: ActionManager = ActionManager.getInstance()
) {
    var shortcuts = IjklShortcuts(keymapInputStream.readShortcutsData())

    registerKeymapListener(application, object: KeymapChangeListener {
        override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
            if (oldKeymap != null) shortcuts.removeFrom(oldKeymap)
            if (newKeymap != null) shortcuts = shortcuts.addTo(newKeymap)

            logger.info(
                "Switched keymap from '$oldKeymap' to '$newKeymap'. Shortcuts: " +
                "added - ${shortcuts.added.size}; " +
                "already existed - ${shortcuts.alreadyExisted.size}; " +
                "conflicts - ${shortcuts.conflicts.values.sumBy { it.size }}"
            )
            if (shortcuts.conflicts.isNotEmpty()) {
                val conflictsDescription = shortcuts
                    .conflicts.entries
                    .map { (shortcutData, conflictingActionIds) ->
                        val shortcutsDescription = shortcutData.shortcuts.map { getShortcutText(it) }
                        val ijklAction = actionManager.actionText(shortcutData.actionId)
                        val actionsDescription = conflictingActionIds
                            .map { id -> actionManager.actionText(id) }
                            .joinToString(", ", "[", "]")

                        "$shortcutsDescription '$ijklAction' conflicts with: $actionsDescription"
                    }

                application.showNotification(
                    message = "There were conflicts between IJKL shortcuts and '$newKeymap' keymap. See <a href=''>IDE log file</a> for more details.",
                    listener = NotificationListener { _, _ ->
                        // Based on com.intellij.ide.actions.ShowLogAction code.
                        if (ShowFilePathAction.isSupported()) {
                            val logFile = File(PathManager.getLogPath(), "idea.log")
                            ShowFilePathAction.openFile(logFile)
                        }
                    }
                )
                logger.info("Conflicts after switching to '$newKeymap'\n" + conflictsDescription.joinToString("\n"))
            }
        }
    })
}

data class ShortcutData(val actionId: String, val shortcuts: List<Shortcut>)

data class IjklShortcuts(
    val all: List<ShortcutData>,
    val added: List<ShortcutData> = ArrayList(),
    val alreadyExisted: Set<ShortcutData> = LinkedHashSet(),
    val conflicts: Map<ShortcutData, List<String>> = HashMap()
) {
    fun addTo(keymap: Keymap): IjklShortcuts {
        val added = ArrayList<ShortcutData>()
        val alreadyExisted = LinkedHashSet<ShortcutData>()
        val conflicts = HashMap<ShortcutData, List<String>>()

        // Collect bound action ids before modifying keymap.
        val boundActionIdsByShortcut: Map<Shortcut, List<String>> = all
            .flatMap { it.shortcuts }.distinct()
            .associate{ Pair(it, keymap.getActionIds(it).toList()) }

        all.forEach { shortcutData ->
            shortcutData.shortcuts.forEach { shortcut ->
                val boundActionIds = boundActionIdsByShortcut[shortcut]!!
                val conflictingActionIds = boundActionIds - shortcutData.actionId

                if (boundActionIds.contains(shortcutData.actionId)) {
                    alreadyExisted.add(shortcutData)
                } else {
                    added.add(shortcutData)
                    keymap.addShortcut(shortcutData.actionId, shortcut)
                }

                if (conflictingActionIds.isNotEmpty()) {
                    conflicts.put(shortcutData, conflictingActionIds)
                }
            }
        }
        return IjklShortcuts(all, added, alreadyExisted, conflicts)
    }

    fun removeFrom(keymap: Keymap) {
        added.forEach { (actionId, shortcuts) ->
            shortcuts.forEach { shortcut ->
                keymap.removeShortcut(actionId, shortcut)
            }
        }
    }

    override fun toString() =
        "IjklShortcuts{added=$added, alreadyExisted=$alreadyExisted, conflictsByActionId=$conflicts}"
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