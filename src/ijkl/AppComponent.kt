package ijkl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.Keymap

class AppComponent: ApplicationComponent {
    private val logger = Logger.getInstance(this.javaClass.canonicalName)

    override fun initComponent() {
        val shortcutsData = readShortcutsData("ijkl-keymap.xml")
        var addShortcutsResult = AddShortcutsResult()

        registerKeymapListener(ApplicationManager.getApplication(), object: KeymapChangeListener {
            override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
                if (oldKeymap != null) oldKeymap.remove(addShortcutsResult.added)
                if (newKeymap != null) addShortcutsResult = newKeymap.add(shortcutsData)

                logger.info(
                    "Switched keymap from '$oldKeymap' to '$newKeymap'. Shortcuts: " +
                        "added - ${addShortcutsResult.added.size}; " +
                        "already existed - ${addShortcutsResult.alreadyExisted.size}; " +
                        "conflicts - ${addShortcutsResult.conflictsByActionId.size}"
                )
            }
        })
    }
}