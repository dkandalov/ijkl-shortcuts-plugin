package ijkl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo

class AppComponent: ApplicationComponent {
    override fun initComponent() {
        val logger = Logger.getInstance(this.javaClass.canonicalName)
        val application = ApplicationManager.getApplication()

        initOsxKeyLayoutInstaller(
            bundleName = "ijkl-keys.bundle",
            systemPathToBundle = "/Library/Keyboard Layouts/ijkl-keys.bundle",
            userPathToBundle = "${System.getProperty("user.home")}/Library/Keyboard Layouts/ijkl-keys.bundle",
            application = application,
            logger = logger
        )

        initCurrentKeymapModifier(
            keymapInputStream = resourceInputStream(if (SystemInfo.isMac) "ijkl-osx-keymap.xml" else "ijkl-keymap.xml"),
            application = application,
            logger = logger
        )
    }
}