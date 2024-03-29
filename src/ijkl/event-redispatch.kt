package ijkl

import com.intellij.find.SearchTextArea
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.IdeEventQueue.EventDispatcher
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.IdePopupEventDispatcher
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.SystemInfo.isMac
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.ALT_DOWN_MASK
import java.awt.event.KeyEvent.CTRL_DOWN_MASK
import java.awt.event.KeyEvent.KEY_TYPED
import java.awt.event.KeyEvent.VK_DELETE
import java.awt.event.KeyEvent.VK_DOWN
import java.awt.event.KeyEvent.VK_END
import java.awt.event.KeyEvent.VK_F
import java.awt.event.KeyEvent.VK_HOME
import java.awt.event.KeyEvent.VK_I
import java.awt.event.KeyEvent.VK_J
import java.awt.event.KeyEvent.VK_K
import java.awt.event.KeyEvent.VK_L
import java.awt.event.KeyEvent.VK_LEFT
import java.awt.event.KeyEvent.VK_M
import java.awt.event.KeyEvent.VK_N
import java.awt.event.KeyEvent.VK_O
import java.awt.event.KeyEvent.VK_PAGE_DOWN
import java.awt.event.KeyEvent.VK_PAGE_UP
import java.awt.event.KeyEvent.VK_RIGHT
import java.awt.event.KeyEvent.VK_SEMICOLON
import java.awt.event.KeyEvent.VK_U
import java.awt.event.KeyEvent.VK_UP
import java.awt.event.KeyEvent.VK_W
import java.util.stream.Stream
import javax.swing.JTree
import kotlin.LazyThreadSafetyMode.NONE

fun initEventReDispatch(
    ideEventQueue: IdeEventQueue,
    keyboardFocusManager: KeyboardFocusManager,
    parentDisposable: Disposable
) {
    val focusOwnerFinder = FocusOwnerFinder(keyboardFocusManager)
    val ijklEventDispatcher = IjklEventDispatcher(focusOwnerFinder, ideEventQueue)

    // This is a workaround to make sure ijkl dispatch works in popups,
    // because IdeEventQueue invokes popup dispatchers before custom dispatchers.
    val popupEventDispatcher = IjklIdePopupEventDispatcher(ijklEventDispatcher, focusOwnerFinder, afterDispatch = { dispatcher ->
        ideEventQueue.popupManager.remove(dispatcher)
    })
    ideEventQueue.addActivityListener({
        if (ideEventQueue.isPopupActive) {
            ideEventQueue.popupManager.push(popupEventDispatcher)
        }
    }, parentDisposable)

    ideEventQueue.addDispatcher(ijklEventDispatcher, parentDisposable)
}

private class FocusOwnerFinder(private val keyboardFocusManager: KeyboardFocusManager) {
    fun find(): Component? = keyboardFocusManager.focusOwner ?: IdeFocusManager.findInstanceByContext(null).focusOwner
}

private class IjklIdePopupEventDispatcher(
    private val delegateDispatcher: IjklEventDispatcher,
    private val focusOwnerFinder: FocusOwnerFinder,
    private val afterDispatch: (IjklIdePopupEventDispatcher) -> Unit
): IdePopupEventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        val result = delegateDispatcher.dispatch(event)
        afterDispatch(this)
        return result
    }

    override fun getComponent() = focusOwnerFinder.find()
    override fun getPopupStream(): Stream<JBPopup> = Stream.empty()
    override fun requestFocus() = false
    override fun close() = false
}

private class IjklEventDispatcher(
    private val focusOwnerFinder: FocusOwnerFinder,
    private val ideEventQueue: IdeEventQueue
): EventDispatcher {

    override fun dispatch(event: AWTEvent): Boolean {
        if (event !is KeyEvent) return false
        val newEvent = event.mapIfIjkl(Context(focusOwnerFinder, ideEventQueue)) ?: return false
        ideEventQueue.dispatchEvent(newEvent)
        return true
    }
}

interface KeyEventContext {
    val hasParentSearchTextArea: Boolean
    val hasParentChooseByName: Boolean
    val hasParentTree: Boolean
    val hasParentCommitDialog: Boolean
    val hasParentWizardPopup: Boolean
    val isNewCommitDialog: Boolean
    val isPopupActive: Boolean
}

private class Context(focusOwnerFinder: FocusOwnerFinder, ideEventQueue: IdeEventQueue) : KeyEventContext {
    val component by lazy(NONE) { focusOwnerFinder.find() }
    override val hasParentSearchTextArea by lazy(NONE) { component.hasParentSearchTextArea() }
    override val hasParentChooseByName by lazy(NONE) { component.hasParentChooseByName() }
    override val hasParentTree by lazy(NONE) { component.hasParentTree() }
    override val hasParentCommitDialog by lazy(NONE) { component.hasParentCommitDialog() }
    override val hasParentWizardPopup by lazy(NONE) { component.hasParentWizardPopup() }
    override val isNewCommitDialog by lazy(NONE) { component.toString().contains("EditorComponent") }
    override val isPopupActive by lazy(NONE) { ideEventQueue.isPopupActive }
}

// For performance optimisation reasons do the cheapest checks first, i.e. key code, popup, focus in tree.
// There is no empirical evidence that these optimisations are actually useful though.
@Suppress("NOTHING_TO_INLINE")
private inline fun KeyEvent.mapIfIjkl(context: KeyEventContext): KeyEvent? {
    if (modifiersEx.and(ALT_DOWN_MASK) == 0) return null

    val isIjkl =
        keyCode == VK_I || keyCode == VK_J ||
            keyCode == VK_K || keyCode == VK_L ||
            keyCode == VK_F || keyCode == VK_W ||
            keyCode == VK_U || keyCode == VK_O ||
            keyCode == VK_M || keyCode == VK_N ||
            keyCode == VK_SEMICOLON
    if (!isIjkl) return null

    // Workarounds for search in current file, Find in Path popup, in Find Class/File popup.
    // (Must be done before hasParentTree() because Find in Path popup has a tree but alt+jl shouldn't be mapped like that for a tree.)
    if (context.hasParentSearchTextArea || context.hasParentChooseByName) {
        when (keyCode) {
            VK_I         -> return copyWithoutAlt(VK_UP)
            VK_K         -> return copyWithoutAlt(VK_DOWN)
            VK_J         -> return copyWithModifier(VK_LEFT)
            VK_L         -> return copyWithModifier(VK_RIGHT)
            VK_U         -> return copyWithoutAlt(VK_HOME)
            VK_O         -> return copyWithoutAlt(VK_END)
            VK_N         -> return copyWithoutAlt(VK_LEFT) // Convert to keys without alt so that it's not interpret as typed characters by input field.
            VK_M         -> return copyWithoutAlt(VK_RIGHT) // Convert to keys without alt so that it's not interpret as typed characters by input field.
            VK_SEMICOLON -> return copyWithoutAlt(VK_DELETE) // Convert to keys without alt so that it's not interpret as typed characters by input field.
        }
    }

    val hasParentTree = context.hasParentTree
    if (hasParentTree) {
        // In some JDK versions (e.g. in jbrex8u152b1024.10) in trees with filter enabled (by typing some letters)
        // alt-ik events are processed as if up/down was pressed twice so some results are skipped.
        // This is a workaround to ignore KEY_TYPED event so that only KEY_PRESSED and KEY_RELEASED are mapped.
        if (id == KEY_TYPED) return null

        when (keyCode) {
            VK_I -> return copyWithoutAlt(VK_UP)
            VK_K -> return copyWithoutAlt(VK_DOWN)
            VK_J -> return copyWithoutAlt(VK_LEFT) // Convert to "left" so that in trees it works as "collapse node".
            VK_L -> return copyWithoutAlt(VK_RIGHT) // Convert to "right" so that in trees it works as "expand node".
            VK_F -> return copyWithoutAlt(VK_PAGE_DOWN)
            VK_W -> return copyWithoutAlt(VK_PAGE_UP)
            VK_U -> return copyWithoutAlt(VK_HOME)
            VK_O -> return copyWithoutAlt(VK_END)
        }
    }

    val useCommitDialogWorkarounds = !hasParentTree && context.hasParentCommitDialog
    if (useCommitDialogWorkarounds) {
        when (keyCode) {
            VK_I -> return null // No mapping so that alt+i triggers "Commit" action.
            VK_K -> return null // No mapping for the symmetry with VK_I.
            VK_J -> return copyWithModifier(VK_LEFT) // Override for the symmetry with the VK_L.
            VK_L -> return copyWithModifier(VK_RIGHT) // Override mnemonic for "Clean code" (assuming move left is used more often).
            VK_N -> return copyWithoutAlt(VK_LEFT) // Override for the symmetry with the VK_M.
            VK_M -> return copyWithoutAlt(VK_RIGHT) // Override mnemonic for "Amend commit" (assuming that commits are not amended very often).
            VK_U -> return copyWithoutAlt(VK_HOME) // Override for the symmetry with the VK_O.
            VK_O -> return copyWithoutAlt(VK_END) // Override mnemonic for "Optimise imports".
        }
    }

    // Starting from #IU-192.5281.24 (2019.2 EAP) there is new commit dialog
    // which seems to be "stealing" alt+i from editor (probably for "Commit" button mnemonics).
    // This is a workaround to make sure editor receives alt+i as VK_UP.
    if (context.isNewCommitDialog) {
        when (keyCode) {
            VK_I -> return copyWithoutAlt(VK_UP)
        }
    }

    if (context.isPopupActive) {
        if (context.hasParentWizardPopup) {
            when (keyCode) {
                VK_J -> return copyWithoutAlt(VK_LEFT) // Convert to "left" so that it works as "collapse sub-menu".
                VK_L -> return copyWithoutAlt(VK_RIGHT) // Convert to "right" so that it works as "expand sub-menu".
            }
        }
        when (keyCode) {
            VK_F -> return copyWithoutAlt(VK_PAGE_DOWN)
            VK_W -> return copyWithoutAlt(VK_PAGE_UP)
        }
    }

    return null
}

// Using zero char because:
//  - if it's original letter, then navigation doesn't work in popups
//  - if it's some other letter, then in Navigate to File/Class the letter is inserted into text area
private const val zeroChar = 0.toChar()

private fun KeyEvent.copyWithoutAlt(keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiersEx.and(ALT_DOWN_MASK.inv()),
        keyCode,
        zeroChar
    )

private fun KeyEvent.copyWithModifier(keyCode: Int) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiersEx.or(if (isMac) ALT_DOWN_MASK else CTRL_DOWN_MASK),
        keyCode,
        zeroChar
    )

private tailrec fun Component?.hasParentTree(): Boolean = when {
    this == null  -> false
    this is JTree -> true
    else          -> parent.hasParentTree()
}

private tailrec fun Component?.hasParentSearchTextArea(): Boolean = when {
    this == null           -> false
    this is SearchTextArea -> true
    else                   -> parent.hasParentSearchTextArea()
}

private tailrec fun Component?.hasParentCommitDialog(): Boolean = when {
    this == null -> false
    this.toString()
        .let {
            it.contains("layout=com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog") || // Heuristic for IJ old commit dialog.
            it.contains("com.intellij.openapi.vcs.ui.CommitMessage") // Heuristic for IJ new commit dialog inside "Version Control" toolwindow.
        }
                 -> true
    else         -> parent.hasParentCommitDialog()
}

/**
 * Ideally this would be a check for "Wizard" in component class name but it doesn't work in Rider.
 */
private fun Component?.hasParentWizardPopup() = !hasParentChooseByName()

private tailrec fun Component?.hasParentChooseByName(): Boolean = when {
    this == null                                 -> false
    this.javaClass.name.contains("ChooseByName") -> true
    else                                         -> parent.hasParentChooseByName()
}
