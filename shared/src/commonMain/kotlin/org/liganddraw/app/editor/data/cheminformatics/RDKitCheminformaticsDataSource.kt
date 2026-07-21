package org.liganddraw.app.editor.data.cheminformatics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.RDKit.Bond.BondType
import org.RDKit.DistanceGeom
import org.RDKit.ForceField
import org.RDKit.PeriodicTable
import org.RDKit.RDKFuncs
import org.RDKit.ROMol
import org.RDKit.RWMol
import org.RDKit.SDMolSupplier
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result
import org.liganddraw.app.core.domain.utils.safeValueOf
import org.liganddraw.app.editor.data.mappers.doubleBondAlignment
import org.liganddraw.app.editor.data.mappers.isLabelReversed
import org.liganddraw.app.editor.data.mappers.toMolecule
import org.liganddraw.app.editor.data.mappers.toRWMol
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculeProperties

class RDKitCheminformaticsDataSource : CheminformaticsDataSource {
    override suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local> =
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
                return@withContext Result.Error(DataError.Local.FILE_CORRUPTED)
            }
        }

    override suspend fun sdfFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local> =
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
                return@withContext Result.Error(DataError.Local.FILE_CORRUPTED)
            }
        }

    override suspend fun generate3DConformer(molecule: Molecule): Result<Molecule, DataError.Local> =
        withContext(
            Dispatchers.Default
        ) {
            // --- CONVERT UI MOLECULE INTO RDKIT MOLECULE ---
            val rdkitMol: ROMol = molecule.toRWMol()

            // --- GENERATE CONFORMER ---
            // Add hydrogens for more accurate conformer prediction
            val hydrogenatedRdkitROMol = RDKFuncs.addHs(rdkitMol)
            rdkitMol.delete()

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

            // --- CONVERT RDKIT MOLECULE INTO UI MOLECULE ---
            val hydrogenatedRdkitRWMol = RWMol(hydrogenatedRdkitROMol)

            // Cleanup
            hydrogenatedRdkitROMol.delete()

            val molecule = hydrogenatedRdkitRWMol.toMolecule(false)

            return@withContext Result.Success(molecule)
        }

    override suspend fun calcProperties(molecule: Molecule): Result<MoleculeProperties, DataError.Local> =
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
    ): Result<Molecule, DataError.Local> = withContext(Dispatchers.Default) {
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

        // --- UPDATE UI BEGIN ATOM ---
        val beginAtomPosition = conformer.getAtomPos(beginAtomIdx)
        val beginAtom = rdkitRWMol.getAtomWithIdx(beginAtomIdx)
        atoms[beginAtomIdx.toInt()] = Atom(
            x = beginAtomPosition.x,
            y = beginAtomPosition.y,
            z = beginAtomPosition.z,
            symbol = beginAtom.symbol,
            numImplicitHydrogen = beginAtom.numImplicitHs,
            charge = beginAtom.formalCharge,
            isLabelReversed = beginAtom.isLabelReversed(conformer)
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
                isLabelReversed = false
            )
        )

        // --- ADD UI BOND ---
        val bonds = molecule.bonds.toMutableList()
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

    override suspend fun replaceAtom(
        molecule: Molecule,
        atomIdx: Long,
        newAtomSymbol: String,
    ): Result<Molecule, DataError.Local> = withContext(Dispatchers.Default) {
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
        val replacedAtom = rdkitRWMol.getAtomWithIdx(atomIdx)
        val replacedAtomPosition = conformer.getAtomPos(atomIdx)
        val atoms = molecule.atoms.toMutableList()
        atoms[atomIdx.toInt()] = Atom(
            x = replacedAtomPosition.x,
            y = replacedAtomPosition.y,
            z = replacedAtomPosition.z,
            symbol = replacedAtom.symbol,
            numImplicitHydrogen = replacedAtom.numImplicitHs,
            charge = replacedAtom.formalCharge,
            isLabelReversed = replacedAtom.isLabelReversed(conformer)
        )

        // --- CLEANUP ---
        rdkitRWMol.delete()

        return@withContext Result.Success(Molecule(atoms, molecule.bonds))
    }
}