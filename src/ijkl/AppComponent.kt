package ijkl

import com.intellij.ide.IdeEventQueue
import com.intellij.notification.NotificationDisplayType.STICKY_BALLOON
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.awt.AWTEvent
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*

class AppComponent: ApplicationComponent {

    override fun initComponent() {
        val logger = Logger.getInstance(this.javaClass.canonicalName)
        val application = ApplicationManager.getApplication()
        NotificationsConfiguration.getNotificationsConfiguration().register(groupDisplayId, STICKY_BALLOON, true)

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

        initEventReDispatch()
    }

    private fun initEventReDispatch() {
        fun KeyEvent.copy(excludeModifier: Int, keyCode: Int) =
            KeyEvent(
                source as Component,
                id,
                `when`,
                modifiers.and(excludeModifier.inv()),
                keyCode,
                keyCode.toChar()
            )

        val eventQueue = IdeEventQueue.getInstance()
        val dispatcher = object: IdeEventQueue.EventDispatcher {
            override fun dispatch(e: AWTEvent): Boolean {
                if (e !is KeyEvent) return false
                if (e.modifiers.and(ALT_MASK) == 0) return false

                if (e.keyCode == VK_K) {
                    eventQueue.dispatchEvent(e.copy(excludeModifier = 0, keyCode = VK_DOWN))
                    return true
                } else if (e.keyCode == VK_J) {
                    eventQueue.dispatchEvent(e.copy(excludeModifier = 0, keyCode = VK_LEFT))
                    return true
                } else if (e.keyCode == VK_I) {
                    eventQueue.dispatchEvent(e.copy(excludeModifier = 0, keyCode = VK_UP))
                    return true
                } else if (e.keyCode == VK_L) {
                    eventQueue.dispatchEvent(e.copy(excludeModifier = 0, keyCode = VK_RIGHT))
                    return true
                } else {
                    return false
                }
            }
        }
        eventQueue.addDispatcher(dispatcher, ApplicationManager.getApplication())
    }

    override fun disposeComponent() {}

    override fun getComponentName() = AppComponent::javaClass.name
}