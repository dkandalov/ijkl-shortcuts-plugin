package ijkl

import com.intellij.ide.*
import com.intellij.notification.NotificationDisplayType.STICKY_BALLOON
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.awt.KeyboardFocusManager

class Main : AppLifecycleListener {
    @Suppress("UnstableApiUsage")
    override fun appStarted() {
        val logger = Logger.getInstance(javaClass.canonicalName)
        val application = ApplicationManager.getApplication()
        NotificationsConfiguration.getNotificationsConfiguration().register(groupDisplayId, STICKY_BALLOON, true)

        installMacosKeyLayout(
            bundleName = "ijkl-keys.bundle",
            systemPathToBundle = "/Library/Keyboard Layouts/ijkl-keys.bundle",
            userPathToBundle = "${System.getProperty("user.home")}/Library/Keyboard Layouts/ijkl-keys.bundle",
            application = application,
            logger = logger
        )

        initCurrentKeymapModifier(
            keymapInputStream = resourceInputStream(if (SystemInfo.isMac) "ijkl-macos-keymap.xml" else "ijkl-keymap.xml"),
            application = application,
            logger = logger,
            actionManager = ActionManager.getInstance()
        )

        initEventReDispatch(
            ideEventQueue = IdeEventQueue.getInstance(),
            keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager(),
            parentDisposable = application
        )
    }
}
