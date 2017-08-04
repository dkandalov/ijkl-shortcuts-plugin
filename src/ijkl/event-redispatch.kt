package ijkl

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.Application
import com.intellij.openapi.ui.popup.IdePopupEventDispatcher
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Event.ALT_MASK
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.util.stream.Stream
import javax.swing.JTree

fun initEventReDispatch(
    ideEventQueue: IdeEventQueue,
    keyboardFocusManager: KeyboardFocusManager,
    application: Application
) {
    val dispatcher = IjklEventDispatcher(keyboardFocusManager, ideEventQueue)

    // This is a workaround to make sure ijkl dispatch works in popups,
    // because IdeEventQueue invokes popup dispatchers before custom dispatchers.
    ideEventQueue.addActivityListener(Runnable {
        if (ideEventQueue.isPopupActive) {
            val component = keyboardFocusManager.focusOwner ?: IdeFocusManager.findInstanceByContext(null).focusOwner
            ideEventQueue.popupManager.push(object: IdePopupEventDispatcher {
                override fun dispatch(event: AWTEvent) = dispatcher.dispatch(event)
                override fun getComponent() = component
                override fun getPopupStream(): Stream<JBPopup> = Stream.empty()
                override fun requestFocus() = false
                override fun close() = false
                override fun setRestoreFocusSilentely() {}
            })
        }
    }, application)

    ideEventQueue.addDispatcher(dispatcher, application)
}

private class IjklEventDispatcher(
    private val keyboardFocusManager: KeyboardFocusManager,
    private val ideEventQueue: IdeEventQueue
) : IdeEventQueue.EventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        if (event !is KeyEvent) return false
        if (event.modifiers.and(ALT_MASK) == 0) return false

        val ijIJKL = event.keyCode == VK_I || event.keyCode == VK_J || event.keyCode == VK_K || event.keyCode == VK_L
        val newEvent =
            if (ijIJKL && (focusIsInTree() || ideEventQueue.isPopupActive)) {
                println("ijkl")
                if (event.keyCode == VK_I) event.copyWithoutAlt(VK_UP)
                else if (event.keyCode == VK_J) event.copyWithoutAlt(VK_LEFT)
                else if (event.keyCode == VK_K) (event.copyWithoutAlt(VK_DOWN))
                else if (event.keyCode == VK_L) event.copyWithoutAlt(VK_RIGHT)
                else error("")
            } else {
                null
            }

        if (newEvent != null) {
            ideEventQueue.dispatchEvent(newEvent)
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

private fun KeyEvent.copyWithoutAlt(keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiers.and(ALT_MASK.inv()),
        keyCode,
        keyCode.toChar()
    )

private fun Component?.hasParentJTree(): Boolean =
    if (this == null) false
    else if (this is JTree) true
    else parent.hasParentJTree()

