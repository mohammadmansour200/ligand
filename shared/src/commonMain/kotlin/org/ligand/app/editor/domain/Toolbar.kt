package org.ligand.app.editor.domain

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
    object ForwardArrow : Tool
    object ResonanceArrow : Tool
    object EquilibriumArrow : Tool
    object SingleElectronPushingArrow : Tool
    object ElectronPairPushingArrow : Tool
    data class Element(val symbol: String) : Tool
    data class Template(val smiles: String) : Tool
}