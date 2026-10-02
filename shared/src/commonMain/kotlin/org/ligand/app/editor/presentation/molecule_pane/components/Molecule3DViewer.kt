package org.ligand.app.editor.presentation.molecule_pane.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.FilamentView
import io.github.erkko68.filament.compose.rememberFilamentScene
import io.github.erkko68.filament.compose.scene.AmbientOcclusion
import io.github.erkko68.filament.compose.scene.AntiAliasing
import io.github.erkko68.filament.compose.scene.Bloom
import io.github.erkko68.filament.compose.scene.CameraState
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.DirectionalLight
import io.github.erkko68.filament.compose.scene.LightIntensity
import io.github.erkko68.filament.compose.scene.LinearColor
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.PostProcessing
import io.github.erkko68.filament.compose.scene.Rotation
import io.github.erkko68.filament.compose.scene.SkyboxSource
import io.github.erkko68.filament.compose.scene.primitives.Cylinder
import io.github.erkko68.filament.compose.scene.primitives.Sphere
import io.github.erkko68.filament.compose.scene.rememberCameraState
import io.github.erkko68.filament.compose.scene.rememberKTXEnvironment
import io.github.erkko68.filament.compose.scene.rememberMaterial
import io.github.erkko68.filament.compose.scene.rememberMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberSkyboxState
import io.github.erkko68.filament.compose.scene.setParameter
import io.github.erkko68.filament.compose.scene.toDirection
import io.github.erkko68.filament.utils.Float3
import io.github.erkko68.filament.utils.Quaternion
import io.github.erkko68.filament.utils.cross
import io.github.erkko68.filament.utils.dot
import io.github.erkko68.filament.utils.length
import io.github.erkko68.filament.utils.normalize
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.ball_and_stick
import ligand.shared.generated.resources.ball_and_stick_epm
import ligand.shared.generated.resources.reset_camera
import ligand.shared.generated.resources.van_der_waals
import org.jetbrains.compose.resources.vectorResource
import org.ligand.app.core.domain.ChemistryError
import org.ligand.app.core.presentation.IconWithTooltip
import org.ligand.app.core.presentation.toErrorText
import org.ligand.app.editor.domain.Bond
import org.ligand.app.editor.domain.EPMMeshData
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

val CAMERA_INITIAL_EYE = Position(0f, 1f, 25f)
val CAMERA_INITIAL_TARGET = Position(0f, 0f, 0f)
val CAMERA_INITIAL_UP = Direction(0f, 1f, 0f)

@Composable
fun BoxScope.Molecule3DViewer(
    engine: Engine,
    epmMeshData: EPMMeshData,
    conformer: Molecule,
    iblBytes: ByteArray,
    solidColorMaterialBytes: ByteArray,
    epmMaterialBytes: ByteArray,
    error: ChemistryError?
) {
    var moleculeRenderMode by remember { mutableStateOf(MoleculeRenderMode.BALL_AND_STICK) }

    val skybox = rememberSkyboxState(initialSource = SkyboxSource.Color(LinearColor(0f, 0f, 0f)))
    val cameraState = rememberCameraState(initialEye = CAMERA_INITIAL_EYE)

    val environment = rememberKTXEnvironment(
        engine = engine,
        initialIntensity = 10_000F,
        ibl = { iblBytes },
    )

    val scene = rememberFilamentScene(
        engine = engine,
        indirectLightState = environment.indirectLightState,
        skyboxState = skybox
    ) {
        // Rear lightening
        DirectionalLight(
            direction = Direction(0f, 0f, 1f),
            intensity = LightIntensity.LuminousPower(25_000F),
        )

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
                    color = LinearColor(red, green, blue),
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
                color = LinearColor(.70f, .80f, 1f),
                metallic = 1f,
                roughness = .6f,
                reflectance = 0f,
            )
        val hydrogenBondMaterial =
            rememberSolidColorInstance(
                template = template,
                color = LinearColor(.70f, .80f, 1f),
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

                val cameraPosition = cameraState.eye
                val viewVector = normalize(
                    Float3(
                        cameraPosition.x - beginAtomXPos,
                        cameraPosition.y - beginAtomYPos,
                        cameraPosition.z - beginAtomZPos
                    )
                )

                val bondOffsetDirection = cross(targetDirection, viewVector)
                when (bond) {
                    is Bond.Single ->
                        CylinderBond(
                            bondMaterial, position, height, rotationAxis, rotationAngle
                        )

                    is Bond.Double -> {
                        val spacing = .15f
                        val offset = Direction(bondOffsetDirection.times(spacing))

                        CylinderBond(
                            bondMaterial,
                            position.plus(offset),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                        CylinderBond(
                            bondMaterial,
                            position.minus(offset),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                    }

                    is Bond.Triple -> {
                        val spacing = 0.25f
                        val offset = Direction(bondOffsetDirection.times(spacing))

                        CylinderBond(
                            bondMaterial,
                            position.plus(offset),
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                        CylinderBond(
                            bondMaterial,
                            position,
                            height,
                            rotationAxis,
                            rotationAngle
                        )
                        CylinderBond(
                            bondMaterial,
                            position.minus(offset),
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
        EPMMoleculeSurface(
            engine = engine,
            meshData = epmMeshData,
            epmMaterialBytes = epmMaterialBytes,
            isVisible = isEPMSurfaceVisible
        )
    }

    if (error == null) {
        FilamentView(
            modifier = Modifier.fillMaxSize().freeOrbitGestures(cameraState),
            cameraState = cameraState,
            postProcessing = PostProcessing(
                antiAliasing = AntiAliasing(),
                bloom = Bloom(),
                ambientOcclusion = AmbientOcclusion()
            ),
            scene = scene,
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

        FilledTonalIconButton(
            modifier = Modifier.padding(8.dp).align(Alignment.BottomStart),
            onClick = {
                cameraState.eye = CAMERA_INITIAL_EYE
                cameraState.target = CAMERA_INITIAL_TARGET
                cameraState.up = CAMERA_INITIAL_UP
            }
        ) {
            IconWithTooltip(
                icon = vectorResource(Res.drawable.reset_camera),
                text = "Reset Camera",
            )
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
        rotation = Rotation(Quaternion.fromAxisAngle(rotationAxis, rotationAngle))
    )
}

@Composable
private fun rememberSolidColorInstance(
    template: Material?,
    color: LinearColor,
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
            textAlign = TextAlign.Center,
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

private fun Modifier.freeOrbitGestures(
    camera: CameraState,
    orbitSpeed: Float = 0.3f,
    zoomSpeed: Float = 2.0f,
    panSpeed: Float = 0.0015f,
    enablePanning: Boolean = true,
): Modifier = this
    // --- DESKTOP ---
    .pointerInput(camera) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val changes = event.changes

                when (event.type) {
                    // MOUSE WHEEL / TRACKPAD SCROLL ZOOM
                    PointerEventType.Scroll -> {
                        val scrollDelta = changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollDelta != 0f) {
                            val offset = camera.eye - camera.target
                            var currentDistance = length(offset.toFloat3())

                            if (currentDistance > 0.0001f) {
                                val zoomFactor = if (scrollDelta < 0) 1.1f else 0.9f
                                currentDistance = (currentDistance / zoomFactor)

                                camera.eye = camera.target + offset.normalized() * currentDistance
                            }
                            changes.forEach { it.consume() }
                        }
                    }

                    // MOUSE DRAG
                    PointerEventType.Move -> {
                        if (changes.size == 1) {
                            val change = changes.first()
                            if (change.pressed) {
                                val panDelta = change.position - change.previousPosition
                                if (panDelta != Offset.Zero) {
                                    val isPanTriggered =
                                        event.buttons.isSecondaryPressed || // Right Click
                                                event.buttons.isTertiaryPressed ||  // Middle Click
                                                (event.buttons.isPrimaryPressed && event.keyboardModifiers.isShiftPressed) // Shift + Left Click

                                    var offset = camera.eye - camera.target
                                    val currentDistance = length(offset.toFloat3())

                                    if (currentDistance > 0.0001f) {
                                        if (enablePanning && isPanTriggered) {
                                            // DESKTOP PAN
                                            val forward = (-offset).normalized()
                                            val right = cross(
                                                forward.toFloat3(),
                                                camera.up.toFloat3()
                                            ).toDirection()
                                            val cameraUp = cross(
                                                right.toFloat3(),
                                                forward.toFloat3()
                                            ).toDirection()

                                            val panScale = currentDistance * panSpeed
                                            val panVector =
                                                (right * (-panDelta.x * panScale)) + (cameraUp * (panDelta.y * panScale))

                                            camera.target += panVector
                                            camera.eye += panVector
                                            change.consume()
                                        } else if (event.buttons.isPrimaryPressed && !event.keyboardModifiers.isShiftPressed) {
                                            // DESKTOP ORBIT
                                            if (panDelta.x != 0f) {
                                                offset = Rotation.axisAngle(
                                                    camera.up,
                                                    -panDelta.x * orbitSpeed
                                                ) * offset
                                            }
                                            if (panDelta.y != 0f) {
                                                val right =
                                                    cross(camera.up.toFloat3(), offset.toFloat3())
                                                val rightLength = length(right)
                                                if (rightLength > 0.0001f) {
                                                    val pitch = Rotation.axisAngle(
                                                        (right / rightLength).toDirection(),
                                                        -panDelta.y * orbitSpeed
                                                    )
                                                    offset = pitch * offset
                                                    camera.up = (pitch * camera.up).normalized()
                                                }
                                            }
                                            camera.eye =
                                                camera.target + offset.normalized() * currentDistance
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // --- TOUCH GESTURES ---
    .pointerInput(camera) {
        detectTransformGestures { _, pan, zoom, _ ->
            var offset = camera.eye - camera.target
            var currentDistance = length(offset.toFloat3())

            if (currentDistance <= 0.0001f) return@detectTransformGestures

            if (zoom != 1.0f && zoom > 0f) {
                val adjustedZoom = 1.0f + (zoom - 1.0f) * zoomSpeed
                if (adjustedZoom > 0f) {
                    currentDistance =
                        (currentDistance / adjustedZoom)
                }
            }

            val isMultiTouch = zoom != 1.0f

            if (enablePanning && isMultiTouch) {
                // Two-finger Touch Pan
                val forward = (-offset).normalized()
                val right = cross(forward.toFloat3(), camera.up.toFloat3()).toDirection()
                val cameraUp = cross(right.toFloat3(), forward.toFloat3()).toDirection()

                val panScale = currentDistance * panSpeed
                val panVector = (right * (-pan.x * panScale)) + (cameraUp * (pan.y * panScale))

                camera.target += panVector
                camera.eye += panVector
            } else if (!isMultiTouch) {
                // One-finger Touch Orbit
                if (pan.x != 0f) {
                    offset = Rotation.axisAngle(camera.up, -pan.x * orbitSpeed) * offset
                }
                if (pan.y != 0f) {
                    val right = cross(camera.up.toFloat3(), offset.toFloat3())
                    val rightLength = length(right)
                    if (rightLength > 0.0001f) {
                        val pitch = Rotation.axisAngle(
                            (right / rightLength).toDirection(),
                            -pan.y * orbitSpeed
                        )
                        offset = pitch * offset
                        camera.up = (pitch * camera.up).normalized()
                    }
                }
            }

            camera.eye = camera.target + offset.normalized() * currentDistance
        }
    }