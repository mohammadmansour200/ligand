package org.ligand.app.editor.domain

data class StyleRun(
    val startIndex: Int,
    val endIndex: Int,
    val fontFamily: String,
    val fontSizePx: Float,
    val colorArgb: Long,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

enum class TextDirection { LTR, RTL, Auto }

data class TextBox(
    val id: String,
    val content: String,
    val runs: List<StyleRun>,
    val anchorX: Float,
    val anchorY: Float,
    val paragraphDirection: TextDirection = TextDirection.Auto,
)


