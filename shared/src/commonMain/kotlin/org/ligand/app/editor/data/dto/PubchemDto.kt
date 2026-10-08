package org.ligand.app.editor.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IupacNamePropertyResponseDto(
    @SerialName("PropertyTable")
    val propertyTable: PropertyTableDto
)

@Serializable
data class PropertyTableDto(
    @SerialName("Properties")
    val properties: List<PropertyDto>
)

@Serializable
data class PropertyDto(
    @SerialName("IUPACName")
    val iupacName: String? = null,
    @SerialName("Title")
    val synonym: String? = null
)