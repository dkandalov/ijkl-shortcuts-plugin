package ijkl

import com.intellij.find.SearchTextArea
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.IdeEventQueue.EventDispatcher
import com.intellij.openapi.Disposable
import com.intellij.openapi.keymap.impl.ui.ShortcutTextField
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.util.stream.Stream
import javax.swing.JTree
import kotlin.LazyThreadSafetyMode.NONE

fun initEventReDispatch(
    ideEventQueue: IdeEventQueue,
    keyboardFocusManager: KeyboardFocusManager,
    parentDisposable: Disposable
) {
    val focusOwnerFinder = FocusOwnerFinder(keyboardFocusManager)
    val ijklDispatcher = IjklEventDispatcher(focusOwnerFinder, ideEventQueue)

    // This is a workaround to make sure ijkl dispatch works in popups,
    // because IdeEventQueue invokes popup dispatchers before custom dispatchers.
    val popupEventDispatcher = IjklIdePopupEventDispatcher(ijklDispatcher, focusOwnerFinder)
    ideEventQueue.addPreprocessor({ if (ideEventQueue.isPopupActive) ideEventQueue.popupManager.push(popupEventDispatcher); false }, parentDisposable)
    ideEventQueue.addPostprocessor({ if (ideEventQueue.isPopupActive) ideEventQueue.popupManager.remove(popupEventDispatcher); false }, parentDisposable)
    ideEventQueue.addDispatcher(ijklDispatcher, parentDisposable)
}

private class FocusOwnerFinder(private val keyboardFocusManager: KeyboardFocusManager) {
    fun find(): Component? = keyboardFocusManager.focusOwner ?: IdeFocusManager.findInstanceByContext(null).focusOwner
}

private class IjklIdePopupEventDispatcher(
    private val delegateDispatcher: IjklEventDispatcher,
    private val focusOwnerFinder: FocusOwnerFinder
) : IdePopupEventDispatcher {
    override fun dispatch(event: AWTEvent) = delegateDispatcher.dispatch(event)
    override fun getComponent() = focusOwnerFinder.find()
    override fun getPopupStream(): Stream<JBPopup> = Stream.empty()
    override fun requestFocus() = false
    override fun close() = false
}

private class IjklEventDispatcher(
    private val focusOwnerFinder: FocusOwnerFinder,
    private val ideEventQueue: IdeEventQueue
) : EventDispatcher {
    override fun dispatch(e: AWTEvent): Boolean {
        if (e !is KeyEvent) return false
        val newEvent = e.mapIfIjkl(Context(focusOwnerFinder, ideEventQueue)) ?: return false
        ideEventQueue.dispatchEvent(newEvent)
        return true
    }
}

interface KeyEventContext {
    val component: Component?
    val hasParentSearchTextArea: Boolean // True in file text search (cmd+F/R or cmd+shift+F/R)
    val hasParentTree: Boolean // True in Project View and similar components with trees
    val isPopupActive: Boolean // True in alt+enter, auto-completion, etc.
    val findActionsByShortcut: Boolean // True in the text field for "Project settings - Keymap: Find Actions by Shortcut"
}

private class Context(focusOwnerFinder: FocusOwnerFinder, ideEventQueue: IdeEventQueue) : KeyEventContext {
    override val component by lazy(NONE) { focusOwnerFinder.find() }
    override val hasParentSearchTextArea by lazy(NONE) { component.hasParentSearchTextArea() }
    override val hasParentTree by lazy(NONE) { component.hasParentTree() }
    override val isPopupActive by lazy(NONE) { ideEventQueue.isPopupActive }
    override val findActionsByShortcut by lazy(NONE) { component is ShortcutTextField }
}

/**
 * Use cases:
 * - project view tool window - ok
 * - alt+enter popup - ok
 * - auto-completion popup - ok (alt+l moves on char at a time)
 * - text search within file (cmd+F) - ok
 * - find in files (cmd+shift+F) - alt+k clashes with mnemonic (page up/down don't work after mismatch)
 * - file structure popup (cmd+F12) - ok
 * - go to classes/files/actions/etc (cmd+N) - ok
 * - commit dialog: alt+m, alt+i clash with mnemonics
 * - project settings - keymap: find actions by shortcut - ok
 * - project settings: speed search and move between usages - up/down doesn't work (unless you press tab to move focus to the tree)
 *   (because of hardcoded up/down shortcuts in com.intellij.openapi.options.newEditor.SettingsSearch.preprocessEventForTextField)
 *
 * For performance optimisation reasons the cheapest checks should be done first.
 * (There is no empirical evidence that these optimisations are actually useful though.)
 */
private fun KeyEvent.mapIfIjkl(context: KeyEventContext): KeyEvent? {
    if (modifiersEx.and(ALT_DOWN_MASK) == 0) return null
    if (context.findActionsByShortcut) return null
    if (context.hasParentSearchTextArea) return null

    if (context.isPopupActive && !context.hasParentTree) {
        when {
            keyCode == VK_I || keyChar == 'i'         -> return copyWithoutAlt(VK_UP, '↑')
            keyCode == VK_K || keyChar == 'k'         -> return copyWithoutAlt(VK_DOWN, '↓')
            keyCode == VK_J || keyChar == 'j'         -> return copyWithoutAlt(VK_LEFT, '←')
            keyCode == VK_L || keyChar == 'l'         -> return copyWithoutAlt(VK_RIGHT, '→')
            keyCode == VK_U || keyChar == 'u'         -> return copyWithoutAlt(VK_HOME)
            keyCode == VK_O || keyChar == 'o'         -> return copyWithoutAlt(VK_END)
            keyCode == VK_F || keyChar == 'f'         -> return copyWithoutAlt(VK_PAGE_DOWN, '⇟')
            keyCode == VK_W || keyChar == 'w'         -> return copyWithoutAlt(VK_PAGE_UP, '⇞')
            keyCode == VK_N || keyChar == 'n'         -> return copyWithoutAlt(VK_LEFT, '←') // Convert to keys without alt so that it's not interpret as typed characters by input field.
            keyCode == VK_M || keyChar == 'm'         -> return copyWithoutAlt(VK_RIGHT, '→') // Convert to keys without alt so that it's not interpret as typed characters by input field.
            keyCode == VK_SEMICOLON || keyChar == ';' -> return copyWithoutAlt(VK_DELETE) // Convert to keys without alt so that it's not interpret as typed characters by input field.
        }
    }

    if (context.hasParentTree) {
        // In some JDK versions (e.g. in jbrex8u152b1024.10) in trees with filter enabled (after typing some letters)
        // alt-ik events are processed as if up/down was pressed twice so some results are skipped.
        // This is a workaround to ignore KEY_TYPED events so that only KEY_PRESSED and KEY_RELEASED are mapped.
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

    return null
}

// Using zero char because:
//  - if it's original letter, then navigation doesn't work in popups
//  - if it's some other letter, then in Navigate to File/Class the letter is inserted into text area
private const val zeroChar = 0.toChar()

private fun KeyEvent.copyWithoutAlt(newKeyCode: Int, newKeyChar: Char? = null) =
    KeyEvent(
        source as Component,
        id,
        `when`,
        modifiersEx.and(ALT_DOWN_MASK.inv()),
        if (id == KEY_PRESSED) newKeyCode else 0,
        newKeyChar ?: zeroChar,
        keyLocation
    )

private tailrec fun Component?.hasParentTree(): Boolean =
    when {
        this == null  -> false
        this is JTree -> true
        else          -> parent.hasParentTree()
    }

private tailrec fun Component?.hasParentSearchTextArea(): Boolean =
    when {
        this == null           -> false
        this is SearchTextArea -> true
        else                   -> parent.hasParentSearchTextArea()
    }
