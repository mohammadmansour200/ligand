package org.liganddraw.app.editor.data.cheminformatics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.RDKit.Atom
import org.RDKit.Bond
import org.RDKit.Bond.BondType
import org.RDKit.Conformer
import org.RDKit.ConformerException
import org.RDKit.DistanceGeom
import org.RDKit.ForceField
import org.RDKit.Int_Pair
import org.RDKit.Match_Vect
import org.RDKit.PeriodicTable
import org.RDKit.Point3D
import org.RDKit.RDKFuncs
import org.RDKit.ROMol
import org.RDKit.RWMol
import org.RDKit.SDMolSupplier
import org.RDKit.Transform3D
import org.liganddraw.app.core.domain.ChemistryError
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result
import org.liganddraw.app.editor.data.mappers.toMolecule
import org.liganddraw.app.editor.data.mappers.toRWMol
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BOND_LENGTH
import org.liganddraw.app.editor.domain.DrawingPaneConstants.MAX_FORMAL_CHARGE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.MIN_FORMAL_CHARGE
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculeProperties
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RDKitCheminformaticsDataSource : CheminformaticsDataSource {
    override suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError> =
        withContext(
            Dispatchers.Default
        ) {
            // --- READ .mol FILE ---
            val mol =
                RWMol.MolFromMolFile(absolutePath, true) // Sanitize is set to true
            try {
                // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
                val molecule = mol.toMolecule()
                return@withContext Result.Success(listOf(molecule))
            } catch (e: Exception) {
                println(e.message)
                return@withContext Result.Error(DataError.FileCorrupted)
            } finally {
                // --- CLEANUP ---
                mol.delete()
            }
        }

    override suspend fun sdfFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError> =
        withContext(Dispatchers.Default) {
            val supplier = SDMolSupplier(absolutePath, true) // Sanitize is set to true
            try {
                val molecules = mutableListOf<Molecule>()
                // --- ITERATE MOLECULES FOUND IN SDF FILE ---
                while (!supplier.atEnd()) {
                    var roMol: ROMol? = null
                    var rwMol: RWMol? = null
                    try {
                        roMol = supplier.next() ?: continue
                        rwMol = RWMol(roMol)
                        // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
                        molecules.add(rwMol.toMolecule())
                    } catch (_: Exception) {
                    } finally {
                        rwMol?.delete()
                        roMol?.delete()
                    }
                }
                return@withContext Result.Success(molecules)
            } catch (e: Exception) {
                println(e.message)
                return@withContext Result.Error(DataError.FileCorrupted)
            } finally {
                // --- CLEANUP ---
                supplier.delete()
            }
        }

    override suspend fun generate3DConformer(molecule: Molecule): Result<Molecule, ChemistryError> =
        withContext(Dispatchers.Default) {
            // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
            val rdkitMol: ROMol = molecule.toRWMol(sanitize = true)
            val hydrogenatedRdkitROMol = RDKFuncs.addHs(rdkitMol)
            val embedParams = RDKFuncs.getETKDGv3()

            var matchVect: Match_Vect? = null
            var centroid: Point3D? = null
            var transform: Transform3D? = null
            var point3D: Point3D? = null
            var hydrogenatedRdkitRWMol: RWMol? = null

            try {
                val conformerId = DistanceGeom.EmbedMolecule(hydrogenatedRdkitROMol, embedParams)
                ForceField.MMFFOptimizeMolecule(
                    hydrogenatedRdkitROMol,
                    "MMFF94",
                    1000,
                    10.0,
                    conformerId,
                    false
                )
                hydrogenatedRdkitROMol.computeGasteigerCharges()

                try {
                    matchVect = Match_Vect()
                    val numHeavyAtoms = rdkitMol.numHeavyAtoms
                    for (i in 0 until numHeavyAtoms) {
                        matchVect.add(Int_Pair(i.toInt(), i.toInt()))
                    }
                    hydrogenatedRdkitROMol.alignMol(rdkitMol, conformerId, 0, matchVect)

                    val conf3D = hydrogenatedRdkitROMol.getConformer(conformerId)
                    centroid = conf3D.computeCentroid()
                    transform = Transform3D()
                    point3D = Point3D(-centroid.x, -centroid.y, -centroid.z)
                    transform.SetTranslation(point3D)
                    conf3D.transformConformer(transform)
                } catch (_: Exception) {
                }

                // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
                hydrogenatedRdkitRWMol = RWMol(hydrogenatedRdkitROMol)
                val resultMolecule = hydrogenatedRdkitRWMol.toMolecule(false)
                return@withContext Result.Success(resultMolecule)
            } catch (_: ConformerException) {
                return@withContext Result.Error(
                    ChemistryError.NoConformation
                )
            } catch (e: Exception) {
                println(e)
                return@withContext Result.Error(
                    ChemistryError.SanitizationFailed
                )
            } finally {
                matchVect?.delete()
                transform?.delete()
                centroid?.delete()
                point3D?.delete()
                hydrogenatedRdkitRWMol?.delete()
                hydrogenatedRdkitROMol.delete()
                rdkitMol.delete()
                embedParams.delete()
            }
        }

    override suspend fun calcProperties(molecule: Molecule): Result<MoleculeProperties, ChemistryError> =
        withContext(Dispatchers.Default) {
            // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
            val rdkitMol = molecule.toRWMol(sanitize = true)

            try {
                // --- CALCULATE PROPERTIES ---
                val formula = RDKFuncs.calcMolFormula(rdkitMol)
                val logp = RDKFuncs.calcMolLogP(rdkitMol)
                val hba = RDKFuncs.calcNumHBA(rdkitMol)
                val hbd = RDKFuncs.calcNumHBD(rdkitMol)
                val mwt = RDKFuncs.calcAMW(rdkitMol)
                val rotatable = RDKFuncs.calcNumRotatableBonds(rdkitMol)

                return@withContext Result.Success(
                    MoleculeProperties(
                        iupacName = null,
                        formula = formula,
                        logp = logp,
                        molecularWeight = mwt,
                        hydrogenBondAcceptors = hba,
                        hydrogenBondDonors = hbd,
                        rotatableBonds = rotatable
                    )
                )
            } catch (_: Exception) {
                return@withContext Result.Error(
                    ChemistryError.SanitizationFailed
                )
            } finally {
                // --- CLEANUP ---
                rdkitMol.delete()
            }
        }

    override suspend fun attachBondToAtom(
        molecule: Molecule,
        targetAtomIdx: Long,
        type: BondType,
        dir: Bond.BondDir
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
        val rdkitRWMol = molecule.toRWMol()
        val template = ROMol(rdkitRWMol)
        try {
            val targetAtom = rdkitRWMol.getAtomWithIdx(targetAtomIdx)
            val targetAtomIsTerminal = targetAtom.bonds.size() == 1L

            val targetAtomNeighbors = rdkitRWMol.getAtomNeighbors(targetAtom)

            // --- ADD END ATOM IN RDKIT MOLECULE ---
            val endAtomIdx = rdkitRWMol.addAtom(Atom("C"))

            // --- ADD BOND IN RDKIT MOLECULE ---
            rdkitRWMol.addBond(targetAtomIdx, endAtomIdx, type)
            rdkitRWMol.getBondBetweenAtoms(targetAtomIdx, endAtomIdx)?.bondDir = dir

            val conformer = rdkitRWMol.conformer
            val targetAtomPosition = conformer.getAtomPos(targetAtomIdx)
            if (targetAtomIsTerminal) {
                val parentAtomIdx = targetAtomNeighbors[0].idx
                val parentAtom = rdkitRWMol.getAtomWithIdx(parentAtomIdx)
                val parentAtomNeighbors = rdkitRWMol.getAtomNeighbors(parentAtom)

                val parentAtomPosition = conformer.getAtomPos(parentAtomIdx)
                val existingDir = parentAtomPosition.directionVector(targetAtomPosition)

                val zigzagAngleOffset = Math.toRadians(60.0)

                val grandParentIdx = parentAtomNeighbors[0]?.idx
                val rotationSign = if (grandParentIdx != null) {
                    val grandPos = conformer.getAtomPos(grandParentIdx)
                    val prevDir = grandPos.directionVector(parentAtomPosition)
                    val cross = prevDir.crossProduct(existingDir)
                    if (cross.z >= 0) -1.0 else 1.0
                } else {
                    val baseAngle = atan2(existingDir.y, existingDir.x)
                    val angleUp = baseAngle + zigzagAngleOffset
                    val angleDown = baseAngle - zigzagAngleOffset

                    if (sin(angleUp) >= sin(angleDown)) 1.0 else -1.0
                }

                val newAngle =
                    atan2(existingDir.y, existingDir.x) + rotationSign * zigzagAngleOffset
                val newPos = Point3D(
                    targetAtomPosition.x + BOND_LENGTH * cos(newAngle),
                    targetAtomPosition.y + BOND_LENGTH * sin(newAngle),
                    0.0
                )

                conformer.setAtomPos(endAtomIdx, newPos)
            } else {
                rdkitRWMol.generateDepictionMatching2DStructure(template)
            }

            try {
                rdkitRWMol.sanitizeMol()
            } catch (_: Exception) {
            }

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            val molecule = rdkitRWMol.toMolecule()
            return@withContext molecule
        } finally {
            // --- CLEANUP ---
            template.delete()
            rdkitRWMol.delete()
        }
    }

    override suspend fun replaceAtomWithAtom(
        molecule: Molecule,
        targetAtomIdx: Long,
        newAtomSymbol: String,
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
        val rdkitRWMol = molecule.toRWMol()

        val periodicTable = PeriodicTable.getTable()
        try {
            val newAtomAtomicNumber = periodicTable.getAtomicNumber(newAtomSymbol)

            // --- EDIT ATOM IN RDKit MOLECULE ---
            rdkitRWMol.getAtomWithIdx(targetAtomIdx).atomicNum = newAtomAtomicNumber

            try {
                rdkitRWMol.sanitizeMol()
            } catch (_: Exception) {
            }

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            val molecule = rdkitRWMol.toMolecule()
            return@withContext molecule
        } finally {
            // --- CLEANUP ---
            rdkitRWMol.delete()
            periodicTable.delete()
        }
    }

    override suspend fun replaceAtomWithTemplate(
        molecule: Molecule,
        targetAtomIdx: Long,
        templateSmiles: String
    ): Molecule = withContext(Dispatchers.Default) {
        val templateMol = RWMol.MolFromSmiles(templateSmiles)
        val rdkitRWMol = molecule.toRWMol()
        val depictionTemplateMol = RWMol(rdkitRWMol)

        try {
            val targetAtom = rdkitRWMol.getAtomWithIdx(targetAtomIdx)

            // --- REPLACE ATOM WITH TEMPLATE: atom has no bonds ---
            if (targetAtom.bonds.size() == 0L) {
                val targetAtomPos = rdkitRWMol.conformer.getAtomPos(targetAtomIdx)

                val molecule = createMoleculeFromSmiles(
                    smiles = templateSmiles,
                    x = targetAtomPos.x,
                    y = targetAtomPos.y
                )
                return@withContext molecule
            }

            // --- INSERT TEMPLATE MOLECULE INTO MAIN MOLECULE ---
            val templateAnchorIdx = rdkitRWMol.numAtoms
            rdkitRWMol.insertMol(templateMol)

            // --- BOND TEMPLATE MOLECULE TO ATOM: atom has multiple existing bonds ---
            if (targetAtom.bonds.size() > 1) {
                rdkitRWMol.addBond(targetAtomIdx, templateAnchorIdx, BondType.SINGLE)
            } else {
                // --- REPLACE ATOM WITH TEMPLATE MOLECULE: terminal atom ---
                val neighborBond = targetAtom.bonds[0]
                val neighborBondOtherAtomIdx = neighborBond.getOtherAtom(targetAtom).idx

                rdkitRWMol.addBond(
                    neighborBondOtherAtomIdx,
                    templateAnchorIdx,
                    neighborBond.bondType
                )
                rdkitRWMol.getBondBetweenAtoms(
                    neighborBondOtherAtomIdx,
                    templateAnchorIdx
                )?.bondDir =
                    neighborBond.bondDir

                rdkitRWMol.removeAtom(targetAtomIdx)
                depictionTemplateMol.removeAtom(targetAtomIdx)
            }

            rdkitRWMol.generateDepictionMatching2DStructure(depictionTemplateMol)
            try {
                rdkitRWMol.sanitizeMol()
            } catch (_: Exception) {
            }

            return@withContext rdkitRWMol.toMolecule()
        } finally {
            templateMol.delete()
            rdkitRWMol.delete()
            depictionTemplateMol.delete()
        }
    }

    override suspend fun fuseTemplateToBond(
        molecule: Molecule,
        targetBondIdx: Long,
        templateSmiles: String
    ): Molecule = withContext(Dispatchers.Default) {
        val templateMol = RWMol.MolFromSmiles(templateSmiles)
        val rdkitRWMol = molecule.toRWMol()
        val depictionTemplateMol = ROMol(rdkitRWMol)

        try {
            // --- TARGET BOND ATOMS ---
            val targetBond = rdkitRWMol.getBondWithIdx(targetBondIdx)
            val targetBeginIdx = targetBond.beginAtomIdx
            val targetEndIdx = targetBond.endAtomIdx

            // --- TEMPLATE BOND ATOMS ---
            val templateBond = templateMol.getBondWithIdx(0L)
            val templateBeginIdx = templateBond.beginAtomIdx
            val templateEndIdx = templateBond.endAtomIdx

            // --- FIND TEMPLATE ATTACHMENT POINTS AND BOND TYPE ---
            var templateBeginNeighborIdx = -1L
            var templateBeginBondType = BondType.SINGLE
            val beginBonds = templateMol.getAtomWithIdx(templateBeginIdx).bonds
            for (i in 0 until beginBonds.size()) {
                val bond = beginBonds[i.toInt()]
                val otherIdx =
                    if (bond.beginAtomIdx == templateBeginIdx) bond.endAtomIdx else bond.beginAtomIdx
                if (otherIdx != templateEndIdx) {
                    templateBeginNeighborIdx = otherIdx
                    templateBeginBondType = bond.bondType
                    break
                }
            }

            var templateEndNeighborIdx = -1L
            var templateEndBondType = BondType.SINGLE
            val endBonds = templateMol.getAtomWithIdx(templateEndIdx).bonds
            for (i in 0 until endBonds.size()) {
                val bond = endBonds[i.toInt()]
                val otherIdx =
                    if (bond.beginAtomIdx == templateEndIdx) bond.endAtomIdx else bond.beginAtomIdx
                if (otherIdx != templateBeginIdx) {
                    templateEndNeighborIdx = otherIdx
                    templateEndBondType = bond.bondType
                    break
                }
            }

            // --- TRUNCATE TEMPLATE ---
            val atomsToRemove = listOf(templateBeginIdx, templateEndIdx).sortedDescending()
            var finalTemplateBeginNeighbor = templateBeginNeighborIdx
            var finalTemplateEndNeighbor = templateEndNeighborIdx

            for (idx in atomsToRemove) {
                if (finalTemplateBeginNeighbor > idx) finalTemplateBeginNeighbor--
                if (finalTemplateEndNeighbor > idx) finalTemplateEndNeighbor--
                templateMol.removeAtom(idx)
            }

            // --- INSERT TEMPLATE ---
            val oldNumAtoms = rdkitRWMol.numAtoms
            rdkitRWMol.insertMol(templateMol)

            // --- FUSE TEMPLATE TO MAIN MOLECULE ---
            val combinedTemplateBeginNeighbor = oldNumAtoms + finalTemplateBeginNeighbor
            rdkitRWMol.addBond(targetBeginIdx, combinedTemplateBeginNeighbor, templateBeginBondType)
            val combinedTemplateEndNeighbor = oldNumAtoms + finalTemplateEndNeighbor
            rdkitRWMol.addBond(targetEndIdx, combinedTemplateEndNeighbor, templateEndBondType)

            rdkitRWMol.generateDepictionMatching2DStructure(depictionTemplateMol)
            try {
                rdkitRWMol.sanitizeMol()
            } catch (_: Exception) {
            }

            return@withContext rdkitRWMol.toMolecule()
        } finally {
            templateMol.delete()
            rdkitRWMol.delete()
            depictionTemplateMol.delete()
        }
    }

    override suspend fun eraseBond(
        molecule: Molecule,
        targetBondIdx: Long,
    ): List<Molecule> = withContext(Dispatchers.Default) {
        val mol = molecule.toRWMol()
        try {
            mol.Kekulize(true, false)

            val bond = mol.getBondWithIdx(targetBondIdx)
            mol.removeBond(bond.beginAtomIdx, bond.endAtomIdx)

            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }

            return@withContext mol.splitIntoFragmentMolecules()
        } finally {
            mol.delete()
        }
    }

    override suspend fun eraseAtom(
        molecule: Molecule,
        targetAtomIdx: Long,
    ): List<Molecule> = withContext(Dispatchers.Default) {
        val mol = molecule.toRWMol()
        try {
            mol.Kekulize(true, false)

            mol.removeAtom(targetAtomIdx)

            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }

            return@withContext mol.splitIntoFragmentMolecules()
        } finally {
            mol.delete()
        }
    }

    private fun RWMol.splitIntoFragmentMolecules(): List<Molecule> {
        val fragments = RDKFuncs.getMolFrags(this, false)
        val molecules = mutableListOf<Molecule>()
        for (i in 0 until fragments.size()) {
            val fragment = fragments[i.toInt()]
            val writableFragment = RWMol(fragment)
            try {
                molecules.add(writableFragment.toMolecule())
            } finally {
                writableFragment.delete()
                fragment.delete()
            }
        }
        return molecules
    }

    override suspend fun createMoleculeFromSmiles(
        smiles: String,
        x: Double,
        y: Double
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT SMILES INTO RDKIT MOLECULE ---
        val mol = RWMol.MolFromSmiles(smiles)

        val transform = Transform3D()
        var point3D: Point3D? = null
        try {
            // --- COMPUTE COORDINATES ---
            mol.compute2DCoords()

            val conformer = mol.conformer

            // --- PLACE MOLECULE AT CENTER OF X, Y coordinates ---
            // offsets to place molecule center at target (x, y)
            val centroid = conformer.computeCentroid()
            val deltaX = x - centroid.x
            val deltaY = y - centroid.y

            point3D = Point3D(deltaX, deltaY, 0.0)
            transform.SetTranslation(point3D)
            conformer.transformConformer(transform)

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            return@withContext mol.toMolecule()
        } finally {
            // --- CLEANUP ---
            transform.delete()
            point3D?.delete()
            mol.delete()
        }
    }

    override suspend fun createMoleculeFromAtom(
        symbol: String,
        x: Double,
        y: Double
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT SMILES INTO RDKIT MOLECULE ---
        val mol = RWMol()

        try {
            val conformer = Conformer()

            // --- ADD ATOM ---
            val atomIdx = mol.addAtom(Atom(symbol))
            conformer.setAtomPos(atomIdx, Point3D(x, y, 0.0))
            conformer.is3D = false
            mol.addConformer(
                conformer, true
            )

            mol.sanitizeMol()

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            return@withContext mol.toMolecule()
        } finally {
            // --- CLEANUP ---
            mol.delete()
        }
    }

    override suspend fun cycleBondType(
        molecule: Molecule,
        targetBondIdx: Long
    ): Molecule = withContext(Dispatchers.Default) {
        val mol = molecule.toRWMol()
        try {
            val bond = mol.getBondWithIdx(targetBondIdx)
            val nextType = when (bond.bondType) {
                BondType.SINGLE -> BondType.DOUBLE
                BondType.DOUBLE -> BondType.TRIPLE
                else -> BondType.SINGLE
            }
            bond.bondType = nextType

            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }

            val molecule = mol.toMolecule()
            return@withContext molecule
        } finally {
            mol.delete()
        }
    }

    override suspend fun setBondType(
        molecule: Molecule,
        targetBondIdx: Long,
        type: BondType,
        dir: Bond.BondDir
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
        val mol = molecule.toRWMol()

        try {
            val bond = mol.getBondWithIdx(targetBondIdx)

            // --- SET NEW BOND TYPE AND DIRECTION ---
            bond.bondType = type
            mol.getBondBetweenAtoms(bond.beginAtomIdx, bond.endAtomIdx)?.bondDir = dir

            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            val molecule = mol.toMolecule()
            return@withContext molecule
        } finally {
            // --- CLEANUP ---
            mol.delete()
        }
    }

    override suspend fun attachAtomToAtomAtAngle(
        molecule: Molecule,
        targetAtomIdx: Long,
        newAtomSymbol: String,
        type: BondType,
        dir: Bond.BondDir,
        angleRadians: Double
    ): Molecule = withContext(Dispatchers.Default) {
        val rdkitRWMol = molecule.toRWMol()
        try {
            val endAtomIdx = rdkitRWMol.addAtom(Atom(newAtomSymbol))
            rdkitRWMol.addBond(targetAtomIdx, endAtomIdx, type)
            rdkitRWMol.getBondBetweenAtoms(targetAtomIdx, endAtomIdx)?.bondDir = dir

            val conformer = rdkitRWMol.conformer
            val targetAtomPosition = conformer.getAtomPos(targetAtomIdx)
            val newPos = Point3D(
                targetAtomPosition.x + BOND_LENGTH * cos(angleRadians),
                targetAtomPosition.y + BOND_LENGTH * sin(angleRadians),
                0.0
            )
            conformer.setAtomPos(endAtomIdx, newPos)

            try {
                rdkitRWMol.sanitizeMol()
            } catch (_: Exception) {
            }
            return@withContext rdkitRWMol.toMolecule()
        } finally {
            rdkitRWMol.delete()
        }
    }

    override suspend fun bondSameMoleculeAtoms(
        molecule: Molecule,
        atomIdxA: Long,
        atomIdxB: Long,
        type: BondType,
        dir: Bond.BondDir
    ): Molecule = withContext(Dispatchers.Default) {
        val mol = molecule.toRWMol()
        try {
            if (mol.getBondBetweenAtoms(atomIdxA, atomIdxB) == null) {
                mol.addBond(atomIdxA, atomIdxB, type)
                mol.getBondBetweenAtoms(atomIdxA, atomIdxB)?.bondDir = dir
            }
            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }
            return@withContext mol.toMolecule()
        } finally {
            mol.delete()
        }
    }

    override suspend fun bondDifferentMoleculesAtoms(
        moleculeA: Molecule,
        atomIdxA: Long,
        moleculeB: Molecule,
        atomIdxB: Long,
        type: BondType,
        dir: Bond.BondDir
    ): Molecule = withContext(Dispatchers.Default) {
        val rwMolA = moleculeA.toRWMol()
        val rwMolB = moleculeB.toRWMol()
        try {
            val offset = rwMolA.numAtoms
            rwMolA.insertMol(rwMolB)
            rwMolA.addBond(atomIdxA, offset + atomIdxB, type)
            rwMolA.getBondBetweenAtoms(atomIdxA, offset + atomIdxB)?.bondDir = dir
            try {
                rwMolA.sanitizeMol()
            } catch (_: Exception) {
            }
            return@withContext rwMolA.toMolecule()
        } finally {
            rwMolA.delete()
            rwMolB.delete()
        }
    }

    override suspend fun buildChainFromAtom(
        molecule: Molecule,
        pivotAtomIdx: Long,
        atomCount: Int,
        angleRadians: Double
    ): Molecule = withContext(Dispatchers.Default) {
        val mol = molecule.toRWMol()
        try {
            val conformer = mol.conformer
            var previousAtomIdx = pivotAtomIdx
            var previousPos = conformer.getAtomPos(pivotAtomIdx)
            var sign = 1.0
            val halfAngle = Math.toRadians(30.0)

            repeat(atomCount) {
                val stepAngle = angleRadians + sign * halfAngle
                val newAtomIdx = mol.addAtom(Atom("C"))
                mol.addBond(previousAtomIdx, newAtomIdx, BondType.SINGLE)

                val newPos = Point3D(
                    previousPos.x + BOND_LENGTH * cos(stepAngle),
                    previousPos.y + BOND_LENGTH * sin(stepAngle),
                    0.0
                )
                conformer.setAtomPos(newAtomIdx, newPos)

                previousAtomIdx = newAtomIdx
                previousPos = newPos
                sign = -sign
            }

            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }
            return@withContext mol.toMolecule()
        } finally {
            mol.delete()
        }
    }

    override suspend fun buildChainFromPoint(
        x: Double,
        y: Double,
        atomCount: Int,
        angleRadians: Double
    ): Molecule = withContext(Dispatchers.Default) {
        val mol = RWMol()
        try {
            val conformer = Conformer()
            val firstAtomIdx = mol.addAtom(Atom("C"))
            var previousPos = Point3D(x, y, 0.0)
            conformer.setAtomPos(firstAtomIdx, previousPos)

            var previousAtomIdx = firstAtomIdx
            var sign = 1.0
            val halfAngle = Math.toRadians(30.0)

            repeat(atomCount - 1) {
                val stepAngle = angleRadians + sign * halfAngle
                val newAtomIdx = mol.addAtom(Atom("C"))
                mol.addBond(previousAtomIdx, newAtomIdx, BondType.SINGLE)

                val newPos = Point3D(
                    previousPos.x + BOND_LENGTH * cos(stepAngle),
                    previousPos.y + BOND_LENGTH * sin(stepAngle),
                    0.0
                )
                conformer.setAtomPos(newAtomIdx, newPos)

                previousAtomIdx = newAtomIdx
                previousPos = newPos
                sign = -sign
            }

            conformer.is3D = false
            mol.addConformer(conformer, true)
            mol.sanitizeMol()

            return@withContext mol.toMolecule()
        } finally {
            mol.delete()
        }
    }

    override suspend fun changeFormalCharge(
        molecule: Molecule,
        targetAtomIdx: Long,
        delta: Int
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
        val mol = molecule.toRWMol()

        try {
            val atom = mol.getAtomWithIdx(targetAtomIdx)

            // --- SET NEW FORMAL CHARGE ---
            val newCharge = (atom.formalCharge + delta)
                .coerceIn(MIN_FORMAL_CHARGE, MAX_FORMAL_CHARGE)
            atom.formalCharge = newCharge

            try {
                mol.sanitizeMol()
            } catch (_: Exception) {
            }

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            val molecule = mol.toMolecule()
            return@withContext molecule
        } finally {
            // --- CLEANUP ---
            mol.delete()
        }
    }

    override suspend fun getInchiKey(molecule: Molecule): Result<String, ChemistryError> =
        withContext(Dispatchers.Default) {
            // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
            val mol = molecule.toRWMol(sanitize = true)

            try {
                val inchiKey = RDKFuncs.MolToInchiKey(mol)
                return@withContext Result.Success(inchiKey)
            } catch (_: Exception) {
                return@withContext Result.Error(ChemistryError.SanitizationFailed)
            } finally {
                // --- CLEANUP ---
                mol.delete()
            }
        }
}