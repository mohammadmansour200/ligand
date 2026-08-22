package org.ligand.app.editor.presentation.molecule_pane.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.FilamentView
import io.github.erkko68.filament.compose.orbitGestures
import io.github.erkko68.filament.compose.rememberFilamentEngine
import io.github.erkko68.filament.compose.rememberFilamentScene
import io.github.erkko68.filament.compose.rememberOrbitCameraController
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
import io.github.erkko68.filament.compose.scene.rememberMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberSkyboxState
import io.github.erkko68.filament.compose.scene.setParameter
import io.github.erkko68.filament.utils.Float3
import io.github.erkko68.filament.utils.Quaternion
import io.github.erkko68.filament.utils.cross
import io.github.erkko68.filament.utils.dot
import io.github.erkko68.filament.utils.normalize
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.ball_and_stick
import ligand.shared.generated.resources.ball_and_stick_epm
import ligand.shared.generated.resources.van_der_waals
import org.jetbrains.compose.resources.vectorResource
import org.ligand.app.core.domain.ChemistryError
import org.ligand.app.core.presentation.IconWithTooltip
import org.ligand.app.core.presentation.toErrorText
import org.ligand.app.editor.domain.Bond
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.MoleculePaneConstants.ATOM_VAN_DER_WAALS_RADII_MAP
import org.ligand.app.editor.domain.MoleculePaneConstants.BALL_STICK_RADII_SCALE
import org.ligand.app.editor.domain.MoleculePaneConstants.CPK_ATOM_COLOR_MAP
import org.ligand.app.editor.domain.MoleculePaneConstants.FALLBACK_ATOM_COLOR
import org.ligand.app.editor.domain.MoleculePaneConstants.FALLBACK_ATOM_VAN_DER_WAALS_RADII
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt
import io.github.erkko68.filament.compose.scene.Color as FilColor

const val MAX_COLOR_VALUE = 255f

enum class MoleculeRenderMode {
    BALL_AND_STICK,
    SPACE_FILLING,
    ELECTRON_DISTRIBUTION
}

val moleculeRenderModeOptions = listOf(
    Triple(
        Res.drawable.ball_and_stick,
        "Ball and Stick",
        MoleculeRenderMode.BALL_AND_STICK
    ),
    Triple(
        Res.drawable.van_der_waals,
        "Space filling",
        MoleculeRenderMode.SPACE_FILLING
    ),
    Triple(
        Res.drawable.ball_and_stick_epm,
        "Electron Distribution",
        MoleculeRenderMode.ELECTRON_DISTRIBUTION
    )
)

@Composable
fun BoxScope.Molecule3DViewer(
    conformer: Molecule,
    iblBytes: ByteArray,
    solidColorMaterialBytes: ByteArray,
    epmMaterialBytes: ByteArray,
    error: ChemistryError?
) {
    var moleculeRenderMode by remember { mutableStateOf(MoleculeRenderMode.BALL_AND_STICK) }

    val engine = rememberFilamentEngine()

    // Black background
    val skybox = rememberSkyboxState(initialSource = SkyboxSource.Color(FilColor(0f, 0f, 0f)))

    val cameraState = rememberCameraState(initialEye = Position(0f, 1f, 25f))
    val orbit = rememberOrbitCameraController(cameraState = cameraState, zoomSpeed = 10f)

    val environment = rememberKTXEnvironment(
        engine = engine,
        initialIntensity = 10_000F,
        ibl = { iblBytes },
    )

    val scene = rememberFilamentScene(
        engine = engine,
        skyboxState = skybox,
        indirectLightState = environment.indirectLightState,
    ) {
        val template = rememberMaterial { solidColorMaterialBytes }

        // --- ATOM SPHERES ---
        conformer.atoms.forEach { atom ->
            // Reference: https://en.wikipedia.org/wiki/CPK_coloring
            val atomColor = CPK_ATOM_COLOR_MAP.getOrDefault(atom.symbol, FALLBACK_ATOM_COLOR)
            val red = atomColor.first / MAX_COLOR_VALUE
            val green = atomColor.second / MAX_COLOR_VALUE
            val blue = atomColor.third / MAX_COLOR_VALUE

            val spaceFillingRadii =
                ATOM_VAN_DER_WAALS_RADII_MAP.getOrDefault(
                    atom.symbol,
                    FALLBACK_ATOM_VAN_DER_WAALS_RADII
                )

            val ballAndStickRadii = spaceFillingRadii * BALL_STICK_RADII_SCALE
            Sphere(
                material = rememberSolidColorInstance(
                    template = template,
                    color = FilColor(red, green, blue),
                    metallic = .2f,
                    roughness = .55f,
                    reflectance = .5f
                ),
                position = Position(atom.x.toFloat(), atom.y.toFloat(), atom.z.toFloat()),
                radius = if (moleculeRenderMode == MoleculeRenderMode.SPACE_FILLING) spaceFillingRadii else ballAndStickRadii
            )
        }

        // --- BOND CYLINDERS ---
        val bondMaterial =
            rememberSolidColorInstance(
                template = template,
                color = FilColor(.70f, .80f, 1f),
                metallic = 1f,
                roughness = .6f,
                reflectance = 0f,
            )
        val hydrogenBondMaterial =
            rememberSolidColorInstance(
                template = template,
                color = FilColor(.70f, .80f, 1f),
                metallic = 1f,
                roughness = .6f,
                reflectance = 0f,
                isDashed = true
            )
        if (moleculeRenderMode != MoleculeRenderMode.SPACE_FILLING) {
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

        val isEPMSurfaceVisible = moleculeRenderMode == MoleculeRenderMode.ELECTRON_DISTRIBUTION
        EPMMoleculeSurface(conformer.atoms, epmMaterialBytes, isEPMSurfaceVisible)
    }

    if (error == null) {
        FilamentView(
            modifier = Modifier.fillMaxSize().orbitGestures(orbit),
            cameraState = cameraState,
            postProcessing = PostProcessing(
                antiAliasing = AntiAliasing(),
                bloom = Bloom(),
                ambientOcclusion = AmbientOcclusion()
            ),
            scene = scene
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)) {
            moleculeRenderModeOptions.forEachIndexed { index, (icon, label, mode) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = moleculeRenderModeOptions.size
                    ),
                    onClick = { moleculeRenderMode = mode },
                    selected = moleculeRenderMode == mode,
                    label = {
                        IconWithTooltip(
                            icon = vectorResource(icon),
                            text = label,
                        )
                    }
                )
            }
        }
    } else Molecule3dViewerErrorNotice(
        error = error,
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun FilamentSceneScope.CylinderBond(
    material: MaterialInstance?,
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
    template: Material?,
    color: Color,
    metallic: Float = 0f,
    roughness: Float = 0.5f,
    reflectance: Float = 0.5f,
    isDashed: Boolean = false
): MaterialInstance? {

    return rememberMaterialInstance(
        template, color
    ) {
        setParameter("baseColor", color)
        if (isDashed)
            setParameter("dashed", 1.0f)

        setParameter("metallic", metallic)
        setParameter("roughness", roughness)
        setParameter("reflectance", reflectance)
    }
}

@Composable
private fun Molecule3dViewerErrorNotice(modifier: Modifier = Modifier, error: ChemistryError) {
    val errorText = error.toErrorText()
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorText.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = errorText.body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        errorText.hint?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
