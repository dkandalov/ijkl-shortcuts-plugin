package ijkl

import com.intellij.ide.IdeEventQueue
import com.intellij.ide.IdeEventQueue.EventDispatcher
import com.intellij.openapi.application.Application
import com.intellij.openapi.ui.popup.IdePopupEventDispatcher
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.SystemInfo.isMac
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
    private val delegate: IjklEventDispatcher,
    private val focusOwnerFinder: FocusOwnerFinder,
    private val afterDispatch: (IjklIdePopupEventDispatcher) -> Unit
) : IdePopupEventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        val result = delegate.dispatch(event)
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
) : EventDispatcher {

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
            keyCode == VK_F || keyCode == VK_W ||
            keyCode == VK_U || keyCode == VK_O ||
            keyCode == VK_M || keyCode == VK_N
        if (!isIjkl) return null

        val component = focusOwnerFinder.find()
        if (!ideEventQueue.isPopupActive && !component.hasParentJTree() && !component.hasCommitDialogParent()) return null
        val isCommitDialog = component.hasCommitDialogParent()

        return when (keyCode) {
            VK_I -> if (isCommitDialog) null else copyWithoutAlt(VK_UP)
            VK_K -> if (isCommitDialog) null else copyWithoutAlt(VK_DOWN)
            VK_J -> if (isCommitDialog) copyWithModifier(VK_LEFT) else copyWithoutAlt(VK_LEFT)
            VK_L -> if (isCommitDialog) copyWithModifier(VK_RIGHT) else copyWithoutAlt(VK_RIGHT)
            VK_N -> if (isCommitDialog) copyWithoutAlt(VK_LEFT) else null
            VK_M -> if (isCommitDialog) copyWithoutAlt(VK_RIGHT) else null
            VK_F -> copyWithoutAlt(VK_PAGE_DOWN)
            VK_W -> copyWithoutAlt(VK_PAGE_UP)
            VK_U -> copyWithoutAlt(VK_HOME)
            VK_O -> copyWithoutAlt(VK_END)
            else -> error("")
        }
    }
}

// Using zero char because:
//  - if it's original letter, then navigation doesn't work in popups
//  - if it's some other letter, then it shows up Navigate to File/Class action
private const val zeroChar = 0.toChar()

private fun KeyEvent.copyWithoutAlt(keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiers.and(ALT_MASK.inv()),
        keyCode,
        zeroChar
    )


private fun KeyEvent.copyWithModifier(keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiers.or(if (isMac) ALT_MASK else CTRL_MASK),
        keyCode,
        zeroChar
    )

private fun Component?.hasParentJTree(): Boolean = when {
    this == null -> false
    this is JTree -> true
    else -> parent.hasParentJTree()
}

private fun Component?.hasCommitDialogParent(): Boolean = when {
    this == null -> false
    this.toString().contains("layout=com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog") -> true
    else -> parent.hasCommitDialogParent()
}
