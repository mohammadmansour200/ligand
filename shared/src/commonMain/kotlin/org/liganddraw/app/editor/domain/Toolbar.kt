package org.liganddraw.app.editor.domain

sealed interface Tool {
    object Pan : Tool
    object StructureSelect : Tool
    object Erase : Tool
    object Plus : Tool
    object Minus : Tool
    object SingleBond : Tool
    object WedgeBond : Tool
    object HashedWedgeBond : Tool
    object DoubleBond : Tool
    object TripleBond : Tool
    object HydrogenBond : Tool
    object Chain : Tool
    data class Element(val symbol: String) : Tool
    data class Template(val smiles: String) : Tool
}