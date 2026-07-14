package org.liganddraw.app.editor.data.cheminformatics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.RDKit.Bond.BondType
import org.RDKit.Conformer
import org.RDKit.DistanceGeom
import org.RDKit.ForceField
import org.RDKit.RDKFuncs
import org.RDKit.ROMol
import org.RDKit.RWMol
import org.RDKit.SDMolSupplier
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result
import org.liganddraw.app.core.domain.utils.safeValueOf
import org.liganddraw.app.editor.data.mappers.toRdkitMol
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.DoubleBondAlignment
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculeProperties

class RDKitCheminformaticsDataSource : CheminformaticsDataSource {
    override suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local> =
        withContext(
            Dispatchers.Default
        ) {
            try {
                val mol: ROMol =
                    RWMol.MolFromMolFile(absolutePath, false) // Sanitize is set to false

                // Generate 2d conformer if none available
                if (mol.numConformers == 0L) {
                    mol.compute2DCoords()
                }

                val conformer = mol.conformer
                // Generating wedging information as RDKit doesn't store bond wedging information by default
                mol.WedgeMolBonds(conformer)

                val atoms = mutableListOf<Atom>()
                for (i in 0 until mol.numAtoms) {
                    val atomPosition = conformer.getAtomPos(i)
                    val atom = mol.getAtomWithIdx(i)

                    atoms.add(
                        Atom(
                            x = atomPosition.x,
                            y = atomPosition.y,
                            z = atomPosition.z,
                            symbol = atom.symbol,
                            charge = atom.formalCharge
                        )
                    )
                }

                val bonds = mutableListOf<Bond>()
                for (i in 0 until mol.numBonds) {
                    val bond = mol.getBondWithIdx(i)

                    bonds.add(
                        when (bond.bondType) {
                            BondType.DOUBLE -> {
                                val alignment =
                                    determineDoubleBondAlignment(
                                        bond,
                                        mol,
                                        conformer
                                    )
                                Bond.Double(
                                    beginAtomIndex = bond.beginAtomIdx,
                                    endAtomIndex = bond.endAtomIdx,
                                    alignment = alignment
                                )
                            }

                            BondType.TRIPLE -> Bond.Triple(
                                beginAtomIndex = bond.beginAtomIdx,
                                endAtomIndex = bond.endAtomIdx
                            )

                            BondType.IONIC -> Bond.Ionic(
                                beginAtomIndex = bond.beginAtomIdx,
                                endAtomIndex = bond.endAtomIdx
                            )

                            BondType.HYDROGEN -> Bond.Hydrogen(
                                beginAtomIndex = bond.beginAtomIdx,
                                endAtomIndex = bond.endAtomIdx
                            )

                            else -> Bond.Single(
                                beginAtomIndex = bond.beginAtomIdx,
                                endAtomIndex = bond.endAtomIdx,
                                direction = safeValueOf<BondDir>(bond.bondDir.name, BondDir.NONE)
                            )
                        }
                    )
                }

                mol.delete()
                return@withContext Result.Success(listOf(Molecule(atoms, bonds)))
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
                val supplier = SDMolSupplier(absolutePath, false) // Sanitize is set to false
                val molecules = mutableListOf<Molecule>()

                // Iterate molecules in SDF
                while (!supplier.atEnd()) {
                    val mol = supplier.next() ?: continue

                    // Generate 2d conformer if none available
                    if (mol.numConformers == 0L) {
                        mol.compute2DCoords()
                    }

                    val conformer = mol.conformer
                    // Generating wedging information as RDKit doesn't store bond wedging information by default
                    mol.WedgeMolBonds(conformer)

                    val atoms = mutableListOf<Atom>()
                    for (i in 0 until mol.numAtoms) {
                        val atomPosition = conformer.getAtomPos(i)
                        val atom = mol.getAtomWithIdx(i)

                        atoms.add(
                            Atom(
                                x = atomPosition.x,
                                y = atomPosition.y,
                                z = atomPosition.z,
                                symbol = atom.symbol,
                                charge = atom.formalCharge
                            )
                        )
                    }

                    val bonds = mutableListOf<Bond>()
                    for (i in 0 until mol.numBonds) {
                        val bond = mol.getBondWithIdx(i)

                        bonds.add(
                            when (bond.bondType) {
                                BondType.DOUBLE -> {
                                    val alignment =
                                        determineDoubleBondAlignment(
                                            bond,
                                            mol,
                                            conformer
                                        )
                                    Bond.Double(
                                        beginAtomIndex = bond.beginAtomIdx,
                                        endAtomIndex = bond.endAtomIdx,
                                        alignment = alignment
                                    )
                                }

                                BondType.TRIPLE -> Bond.Triple(
                                    beginAtomIndex = bond.beginAtomIdx,
                                    endAtomIndex = bond.endAtomIdx
                                )

                                BondType.IONIC -> Bond.Ionic(
                                    beginAtomIndex = bond.beginAtomIdx,
                                    endAtomIndex = bond.endAtomIdx
                                )

                                BondType.HYDROGEN -> Bond.Hydrogen(
                                    beginAtomIndex = bond.beginAtomIdx,
                                    endAtomIndex = bond.endAtomIdx
                                )

                                else -> Bond.Single(
                                    beginAtomIndex = bond.beginAtomIdx,
                                    endAtomIndex = bond.endAtomIdx,
                                    direction = safeValueOf<BondDir>(
                                        bond.bondDir.name,
                                        BondDir.NONE
                                    )
                                )
                            }
                        )
                    }

                    mol.delete()
                    molecules.add(Molecule(atoms, bonds))
                }
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
            val rdkitMol = molecule.toRdkitMol()

            // --- GENERATE CONFORMER ---
            // Add hydrogens for more accurate conformer prediction
            val hydrogenatedRdkitMol = RDKFuncs.addHs(rdkitMol)
            rdkitMol.delete()

            val embedParams = RDKFuncs.getETKDGv3()
            val conformerId = DistanceGeom.EmbedMolecule(hydrogenatedRdkitMol, embedParams)

            ForceField.MMFFOptimizeMolecule(
                hydrogenatedRdkitMol,
                "MMFF94",
                1000,
                10.0,
                conformerId,
                false
            )

            // --- CONVERT TO MOLECULE ---
            // Set atoms
            val generated3DConformer = hydrogenatedRdkitMol.getConformer(conformerId)
            val atoms = mutableListOf<Atom>()
            for (i in 0 until hydrogenatedRdkitMol.numAtoms) {
                val atomPosition = generated3DConformer.getAtomPos(i)
                val atom = hydrogenatedRdkitMol.getAtomWithIdx(i)

                atoms.add(
                    Atom(
                        x = atomPosition.x,
                        y = atomPosition.y,
                        z = atomPosition.z,
                        symbol = atom.symbol,
                        charge = atom.formalCharge
                    )
                )
            }

            // Set bonds
            val bonds = mutableListOf<Bond>()
            for (i in 0 until hydrogenatedRdkitMol.numBonds) {
                val bond = hydrogenatedRdkitMol.getBondWithIdx(i)

                bonds.add(
                    when (bond.bondType) {
                        BondType.DOUBLE ->
                            Bond.Double(
                                beginAtomIndex = bond.beginAtomIdx,
                                endAtomIndex = bond.endAtomIdx,
                                alignment = DoubleBondAlignment.POSITIVE
                            )

                        BondType.TRIPLE -> Bond.Triple(
                            beginAtomIndex = bond.beginAtomIdx,
                            endAtomIndex = bond.endAtomIdx
                        )

                        BondType.IONIC -> Bond.Ionic(
                            beginAtomIndex = bond.beginAtomIdx,
                            endAtomIndex = bond.endAtomIdx
                        )

                        BondType.HYDROGEN -> Bond.Hydrogen(
                            beginAtomIndex = bond.beginAtomIdx,
                            endAtomIndex = bond.endAtomIdx
                        )

                        else -> Bond.Single(
                            beginAtomIndex = bond.beginAtomIdx,
                            endAtomIndex = bond.endAtomIdx,
                            direction = safeValueOf<BondDir>(bond.bondDir.name, BondDir.NONE)
                        )
                    }
                )
            }
            hydrogenatedRdkitMol.delete()

            return@withContext Result.Success(Molecule(atoms, bonds))
        }

    override suspend fun calcProperties(molecule: Molecule): Result<MoleculeProperties, DataError.Local> =
        withContext(Dispatchers.Default) {
            val rdkitMol = molecule.toRdkitMol()

            val logp = RDKFuncs.calcMolLogP(rdkitMol)
            val hba = RDKFuncs.calcNumHBA(rdkitMol)
            val hbd = RDKFuncs.calcNumHBD(rdkitMol)
            val mwt = RDKFuncs.calcAMW(rdkitMol)
            val rotatable = RDKFuncs.calcNumRotatableBonds(rdkitMol)

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

    private fun determineDoubleBondAlignment(
        bond: org.RDKit.Bond,
        mol: ROMol,
        conformer: Conformer
    ): DoubleBondAlignment {
        val isCentered = bond.beginAtom.degree == 1L || bond.endAtom.degree == 1L
        return if (isCentered) DoubleBondAlignment.CENTERED else
            determineAsymmetricDoubleBondSide(
                bond,
                mol,
                conformer
            )
    }

    private fun determineAsymmetricDoubleBondSide(
        bond: org.RDKit.Bond,
        mol: ROMol,
        conformer: Conformer
    ): DoubleBondAlignment {
        val startPos = conformer.getAtomPos(bond.beginAtomIdx)
        val endPos = conformer.getAtomPos(bond.endAtomIdx)

        // 1. Identify which rings contain this bond
        val bondRings = mol.ringInfo.bondRings()

        if (bondRings.isEmpty) return DoubleBondAlignment.POSITIVE

        val bondInRings = mutableListOf<Int>()

        for (i in 0 until bondRings.size()) {
            val ring = bondRings.get(i.toInt())
            for (j in 0 until ring.size()) {
                val ringBondIdx = ring.get(j.toInt())
                if (ringBondIdx == bond.idx.toInt()) {
                    bondInRings.add(i.toInt())
                }
            }
        }

        if (bondInRings.isEmpty()) return DoubleBondAlignment.POSITIVE

        // 2. Choose the ring to use
        val currentBond = mol.getBondWithIdx(bond.idx)
        var ringToUse = bondRings.get(bondInRings.first())

        if (bondInRings.size > 1) {
            for (i in bondInRings) {
                val ring = bondRings.get(i)
                var ringOk = true
                for (j in 0 until ring.size()) {
                    val bIdx = ring.get(j.toInt())
                    val otherBond = mol.getBondWithIdx(bIdx.toLong())
                    if (currentBond.isAromatic != otherBond.isAromatic) {
                        ringOk = false
                        break
                    }
                }
                if (ringOk) {
                    ringToUse = ring
                    break
                }
            }
        }

        val ringBondSet = HashSet<Int>()
        for (i in 0 until ringToUse.size()) {
            ringBondSet.add(ringToUse.get(i.toInt()))
        }

        // 3. Find one adjacent ring atom connected to the start of our bond
        var thirdAtomIdx = -1L
        val beginAtom = mol.getAtomWithIdx(bond.beginAtomIdx)
        val beginAtomBonds = mol.getAtomBonds(beginAtom)

        for (i in 0 until beginAtomBonds.size()) {
            val b = beginAtomBonds.get(i.toInt())
            if (b.idx == bond.idx) continue
            if (ringBondSet.contains(b.idx.toInt())) {
                thirdAtomIdx =
                    if (b.beginAtomIdx == bond.beginAtomIdx) b.endAtomIdx else b.beginAtomIdx
                break
            }
        }

        // 4. Calculate the side pointing "inside" the ring
        if (thirdAtomIdx != -1L) {
            val thirdAtomPos = conformer.getAtomPos(thirdAtomIdx)

            val vx = endPos.x - startPos.x
            val vy = endPos.y - startPos.y

            val rx = thirdAtomPos.x - startPos.x
            val ry = thirdAtomPos.y - startPos.y

            val crossProduct = (vx * ry) - (vy * rx)

            return if (crossProduct >= 0.0) DoubleBondAlignment.NEGATIVE else DoubleBondAlignment.POSITIVE
        }

        return DoubleBondAlignment.POSITIVE
    }
}