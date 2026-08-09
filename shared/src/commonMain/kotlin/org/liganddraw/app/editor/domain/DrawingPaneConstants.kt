package org.liganddraw.app.editor.domain

object DrawingPaneConstants {
    val SUPPORTED_LOWERCASE_EXTENSIONS = listOf("sdf", "mol")
    const val SCALE_FACTOR = 40f
    const val ROTATION_SNAP_DEGREES = 15.0
    const val BOND_LENGTH = 1.5
    const val BOND_STROKE_WIDTH = 2f
    const val BOND_LINES_SPACING = 6f
    const val CENTERED_DOUBLE_BOND_LINES_SPACING = 3f
    const val SYMBOL_FONT_SIZE = 24f
    const val HYDROGEN_COUNT_FONT_SIZE = 20f
    const val HIGHLIGHT_STROKE_WIDTH = 2f
    const val HIGHLIGHT_CORNER_RADIUS = 8f
    const val BOND_HIT_TOLERANCE = 20f
    const val ATOM_HIT_TOLERANCE = 5f
    const val BENZENE = "c1ccccc1"
    const val NAPHTHALENE = "c1ccc2ccccc2c1"
    const val CYCLOPROPANE = "C1CC1"
    const val CYCLOBUTANE = "C1CCC1"
    const val CYCLOPENTANE = "C1CCCC1"
    const val CYCLOHEXANE = "C1CCCCC1"
    const val CYCLOHEPTANE = "C1CCCCCC1"
    const val CYCLOOCTANE = "C1CCCCCCC1"
    const val MAX_FORMAL_CHARGE = 1
    const val MIN_FORMAL_CHARGE = -1
}