package org.liganddraw.app.editor.data.cheminformatics

import org.RDKit.ChemDrawParserParams
import org.RDKit.RDKFuncs
import org.RDKit.ROMol
import org.RDKit.RWMol
import org.RDKit.SDMolSupplier
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result
import org.liganddraw.app.core.domain.utils.safeValueOf
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.BondType
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.Molecule

class RDKitCheminformaticsDataSource : CheminformaticsDataSource {
    override suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local> {
        try {
            val mol: ROMol = RWMol.MolFromMolFile(absolutePath, false) // Sanitize is set to false

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
                    Bond(
                        beginAtomIndex = bond.beginAtomIdx,
                        endAtomIndex = bond.endAtomIdx,
                        type = safeValueOf<BondType>(bond.bondType.name, BondType.SINGLE),
                        direction = safeValueOf<BondDir>(bond.bondDir.name, BondDir.NONE),
                    )
                )
            }

            mol.delete()
            return Result.Success(listOf(Molecule(atoms, bonds)))
        } catch (e: Exception) {
            println(e.message)
            return Result.Error(DataError.Local.FILE_CORRUPTED)
        }
    }

    override suspend fun sdfFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local> {
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
                        Bond(
                            beginAtomIndex = bond.beginAtomIdx,
                            endAtomIndex = bond.endAtomIdx,
                            type = safeValueOf<BondType>(bond.bondType.name, BondType.SINGLE),
                            direction = safeValueOf<BondDir>(bond.bondDir.name, BondDir.NONE),
                        )
                    )
                }

                mol.delete()
                molecules.add(Molecule(atoms, bonds))
            }
            return Result.Success(molecules)
        } catch (e: Exception) {
            println(e.message)
            return Result.Error(DataError.Local.FILE_CORRUPTED)
        }
    }

    override suspend fun cdxFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local> {
        try {
            val chemDrawParserParams = ChemDrawParserParams()
            chemDrawParserParams.sanitize = false

            val mols = RDKFuncs.MolsFromChemDrawFile(absolutePath, chemDrawParserParams)
            val molecules = mutableListOf<Molecule>()

            // Iterate molecules in SDF
            for (i in 0 until mols.size()) {
                val mol = mols.get(i.toInt()) ?: continue

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
                        Bond(
                            beginAtomIndex = bond.beginAtomIdx,
                            endAtomIndex = bond.endAtomIdx,
                            type = safeValueOf<BondType>(bond.bondType.name, BondType.SINGLE),
                            direction = safeValueOf<BondDir>(bond.bondDir.name, BondDir.NONE),
                        )
                    )
                }

                mol.delete()
                molecules.add(Molecule(atoms, bonds))
            }
            return Result.Success(molecules)
        } catch (e: Exception) {
            println(e.message)
            return Result.Error(DataError.Local.FILE_CORRUPTED)
        }
    }
}