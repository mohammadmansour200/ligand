package org.liganddraw.app.editor.data.cheminformatics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.RDKit.Atom
import org.RDKit.Bond
import org.RDKit.Bond.BondType
import org.RDKit.Conformer
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
import org.liganddraw.app.editor.domain.DrawingPaneConstants.MAX_FORMAL_CHARGE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.MIN_FORMAL_CHARGE
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculeProperties

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
            } catch (_: Exception) {
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
                val logp = RDKFuncs.calcMolLogP(rdkitMol)
                val hba = RDKFuncs.calcNumHBA(rdkitMol)
                val hbd = RDKFuncs.calcNumHBD(rdkitMol)
                val mwt = RDKFuncs.calcAMW(rdkitMol)
                val rotatable = RDKFuncs.calcNumRotatableBonds(rdkitMol)

                return@withContext Result.Success(
                    MoleculeProperties(
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
            // --- ADD END ATOM IN RDKIT MOLECULE ---
            val endAtomIdx = rdkitRWMol.addAtom(Atom("C"))

            // --- ADD BOND IN RDKIT MOLECULE ---
            rdkitRWMol.addBond(targetAtomIdx, endAtomIdx, type)
            rdkitRWMol.getBondBetweenAtoms(targetAtomIdx, endAtomIdx)?.bondDir = dir

            // Maintains accurate bond angles
            rdkitRWMol.generateDepictionMatching2DStructure(template)

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
}