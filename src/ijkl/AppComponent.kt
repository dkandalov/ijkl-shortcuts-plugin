package ijkl

import com.intellij.ide.IdeEventQueue
import com.intellij.notification.NotificationDisplayType.STICKY_BALLOON
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import javax.swing.JTree

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
        fun KeyEvent.copy(excludeModifier: Int = ALT_MASK, keyCode: Int) =
            KeyEvent(
                source as Component,
                id,
                `when`,
                modifiers.and(excludeModifier.inv()),
                keyCode,
                keyCode.toChar()
            )

        fun Component?.hasParentJTree(): Boolean =
            if (this == null) false
            else if (this is JTree) true
            else parent.hasParentJTree()

        val keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()

        fun focusIsInTree(): Boolean {
            val component = keyboardFocusManager.focusOwner ?: IdeFocusManager.findInstanceByContext(null).focusOwner
            return component.hasParentJTree()
        }

        val eventQueue = IdeEventQueue.getInstance()
        val dispatcher = object: IdeEventQueue.EventDispatcher {
            override fun dispatch(e: AWTEvent): Boolean {
                if (e !is KeyEvent) return false
                if (e.modifiers.and(ALT_MASK) == 0) return false

                val newEvent =
                    if (e.keyCode == VK_K) (e.copy(keyCode = VK_DOWN))
                    else if (e.keyCode == VK_I) e.copy(keyCode = VK_UP)
                    else if ((e.keyCode == VK_L || e.keyCode == VK_J) && focusIsInTree()) {
                        if (e.keyCode == VK_L) e.copy(keyCode = VK_RIGHT)
                        else if (e.keyCode == VK_J) e.copy(keyCode = VK_LEFT)
                        else error("")
                    } else null

                if (newEvent != null) {
                    eventQueue.dispatchEvent(newEvent)
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
