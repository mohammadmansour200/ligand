package org.ligand.app.editor.data.mappers

import org.RDKit.Atom
import org.RDKit.Conformer
import org.ligand.app.editor.domain.LabelOrientation
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private val ELEMENTS_WITH_PREFIXED_HYDROGEN = setOf(
    "O", "F", "S", "Cl", "Se", "Br", "Te", "I", "Po", "At"
)

/**
 * Determines whether hydrogen count placement, whether its to the left (WEST), or to the right (EAST), or on top (NORTH), or on the bottom (SOUTH) of the symbol.
 */
fun Atom.labelOrientation(conformer: Conformer): LabelOrientation {
    // --- NO HYDROGENS ---
    if (this.totalNumHs == 0L) return LabelOrientation.EAST

    val isIsolatedAtom = this.degree == 0L
    // --- i.e. HCl, H2O, HI etc... ---
    if (isIsolatedAtom && this.symbol in ELEMENTS_WITH_PREFIXED_HYDROGEN) return LabelOrientation.WEST
    // --- i.e. NH3 ---
    if (isIsolatedAtom && this.symbol !in ELEMENTS_WITH_PREFIXED_HYDROGEN) return LabelOrientation.EAST

    val atomPosition = conformer.getAtomPos(this.idx)
    val isTerminalAtom = this.degree == 1L

    // --- TERMINAL ATOM ---
    if (isTerminalAtom) {
        val neighborAtom = this.bonds[0].getOtherAtom(this)

        val neighborAtomPosition = conformer.getAtomPos(neighborAtom.idx)

        val atomIsOnLeft = atomPosition.x < neighborAtomPosition.x
        return if (atomIsOnLeft) LabelOrientation.WEST else LabelOrientation.EAST
    } else {
        // --- NON-TERMINAL ATOM ---

        // Determine angles between atom and bonding atoms in degrees
        val angles = mutableListOf<Double>()
        for (i in 0 until this.bonds.size()) {
            val otherAtom = this.bonds[i.toInt()].getOtherAtom(this)
            val otherAtomPosition = conformer.getAtomPos(otherAtom.idx)

            val dx = otherAtomPosition.x - atomPosition.x
            val dy = otherAtomPosition.y - atomPosition.y
            val distance = sqrt(dx * dx + dy * dy)
            val ux = dx / distance
            val uy = dy / distance
            val angle = atan2(uy, ux) * (180.0 / PI)

            angles.add(angle)
        }

        // Sort angles ascendingly
        angles.sort()

        // Determine gaps between each pair of bonding atoms
        val gaps = mutableListOf<Double>()
        for (i in angles.indices) {
            val currentAngle = angles[i]

            val isLastAngle = i == angles.size - 1
            if (isLastAngle) {
                val firstAngle = angles[0]
                val gap = firstAngle + 360.0 - currentAngle
                gaps.add(gap)
            } else {
                val nextAngle = angles[i + 1]
                val gap = currentAngle - nextAngle
                gaps.add(gap)
            }
        }

        // Determine bisector
        val largestGap = gaps.max()
        val largestGapIdx = gaps.indexOf(largestGap)
        val largestGapStart = angles[largestGapIdx]

        var bisector = largestGapStart + largestGap / 2.0
        if (bisector > 180.0) bisector -= 360.0

        val orientation = when (bisector) {
            in -45.0..45.0 -> LabelOrientation.EAST
            in 45.0..135.0 -> LabelOrientation.NORTH
            in -135.0..-45.0 -> LabelOrientation.SOUTH
            else -> LabelOrientation.WEST
        }

        return orientation
    }
}

/**Determines whether to show label on 2D canvas or not*/
fun Atom.isLabelVisible(): Boolean {
    val isCarbon = this.symbol == "C"

    // Heteroatoms
    if (!isCarbon) return true

    // Methane
    val isIsolated = this.degree == 0L
    if (isIsolated) return true

    // Ethane, Methanol etc.
    val otherAtom = this.bonds[0].getOtherAtom(this)
    val isDiatomic = this.degree == 1L && otherAtom.degree == 1L

    return isDiatomic
}