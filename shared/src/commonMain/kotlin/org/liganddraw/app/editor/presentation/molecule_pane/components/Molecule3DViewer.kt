package org.liganddraw.app.editor.presentation.molecule_pane.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.FilamentView
import io.github.erkko68.filament.compose.LocalFilamentEngine
import io.github.erkko68.filament.compose.orbitGestures
import io.github.erkko68.filament.compose.rememberFilamentEngine
import io.github.erkko68.filament.compose.rememberFilamentScene
import io.github.erkko68.filament.compose.rememberOrbitCameraState
import io.github.erkko68.filament.compose.scene.AmbientOcclusion
import io.github.erkko68.filament.compose.scene.AntiAliasing
import io.github.erkko68.filament.compose.scene.Bloom
import io.github.erkko68.filament.compose.scene.Color
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.PostProcessing
import io.github.erkko68.filament.compose.scene.SkyboxSource
import io.github.erkko68.filament.compose.scene.primitives.Cylinder
import io.github.erkko68.filament.compose.scene.primitives.Sphere
import io.github.erkko68.filament.compose.scene.rememberCameraState
import io.github.erkko68.filament.compose.scene.rememberKTXEnvironment
import io.github.erkko68.filament.compose.scene.rememberMaterial
import io.github.erkko68.filament.compose.scene.rememberSkyboxState
import io.github.erkko68.filament.utils.Float3
import io.github.erkko68.filament.utils.Quaternion
import io.github.erkko68.filament.utils.cross
import io.github.erkko68.filament.utils.dot
import io.github.erkko68.filament.utils.normalize
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculePaneConstants.CPK_ATOM_COLOR_MAP
import org.liganddraw.app.editor.domain.MoleculePaneConstants.FALLBACK_ATOM_COLOR
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt
import io.github.erkko68.filament.compose.scene.Color as FilColor

const val MAX_COLOR_VALUE = 255f

@Composable
fun ColumnScope.Molecule3DViewer(
    conformer: Molecule,
    ibl: ByteArray,
    solidColorMaterial: ByteArray
) {
    val engine = rememberFilamentEngine()

    // Black background
    val skybox = rememberSkyboxState(source = SkyboxSource.Color(FilColor(0f, 0f, 0f)))

    val cameraState = rememberCameraState(eye = Position(0f, 1f, 25f))
    val orbit = rememberOrbitCameraState(cameraState = cameraState, zoomSpeed = 10f)

    val environment = rememberKTXEnvironment(
        engine = engine,
        intensity = 10_000F,
        ibl = { ibl },
    )
    val material = rememberMaterial(engine) { solidColorMaterial }

    val scene = rememberFilamentScene(
        engine = engine,
        skyboxState = skybox,
        indirectLightState = environment.indirectLightState,
    ) {
        material?.let { tmpl ->
            // --- ATOM SPHERES ---
            conformer.atoms.forEach { atom ->
                // Reference: https://en.wikipedia.org/wiki/CPK_coloring
                val atomColor = CPK_ATOM_COLOR_MAP.getOrDefault(atom.symbol, FALLBACK_ATOM_COLOR)
                val red = atomColor.first / MAX_COLOR_VALUE
                val green = atomColor.second / MAX_COLOR_VALUE
                val blue = atomColor.third / MAX_COLOR_VALUE
                Sphere(
                    material = rememberSolidColorInstance(tmpl, FilColor(red, green, blue)),
                    position = Position(atom.x.toFloat(), atom.y.toFloat(), atom.z.toFloat()),
                    // TODO("Radius according to atomic size")
                    radius = .4f
                )
            }

            // --- BOND CYLINDERS ---
            val bondMaterial = rememberSolidColorInstance(tmpl, FilColor(0.20f, 0.55f, 0.95f))
            val hydrogenBondMaterial =
                rememberSolidColorInstance(tmpl, FilColor(0.20f, 0.55f, 0.95f), isDashed = true)
            conformer.bonds.forEach { bond ->
                if (bond is Bond.Ionic) return@forEach

                val beginAtom = conformer.atoms[bond.beginAtomIndex.toInt()]
                val beginAtomXPos = beginAtom.x.toFloat()
                val beginAtomYPos = beginAtom.y.toFloat()
                val beginAtomZPos = beginAtom.z.toFloat()
                val endAtom = conformer.atoms[bond.endAtomIndex.toInt()]
                val endAtomXPos = endAtom.x.toFloat()
                val endAtomYPos = endAtom.y.toFloat()
                val endAtomZPos = endAtom.z.toFloat()

                // Height is the distance between the two atoms determined by Pythagoras theorem
                val height = sqrt(
                    (endAtomXPos - beginAtomXPos).pow(2) + (endAtomYPos - beginAtomYPos).pow(2) + (endAtomZPos - beginAtomZPos).pow(
                        2
                    )
                )

                val initialDirection = normalize(Float3(0f, 1f, 0f))
                val targetDirection = normalize(
                    Float3(
                        endAtomXPos - beginAtomXPos,
                        endAtomYPos - beginAtomYPos,
                        endAtomZPos - beginAtomZPos
                    )
                )

                val rotationAxis = normalize(cross(initialDirection, targetDirection))

                val rotationAngleRadians = acos(dot(initialDirection, targetDirection))
                val rotationAngle = rotationAngleRadians.times(180.div(PI)).toFloat()

                val position = Position(
                    beginAtomXPos,
                    beginAtomYPos,
                    beginAtomZPos
                )

                when (bond) {
                    is Bond.Single ->
                        CylinderBond(
                            bondMaterial, position, height, rotationAxis, rotationAngle
                        )

                    is Bond.Double -> {
                        val spacing = .15f
                        CylinderBond(
                            bondMaterial,
                            position.plus(Direction(rotationAxis.times(spacing))),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                        CylinderBond(
                            bondMaterial,
                            position.minus(Direction(rotationAxis.times(spacing))),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                    }

                    is Bond.Triple -> {
                        val spacing = 0.25f
                        CylinderBond(
                            bondMaterial,
                            position.plus(Direction(rotationAxis.times(spacing))),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                        CylinderBond(
                            bondMaterial, position, height, rotationAxis, rotationAngle
                        )
                        CylinderBond(
                            bondMaterial,
                            position.minus(Direction(rotationAxis.times(spacing))),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                    }

                    is Bond.Hydrogen -> CylinderBond(
                        hydrogenBondMaterial, position, height, rotationAxis, rotationAngle
                    )
                }
            }
        }
    }

    FilamentView(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .weight(1f).fillMaxSize()
            .onSizeChanged { orbit.setViewport(it.width, it.height) }
            .orbitGestures(orbit),
        cameraState = cameraState,
        postProcessing = PostProcessing(
            antiAliasing = AntiAliasing(),
            bloom = Bloom(),
            ambientOcclusion = AmbientOcclusion()
        ),
        scene = scene
    )
}

@Composable
private fun FilamentSceneScope.CylinderBond(
    material: MaterialInstance,
    position: Position,
    height: Float,
    rotationAxis: Float3,
    rotationAngle: Float
) {
    Cylinder(
        material = material,
        position = position,
        height = height,
        radius = 0.1f,
        pivot = Position(0f, -height.div(2), 0f),
        rotation = Quaternion.fromAxisAngle(rotationAxis, rotationAngle)
    )
}

@Composable
private fun rememberSolidColorInstance(
    template: Material,
    color: Color,
    isDashed: Boolean = false
): MaterialInstance {
    val engine = LocalFilamentEngine.current
    val instance = remember(template, color) {
        template.createInstance().also {
            it.setParameter("baseColor", color.r, color.g, color.b)
            if (isDashed)
                it.setParameter("dashed", 1.0f)
        }
    }
    DisposableEffect(instance) {

        onDispose { engine.destroyMaterialInstance(instance) }
    }
    return instance
}
