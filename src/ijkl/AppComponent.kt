package ijkl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.io.InputStream

class AppComponent: ApplicationComponent {
    private val logger = Logger.getInstance(this.javaClass.canonicalName)
    private val logConflicts = System.getProperty("ijkl.log.shortcut.conflicts").toBoolean()

    override fun initComponent() {
        // https://apple.stackexchange.com/questions/44921/how-to-remove-or-disable-a-default-keyboard-layout
        val layoutFileName = "US-no-alt.keylayout"
        val systemLayout = File("/Library/Keyboard Layouts/$layoutFileName")
        val userLayout = File("${System.getProperty("user.home")}/Library/Keyboard Layouts/$layoutFileName")
        if (SystemInfo.isMac && !userLayout.exists() && !systemLayout.exists()) {
            val inputStream = resourceInputStream(layoutFileName)
            val outputStream = userLayout.outputStream()
            try {
                inputStream.use {
                    outputStream.use {
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch(e: Exception) {
                TODO()
            }
        }

        val shortcutsData = resourceInputStream("ijkl-keymap.xml").readShortcutsData()
        var addShortcutsResult = AddShortcutsResult()

        registerKeymapListener(ApplicationManager.getApplication(), object: KeymapChangeListener {
            override fun onChange(oldKeymap: Keymap?, newKeymap: Keymap?) {
                if (oldKeymap != null) oldKeymap.remove(addShortcutsResult.added)
                if (newKeymap != null) addShortcutsResult = newKeymap.add(shortcutsData)

                if (logConflicts) {
                    logger.info(
                        "Switched keymap from '$oldKeymap' to '$newKeymap'. Shortcuts: " +
                            "added - ${addShortcutsResult.added.size}; " +
                            "already existed - ${addShortcutsResult.alreadyExisted.size}; " +
                            "conflicts - ${addShortcutsResult.conflictsByActionId.size}"
                    )
                    addShortcutsResult.conflictsByActionId.forEach { logger.info(it.toString()) }
                }
            }
        })
    }
}

fun resourceInputStream(fileName: String): InputStream =
    if (File(fileName).exists()) File(fileName).inputStream()
    else AppComponent::class.java.classLoader.getResource(fileName).openStream()
