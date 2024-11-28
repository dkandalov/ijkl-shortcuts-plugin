package ijkl

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.*
import ijkl.MoveType.*

// Actions to move cursor left/right (similar to com.intellij.openapi.editor.actions.TextStartAction)
// which work outside of code editor, e.g. in the text search input field.

class MoveCaretLeftAction : TextComponentEditorAction(CareMoveHandler(Left))
class MoveCaretRightAction : TextComponentEditorAction(CareMoveHandler(Right))
class MoveCaretLeftWithSelectionAction : TextComponentEditorAction(CareMoveHandler(LeftWithSelection))
class MoveCaretRightWithSelectionAction : TextComponentEditorAction(CareMoveHandler(RightWithSelection))

private class CareMoveHandler(private val moveType: MoveType) : EditorActionHandler.ForEachCaret() {
    public override fun doExecute(editor: Editor, caret: Caret, dataContext: DataContext) {
        when (moveType) {
            Left               -> if (caret.offset != 0) EditorActionUtil.moveCaret(caret, caret.offset - 1, false)
            LeftWithSelection  -> if (caret.offset != 0) EditorActionUtil.moveCaret(caret, caret.offset - 1, true)
            Right              -> EditorActionUtil.moveCaret(caret, caret.offset + 1, false)
            RightWithSelection -> EditorActionUtil.moveCaret(caret, caret.offset + 1, true)
        }
    }
}

private enum class MoveType {
    Left, Right, LeftWithSelection, RightWithSelection
}
