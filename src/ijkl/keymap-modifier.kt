package ijkl

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.*
import com.intellij.openapi.keymap.KeymapUtil.getShortcutText
import com.intellij.openapi.util.Disposer
import java.io.*

fun initCurrentKeymapModifier(
    keymapInputStream: InputStream,
    application: Application,
    logger: Logger,
    actionManager: ActionManager
) {
    var shortcuts = IjklShortcuts(keymapInputStream.readShortcutsData())

    registerKeymapListener(application, object : KeymapChangeListener {
        override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
            if (oldKeymap != null) shortcuts.removeFrom(oldKeymap)
            if (newKeymap == null) return
            shortcuts = shortcuts.addTo(newKeymap)

            logger.info(
                "Switched keymap from '$oldKeymap' to '$newKeymap'. Shortcuts: " +
                    "added - ${shortcuts.added.size}; " +
                    "already existed - ${shortcuts.alreadyExisted.size}; " +
                    "conflicts - ${shortcuts.conflicts.values.sumOf { it.size }}"
            )
            if (shortcuts.conflicts.isNotEmpty()) {
                val conflictsDescription = shortcuts
                    .conflicts.entries
                    .map {
                        val shortcutData = it.key
                        val conflictingActionIds = it.value
                        val shortcutsDescription = shortcutData.shortcuts.map { shortcut -> getShortcutText(shortcut) }
                        val ijklAction = actionManager.actionText(shortcutData.actionId)
                        val actionsDescription = conflictingActionIds.asSequence()
                            .map { id -> actionManager.actionText(id) }
                            .joinToString(", ", "[", "]")
                        "$shortcutsDescription '$ijklAction' conflicts with: $actionsDescription"
                    }

                application.showNotification(
                    message = "There were conflicts between IJKL shortcuts and '$newKeymap' keymap. See <a href=''>IDE log file</a> for more details.",
                    listener = { _, _ ->
                        // Based on com.intellij.ide.actions.ShowLogAction code.
                        if (RevealFileAction.isSupported()) {
                            RevealFileAction.openFile(File(PathManager.getLogPath(), "idea.log"))
                        }
                    }
                )
                logger.info("Conflicts after switching to '$newKeymap'\n" + conflictsDescription.joinToString("\n"))
            }
        }
    })
}

data class ShortcutData(val actionId: String, val shortcuts: List<Shortcut>)

private data class IjklShortcuts(
    val shortcutsToAdd: List<ShortcutData>,
    val added: List<ShortcutData> = ArrayList(),
    val alreadyExisted: Set<ShortcutData> = LinkedHashSet(),
    val conflicts: Map<ShortcutData, List<String>> = HashMap()
) {
    fun addTo(keymap: Keymap): IjklShortcuts {
        val added = ArrayList<ShortcutData>()
        val alreadyExisted = LinkedHashSet<ShortcutData>()
        val conflicts = HashMap<ShortcutData, List<String>>()

        // Collect bound action ids before modifying keymap.
        val actionIdsByShortcut: Map<Shortcut, List<String>> =
            shortcutsToAdd.flatMap { it.shortcuts }.distinct()
                .associateWith { keymap.getActionIdList(it) }

        shortcutsToAdd.forEach { shortcutData ->
            shortcutData.shortcuts.forEach { shortcut ->
                val boundActionIds = actionIdsByShortcut[shortcut] ?: error("no value for $shortcut")

                if (shortcutData.actionId in boundActionIds) {
                    alreadyExisted.add(shortcutData)
                } else {
                    keymap.addShortcut(shortcutData.actionId, shortcut)
                    added.add(shortcutData)
                }

                val conflictingActionIds = boundActionIds - shortcutData.actionId
                if (conflictingActionIds.isNotEmpty()) {
                    conflicts[shortcutData] = conflictingActionIds
                }
            }
        }
        return IjklShortcuts(shortcutsToAdd, added, alreadyExisted, conflicts)
    }

    fun removeFrom(keymap: Keymap) {
        added.forEach {
            it.shortcuts.forEach { shortcut ->
                keymap.removeShortcut(it.actionId, shortcut)
            }
        }
    }

    override fun toString() =
        "IjklShortcuts{added=$added, alreadyExisted=$alreadyExisted, conflictsByActionId=$conflicts}"
}

interface KeymapChangeListener {
    fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?)
}

private fun registerKeymapListener(application: Application, listener: KeymapChangeListener) {
    var keymap: Keymap? = KeymapManager.getInstance().activeKeymap

    application.messageBus.connect().subscribe(KeymapManagerListener.TOPIC, object : KeymapManagerListener {
        override fun activeKeymapChanged(newKeymap: Keymap?) {
            val oldKeymap = keymap
            keymap = newKeymap

            if (oldKeymap != newKeymap) {
                listener.onChange(oldKeymap, newKeymap)
            }
        }
    })

    Disposer.register(application) {
        listener.onChange(oldKeymap = keymap, newKeymap = null)
    }

    listener.onChange(oldKeymap = null, newKeymap = keymap)
}