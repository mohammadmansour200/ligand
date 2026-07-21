package org.liganddraw.app.editor.domain

sealed interface Tool {
    object Pan : Tool
    object Select : Tool
    object SingleBond : Tool
    object WedgeBond : Tool
    object HashedWedgeBond : Tool
    data class Element(val symbol: String) : Tool
}