package org.liganddraw.app.editor.data.cheminformatics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.RDKit.Bond.BondType
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
import org.liganddraw.app.core.domain.utils.safeValueOf
import org.liganddraw.app.editor.data.mappers.doubleBondAlignment
import org.liganddraw.app.editor.data.mappers.isLabelReversed
import org.liganddraw.app.editor.data.mappers.isLabelVisible
import org.liganddraw.app.editor.data.mappers.toMolecule
import org.liganddraw.app.editor.data.mappers.toRWMol
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculeProperties

class RDKitCheminformaticsDataSource : CheminformaticsDataSource {
    override suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError> =
        withContext(
            Dispatchers.Default
        ) {
            try {
                // --- READ .mol FILE ---
                val mol =
                    RWMol.MolFromMolFile(absolutePath, true) // Sanitize is set to true

                // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
                val molecule = mol.toMolecule()

                // --- CLEANUP ---
                mol.delete()
                return@withContext Result.Success(listOf(molecule))
            } catch (e: Exception) {
                println(e.message)
                return@withContext Result.Error(DataError.FileCorrupted)
            }
        }

    override suspend fun sdfFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError> =
        withContext(
            Dispatchers.Default
        ) {
            try {
                val supplier = SDMolSupplier(absolutePath, true) // Sanitize is set to true
                val molecules = mutableListOf<Molecule>()

                // --- ITERATE MOLECULES FOUND IN SDF FILE ---
                while (!supplier.atEnd()) {
                    val roMol = supplier.next() ?: continue
                    val rwMol = RWMol(roMol)

                    // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
                    val molecule = rwMol.toMolecule()

                    // Cleanup
                    roMol.delete()
                    rwMol.delete()
                    molecules.add(molecule)
                }
                // Cleanup
                supplier.delete()
                return@withContext Result.Success(molecules)
            } catch (e: Exception) {
                println(e.message)
                return@withContext Result.Error(DataError.FileCorrupted)
            }
        }

    override suspend fun generate3DConformer(molecule: Molecule): Result<Molecule, ChemistryError> =
        withContext(
            Dispatchers.Default
        ) {
            // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
            val rdkitMol: ROMol = molecule.toRWMol()

            // --- GENERATE CONFORMER ---
            // Add hydrogens for more accurate conformer prediction
            val hydrogenatedRdkitROMol = RDKFuncs.addHs(rdkitMol)

            val embedParams = RDKFuncs.getETKDGv3()
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
                val matchVect = Match_Vect()
                val numHeavyAtoms = rdkitMol.numHeavyAtoms

                for (i in 0 until numHeavyAtoms) {
                    matchVect.add(Int_Pair(i.toInt(), i.toInt()))
                }

                hydrogenatedRdkitROMol.alignMol(
                    rdkitMol,
                    conformerId,
                    0,
                    matchVect,
                )

                val conf3D = hydrogenatedRdkitROMol.getConformer(conformerId)
                val centroid = conf3D.computeCentroid()

                val trans = Transform3D()
                trans.SetTranslation(Point3D(-centroid.x, -centroid.y, -centroid.z))
                conf3D.transformConformer(trans)
            } catch (_: Exception) {
            }
            rdkitMol.delete()

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            val hydrogenatedRdkitRWMol = RWMol(hydrogenatedRdkitROMol)

            // Cleanup
            hydrogenatedRdkitROMol.delete()

            val molecule = hydrogenatedRdkitRWMol.toMolecule(false)

            return@withContext Result.Success(molecule)
        }

    override suspend fun calcProperties(molecule: Molecule): Result<MoleculeProperties, ChemistryError> =
        withContext(Dispatchers.Default) {
            // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
            val rdkitMol = molecule.toRWMol()

            // --- CALCULATE PROPERTIES ---
            val logp = RDKFuncs.calcMolLogP(rdkitMol)
            val hba = RDKFuncs.calcNumHBA(rdkitMol)
            val hbd = RDKFuncs.calcNumHBD(rdkitMol)
            val mwt = RDKFuncs.calcAMW(rdkitMol)
            val rotatable = RDKFuncs.calcNumRotatableBonds(rdkitMol)

            // --- CLEANUP ---
            rdkitMol.delete()
            return@withContext Result.Success(
                MoleculeProperties(
                    logp = logp,
                    molecularWeight = mwt,
                    hydrogenBondAcceptors = hba,
                    hydrogenBondDonors = hbd,
                    rotatableBonds = rotatable
                )
            )
        }

    override suspend fun addBond(
        molecule: Molecule,
        beginAtomIdx: Long,
        type: BondType,
        dir: org.RDKit.Bond.BondDir
    ): Result<Molecule, ChemistryError> = withContext(Dispatchers.Default) {
        // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
        val rdkitRWMol = molecule.toRWMol()
        val template = ROMol(rdkitRWMol)

        // --- ADD END ATOM IN RDKIT MOLECULE ---
        val endAtomIdx = rdkitRWMol.addAtom(org.RDKit.Atom("C"))

        // --- ADD BOND IN RDKIT MOLECULE ---
        rdkitRWMol.addBond(beginAtomIdx, endAtomIdx, type)
        // Maintains accurate bond angles
        rdkitRWMol.generateDepictionMatching2DStructure(template)
        template.delete()

        try {
            rdkitRWMol.sanitizeMol()
        } catch (e: Exception) {
            // TODO(Handle valence error)
            println(e)
        }

        val addedBond = rdkitRWMol.getBondBetweenAtoms(beginAtomIdx, endAtomIdx)
        addedBond.bondDir = dir

        val conformer = rdkitRWMol.conformer
        val atoms = molecule.atoms.toMutableList()
        val bonds = molecule.bonds.toMutableList()

        val beginAtom = rdkitRWMol.getAtomWithIdx(beginAtomIdx)
        // --- UPDATE UI BEGIN ATOM DOUBLE BONDS ---
        for (i in 0 until beginAtom.bonds.size()) {
            val currentBond = beginAtom.bonds[i.toInt()]
            if (currentBond.bondType == BondType.DOUBLE)
                bonds[currentBond.idx.toInt()] = Bond.Double(
                    beginAtomIndex = currentBond.beginAtomIdx,
                    endAtomIndex = currentBond.endAtomIdx,
                    alignment = currentBond.doubleBondAlignment(conformer)
                )
        }

        // --- UPDATE UI BEGIN ATOM ---
        val beginAtomPosition = conformer.getAtomPos(beginAtomIdx)
        atoms[beginAtomIdx.toInt()] = Atom(
            x = beginAtomPosition.x,
            y = beginAtomPosition.y,
            z = beginAtomPosition.z,
            symbol = beginAtom.symbol,
            numImplicitHydrogen = beginAtom.numImplicitHs,
            charge = beginAtom.formalCharge,
            gasteigerCharge = null,
            isLabelReversed = beginAtom.isLabelReversed(conformer),
            isLabelVisible = beginAtom.isLabelVisible(),
        )

        // --- ADD UI END ATOM ---
        val endAtomPosition = conformer.getAtomPos(endAtomIdx)
        val endAtom = rdkitRWMol.getAtomWithIdx(endAtomIdx)

        atoms.add(
            Atom(
                x = endAtomPosition.x,
                y = endAtomPosition.y,
                z = endAtomPosition.z,
                symbol = endAtom.symbol,
                numImplicitHydrogen = endAtom.numImplicitHs,
                charge = endAtom.formalCharge,
                gasteigerCharge = null,
                isLabelReversed = endAtom.isLabelReversed(conformer),
                isLabelVisible = endAtom.isLabelVisible(),
            )
        )

        // --- ADD UI BOND ---
        bonds.add(
            when (type) {
                BondType.DOUBLE ->
                    Bond.Double(
                        beginAtomIndex = addedBond.beginAtomIdx,
                        endAtomIndex = addedBond.endAtomIdx,
                        alignment = addedBond.doubleBondAlignment(conformer)
                    )

                BondType.TRIPLE -> Bond.Triple(
                    beginAtomIndex = addedBond.beginAtomIdx,
                    endAtomIndex = addedBond.endAtomIdx
                )

                BondType.IONIC -> Bond.Ionic(
                    beginAtomIndex = addedBond.beginAtomIdx,
                    endAtomIndex = addedBond.endAtomIdx
                )

                BondType.HYDROGEN -> Bond.Hydrogen(
                    beginAtomIndex = addedBond.beginAtomIdx,
                    endAtomIndex = addedBond.endAtomIdx
                )

                else -> Bond.Single(
                    beginAtomIndex = addedBond.beginAtomIdx,
                    endAtomIndex = addedBond.endAtomIdx,
                    direction = safeValueOf<BondDir>(dir.name, BondDir.NONE)
                )
            }
        )
        // --- CLEANUP ---
        rdkitRWMol.delete()

        return@withContext Result.Success(Molecule(atoms, bonds))
    }

    override suspend fun replaceAtomWithAtom(
        molecule: Molecule,
        atomIdx: Long,
        newAtomSymbol: String,
    ): Result<Molecule, ChemistryError> = withContext(Dispatchers.Default) {
        // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
        val rdkitRWMol = molecule.toRWMol()

        val periodicTable = PeriodicTable.getTable()
        val newAtomAtomicNumber = periodicTable.getAtomicNumber(newAtomSymbol)

        // --- EDIT ATOM IN RDKit MOLECULE ---
        rdkitRWMol.getAtomWithIdx(atomIdx).atomicNum = newAtomAtomicNumber

        try {
            rdkitRWMol.sanitizeMol()
        } catch (e: Exception) {
            // TODO(Handle valence error)
            println(e)
        }

        // --- EDIT ATOM IN UI MOLECULE ---
        val conformer = rdkitRWMol.conformer
        val newAtom = rdkitRWMol.getAtomWithIdx(atomIdx)
        val newAtomPosition = conformer.getAtomPos(atomIdx)
        val atoms = molecule.atoms.toMutableList()
        atoms[atomIdx.toInt()] = Atom(
            x = newAtomPosition.x,
            y = newAtomPosition.y,
            z = newAtomPosition.z,
            symbol = newAtom.symbol,
            numImplicitHydrogen = newAtom.numImplicitHs,
            charge = newAtom.formalCharge,
            gasteigerCharge = null,
            isLabelReversed = newAtom.isLabelReversed(conformer),
            isLabelVisible = newAtom.isLabelVisible(),
        )

        // --- CLEANUP ---
        rdkitRWMol.delete()

        return@withContext Result.Success(Molecule(atoms, molecule.bonds))
    }

    override suspend fun replaceAtomWithTemplate(
        molecule: Molecule,
        targetAtomIdx: Long,
        templateSmiles: String
    ): Result<Molecule, ChemistryError> = withContext(Dispatchers.Default) {
        val templateMol = RWMol.MolFromSmiles(templateSmiles)
        val rdkitRWMol = molecule.toRWMol()
        val depictionTemplateMol = ROMol(rdkitRWMol)

        try {
            val targetAtom = rdkitRWMol.getAtomWithIdx(targetAtomIdx)

            // --- INSERT TEMPLATE MOLECULE INTO MAIN MOLECULE ---
            rdkitRWMol.insertMol(templateMol)

            // --- BOND TEMPLATE MOLECULE TO ATOM: atom has multiple existing bonds ---
            val templateAnchorIdx = rdkitRWMol.numAtoms
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
            }

            rdkitRWMol.generateDepictionMatching2DStructure(depictionTemplateMol)
            rdkitRWMol.sanitizeMol()

            Result.Success(rdkitRWMol.toMolecule())
        } finally {
            templateMol.delete()
            rdkitRWMol.delete()
            depictionTemplateMol.delete()
        }
    }

    override suspend fun createMoleculeFromSmiles(
        smiles: String,
        x: Double,
        y: Double
    ): Molecule = withContext(Dispatchers.Default) {
        // --- CONVERT SMILES INTO RDKIT MOLECULE ---
        val mol = RWMol.MolFromSmiles(smiles)
        try {
            // --- COMPUTE COORDINATES ---
            mol.compute2DCoords()

            val conformer = mol.conformer

            // --- PLACE MOLECULE AT CENTER OF X, Y coordinates ---
            // offsets to place molecule center at target (x, y)
            val centroid = conformer.computeCentroid()
            val deltaX = x - centroid.x
            val deltaY = y - centroid.y

            val transform = Transform3D()
            transform.SetTranslation(Point3D(deltaX, deltaY, 0.0))
            conformer.transformConformer(transform)

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            return@withContext mol.toMolecule()
        } finally {
            // --- CLEANUP ---
            mol.delete()
        }
    }
}