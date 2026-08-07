package org.liganddraw.app.editor.presentation.utils

class UndoRedoStack<T>(
    initial: T,
    private val maxHistory: Int = 50,
) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    var current: T = initial
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(newState: T) {
        undoStack.addLast(current)
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
        current = newState
    }

    fun undo(): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        current = previous
        return current
    }

    fun redo(): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        current = next
        return current
    }
}