package ijkl

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.editor.actions.*
import com.intellij.openapi.ide.CopyPasteManager
import ijkl.MoveType.*

// Actions to which work outside of code editor, e.g. in the text search input field.
// Ideally, they should be part of IDE TODO find/raise youtrack issue

class MoveCaretLeftAction : TextComponentEditorAction(CareMoveHandler(Left))
class MoveCaretRightAction : TextComponentEditorAction(CareMoveHandler(Right))
class MoveCaretLeftWithSelectionAction : TextComponentEditorAction(CareMoveHandler(LeftWithSelection))
class MoveCaretRightWithSelectionAction : TextComponentEditorAction(CareMoveHandler(RightWithSelection))

// Based on com.intellij.openapi.editor.actions.DeleteToWordBoundaryHandler
class DeleteAction : TextComponentEditorAction(object : EditorWriteActionHandler.ForEachCaret() {
    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        CommandProcessor.getInstance().currentCommandGroupId = EditorActionUtil.DELETE_COMMAND_GROUP
        CopyPasteManager.getInstance().stopKillRings()

        if (editor.selectionModel.hasSelection()) {
            EditorModificationUtil.deleteSelectedText(editor)
            return
        }
        val offset = editor.caretModel.offset
        editor.document.deleteString(offset, offset + 1)
    }
})

// Based on com.intellij.openapi.editor.actions.TextStartAction
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
