package ijkl

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.Application
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Event.ALT_MASK
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import javax.swing.JTree

fun initEventReDispatch(
    eventQueue: IdeEventQueue,
    keyboardFocusManager: KeyboardFocusManager,
    application: Application
) {
    val dispatcher = IjklEventDispatcher(keyboardFocusManager, onSubstituteEvent = { event ->
        eventQueue.dispatchEvent(event)
    })
    eventQueue.addDispatcher(dispatcher, application)
}

private class IjklEventDispatcher(
    val keyboardFocusManager: KeyboardFocusManager,
    val onSubstituteEvent: (AWTEvent) -> Unit
) : IdeEventQueue.EventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        if (event !is KeyEvent) return false
        if (event.modifiers.and(ALT_MASK) == 0) return false

        val newEvent =
            if ((event.keyCode == VK_I || event.keyCode == VK_J || event.keyCode == VK_K || event.keyCode == VK_L) &&
                (focusIsInTree() || (event.component?.hasParentListPopup() ?: false))) {
                if (event.keyCode == VK_I) event.copy(keyCode = VK_UP)
                else if (event.keyCode == VK_J) event.copy(keyCode = VK_LEFT)
                else if (event.keyCode == VK_K) (event.copy(keyCode = VK_DOWN))
                else if (event.keyCode == VK_L) event.copy(keyCode = VK_RIGHT)
                else error("")
            } else {
                null
            }

        if (newEvent != null) {
            onSubstituteEvent(newEvent)
            return true
        } else {
            return false
        }
    }

    fun focusIsInTree(): Boolean {
        val component = keyboardFocusManager.focusOwner ?: IdeFocusManager.findInstanceByContext(null).focusOwner
        return component.hasParentJTree()
    }
}

private fun KeyEvent.copy(excludeModifier: Int = ALT_MASK, keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiers.and(excludeModifier.inv()),
        keyCode,
        keyCode.toChar()
    )

private fun Component?.hasParentJTree(): Boolean =
    if (this == null) false
    else if (this is JTree) true
    else parent.hasParentJTree()

private fun Component?.hasParentListPopup(): Boolean =
    if (this == null) false
    // There seems to be no easy way to determine from public classes in component hierarchy if we are in a popup.
    // Therefore, checking for class name which is more fragile.
    else if (this.javaClass.toString().contains("com.intellij.ui.popup.list.ListPopup")) true
    else parent.hasParentListPopup()
