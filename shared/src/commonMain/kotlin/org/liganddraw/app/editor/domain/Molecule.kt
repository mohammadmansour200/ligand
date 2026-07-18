package org.liganddraw.app.editor.domain

data class Atom(
    val x: Double,
    val y: Double,
    val z: Double,
    val symbol: String,
    val numImplicitHydrogen: Long,
    val charge: Int,
    val isLabelReversed: Boolean
)

enum class BondDir {
    NONE, BEGINWEDGE, BEGINDASH;
}

enum class DoubleBondAlignment {
    CENTERED, POSITIVE, NEGATIVE
}

sealed interface Bond {
    val beginAtomIndex: Long
    val endAtomIndex: Long

    data class Single(
        override val beginAtomIndex: Long,
        override val endAtomIndex: Long,
        val direction: BondDir = BondDir.NONE
    ) : Bond

    data class Double(
        override val beginAtomIndex: Long,
        override val endAtomIndex: Long,
        val alignment: DoubleBondAlignment
    ) : Bond

    data class Triple(
        override val beginAtomIndex: Long,
        override val endAtomIndex: Long
    ) : Bond

    data class Ionic(
        override val beginAtomIndex: Long,
        override val endAtomIndex: Long
    ) : Bond

    data class Hydrogen(
        override val beginAtomIndex: Long,
        override val endAtomIndex: Long
    ) : Bond
}

data class Molecule(
    val atoms: List<Atom>,
    val bonds: List<Bond>
)

data class MoleculeProperties(
    val logp: Double,
    val molecularWeight: Double,
    val hydrogenBondAcceptors: Long,
    val hydrogenBondDonors: Long,
    val rotatableBonds: Long
)
