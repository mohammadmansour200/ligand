package org.ligand.app.editor.presentation.molecule_pane.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.Texture
import io.github.erkko68.filament.TextureSampler
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.scene.primitives.Mesh
import io.github.erkko68.filament.compose.scene.rememberMaterial
import io.github.erkko68.filament.compose.scene.rememberMaterialInstance
import org.ligand.app.editor.domain.EPMMeshData

/**
 * Builds a 1-row, [width]-column RGBA8 pixel buffer for a blue-white-red diverging gradient
 * (blue = electron-poor/positive, white = neutral, red = electron-rich/negative). Piecewise
 * linear: blue -> white over t in [0, 0.5], white -> red over t in [0.5, 1].
 */
private fun buildRedWhiteBlueLutRgba(width: Int = 256): ByteArray {
    val bytes = ByteArray(width * 4)
    for (x in 0 until width) {
        val t = x / (width - 1f) // 0 = blue, 0.5 = white, 1 = red
        var red: Int
        var green: Int
        var blue: Int
        if (t < 0.5f) {
            val local = t / 0.5f // 0 at blue, 1 at white
            red = (255 * local).toInt()
            green = (255 * local).toInt()
            blue = 255
        } else {
            val local = (t - 0.5f) / 0.5f // 0 at white, 1 at red
            red = 255
            green = (255 * (1f - local)).toInt()
            blue = (255 * (1f - local)).toInt()
        }
        val o = x * 4
        bytes[o] = red.coerceIn(0, 255).toByte()
        bytes[o + 1] = green.coerceIn(0, 255).toByte()
        bytes[o + 2] = blue.coerceIn(0, 255).toByte()
        bytes[o + 3] = 255.toByte() // opaque; overall translucency comes from the material's alpha
    }
    return bytes
}

/**
 * Uploads a red-green-blue gradient LUT to a Filament [Texture] using the
 * `PixelBufferDescriptor(storage, sizeInBytes, format, type, alignment, left, top, stride, callback)`
 * constructor. Call once (e.g. in a `remember { }`) and reuse the returned texture across frames —
 * no need to rebuild it per molecule, only the vertex UVs change.
 */
private fun createEpmLutTexture(engine: Engine, width: Int = 256): Texture {
    val pixels = buildRedWhiteBlueLutRgba(width) // RGBA8, tightly packed, 4 bytes/pixel

    val texture = Texture.Builder()
        .width(width)
        .height(1)
        .levels(1)
        .sampler(Texture.Sampler.SAMPLER_2D)
        .format(Texture.InternalFormat.RGBA8)
        .build(engine)

    val descriptor = Texture.PixelBufferDescriptor(
        storage = pixels,
        sizeInBytes = pixels.size,
        format = Texture.Format.RGBA,
        type = Texture.Type.UBYTE,
        alignment = 1,   // RGBA8 rows are always 4-byte aligned regardless of width, but 1 is always safe
        left = 0,
        top = 0,
        stride = width,  // pixels per row; matches width since the buffer is tightly packed
    )

    texture.setImage(engine, 0, descriptor)
    return texture
}

@Composable
private fun rememberEPMSurfaceInstance(
    engine: Engine,
    materialBytes: ByteArray,
    opacity: Float = 0.3f,
): MaterialInstance? {
    val template = rememberMaterial { materialBytes }

    val lutTexture = remember { createEpmLutTexture(engine) }
    val sampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.CLAMP_TO_EDGE,
    )
    return rememberMaterialInstance(
        template, lutTexture, sampler
    ) {
        setParameter("lutSampler", lutTexture, sampler)
        setParameter("opacity", opacity)
    }
}

@Composable
fun FilamentSceneScope.EPMMoleculeSurface(
    engine: Engine,
    meshData: EPMMeshData,
    epmMaterialBytes: ByteArray,
    isVisible: Boolean
) {
    val material = rememberEPMSurfaceInstance(engine, epmMaterialBytes)

    if (isVisible) {
        Mesh(
            material = material,
            positions = meshData.positions,
            normals = meshData.normals,
            uvs = meshData.uvs,
            indices = meshData.indices,
            castShadows = false,
            receiveShadows = false,
        )
    }
}
