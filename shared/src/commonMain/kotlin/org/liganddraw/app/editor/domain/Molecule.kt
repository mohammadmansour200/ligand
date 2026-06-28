package org.liganddraw.app.editor.domain

data class Atom(
    val x: Double,
    val y: Double,
    val z: Double,
    val symbol: String,
    val charge: Int,
)

enum class BondType {
    SINGLE, DOUBLE, TRIPLE, IONIC, HYDROGEN;
}

enum class BondDir {
    NONE, BEGINWEDGE, BEGINDASH;
}

data class Bond(
    val beginAtomIndex: Long,
    val endAtomIndex: Long,
    val type: BondType,
    val direction: BondDir
)

data class Molecule(
    val atoms: List<Atom>,
    val bonds: List<Bond>
)
