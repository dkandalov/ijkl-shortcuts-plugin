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
    val focusOwnerFinder = FocusOwnerFinder(keyboardFocusManager)
    val dispatcher = IjklEventDispatcher(focusOwnerFinder, ideEventQueue)

    // This is a workaround to make sure ijkl dispatch works in popups,
    // because IdeEventQueue invokes popup dispatchers before custom dispatchers.
    val popupEventDispatcher = IjklIdePopupEventDispatcher(dispatcher, focusOwnerFinder, afterDispatch = {
        ideEventQueue.popupManager.remove(it)
    })
    ideEventQueue.addActivityListener(Runnable {
        if (ideEventQueue.isPopupActive) {
            ideEventQueue.popupManager.push(popupEventDispatcher)
        }
    }, application)

    ideEventQueue.addDispatcher(dispatcher, application)
}

private class FocusOwnerFinder(private val keyboardFocusManager: KeyboardFocusManager) {
    fun find(): Component? = keyboardFocusManager.focusOwner ?: IdeFocusManager.findInstanceByContext(null).focusOwner
}

private class IjklIdePopupEventDispatcher(
    private val dispatcher: IjklEventDispatcher,
    private val focusOwnerFinder: FocusOwnerFinder,
    private val afterDispatch: (IjklIdePopupEventDispatcher) -> Unit
) : IdePopupEventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        val result = dispatcher.dispatch(event)
        afterDispatch(this)
        return result
    }
    override fun getComponent() = focusOwnerFinder.find()
    override fun getPopupStream(): Stream<JBPopup> = Stream.empty()
    override fun requestFocus() = false
    override fun close() = false
    override fun setRestoreFocusSilentely() {}
}

private class IjklEventDispatcher(
    private val focusOwnerFinder: FocusOwnerFinder,
    private val ideEventQueue: IdeEventQueue
) : IdeEventQueue.EventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        if (event !is KeyEvent) return false
        if (event.modifiers.and(ALT_MASK) == 0) return false

        val newEvent = event.mapIfIjkl()

        return if (newEvent != null) {
            ideEventQueue.dispatchEvent(newEvent)
            true
        } else {
            false
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun KeyEvent.mapIfIjkl(): KeyEvent? {
        // For performance optimisation reasons do the cheapest checks first, i.e. key code, popup then focus in tree.
        // There is not empirical evidence these optimisations actually useful though.
        val isIjkl =
            keyCode == VK_I || keyCode == VK_J ||
            keyCode == VK_K || keyCode == VK_L ||
            keyCode == VK_F || keyCode == VK_W
        if (!isIjkl) return null
        if (!ideEventQueue.isPopupActive && !focusIsInTree()) return null

        return when (keyCode) {
            VK_I -> copyWithoutAlt(VK_UP)
            VK_J -> copyWithoutAlt(VK_LEFT)
            VK_K -> copyWithoutAlt(VK_DOWN)
            VK_L -> copyWithoutAlt(VK_RIGHT)
            VK_F -> copyWithoutAlt(VK_PAGE_DOWN)
            VK_W -> copyWithoutAlt(VK_PAGE_UP)
            else -> error("")
        }
    }

    private fun focusIsInTree() = focusOwnerFinder.find().hasParentJTree()
}

private fun KeyEvent.copyWithoutAlt(keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiers.and(ALT_MASK.inv()),
        keyCode,
        // Using zero char because:
        //  - if it's original letter, then navigation doesn't work in popups
        //  - if it's some other letter, then it shows up Navigate to File/Class action
        0.toChar()
    )

private fun Component?.hasParentJTree(): Boolean =
    if (this == null) false
    else if (this is JTree) true
    else parent.hasParentJTree()
