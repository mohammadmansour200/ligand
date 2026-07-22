package org.liganddraw.app.editor.data.mappers

import org.RDKit.Atom
import org.RDKit.Conformer

private val ELEMENTS_WITH_PREFIXED_HYDROGEN = setOf(
    "O", "F", "S", "Cl", "Se", "Br", "Te", "I", "Po", "At"
)

/**Determines whether its reversed (e.g. H2N) or not (e.g. NH2)*/
fun Atom.isLabelReversed(conformer: Conformer): Boolean {
    val isIsolatedAtom = this.degree == 0L
    if (isIsolatedAtom && this.symbol in ELEMENTS_WITH_PREFIXED_HYDROGEN) return true

    val isTerminalAtom = this.degree == 1L

    if (isTerminalAtom) {
        val neighborAtom = this.bonds[0].getOtherAtom(this)

        val atomPosition = conformer.getAtomPos(this.idx)
        val neighborAtomPosition = conformer.getAtomPos(neighborAtom.idx)

        val atomIsOnLeft = atomPosition.x < neighborAtomPosition.x
        return atomIsOnLeft
    }

    return false
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