package ijkl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger

class AppComponent: ApplicationComponent {
    override fun initComponent() {
        val logger = Logger.getInstance(javaClass.canonicalName)
        val application = ApplicationManager.getApplication()

        initOsxKeyLayoutInstaller(
            bundleName = "ijkl-keys.bundle",
            systemPathToBundle = "/Library/Keyboard Layouts/ijkl-keys.bundle",
            userPathToBundle = "${System.getProperty("user.home")}/Library/Keyboard Layouts/ijkl-keys.bundle",
            application = application,
            logger = logger
        )

        initCurrentKeymapModifier(
            keymapInputStream = resourceInputStream("ijkl-keymap.xml"),
            shouldLogConflicts = System.getProperty("ijkl.log.shortcut.conflicts").toBoolean(),
            application = application,
            logger = logger
        )
    }
}