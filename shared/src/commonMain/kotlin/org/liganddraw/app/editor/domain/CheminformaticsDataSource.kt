package org.liganddraw.app.editor.domain

import org.RDKit.Bond
import org.liganddraw.app.core.domain.ChemistryError
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result

interface CheminformaticsDataSource {
    /**
     * This suspend function performs non-blocking I/O to read .mol file at the specified
     * absolute path and converts the chemical structure data into one [Molecule].
     * Even if the file contains only one structure, the result is wrapped in a list.
     *
     * @param absolutePath The full file system path to the `.mol` file.
     * @return [Result] containing a [List] of [Molecule] on success, or a [DataError]
     */
    suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError>

    /**
     * This suspend function performs non-blocking I/O to read .sdf file at the specified
     * absolute path and converts the chemical structure data into list of [Molecule].
     *
     * @param absolutePath The full file system path to the `.sdf` file.
     * @return [Result] containing a [List] of [Molecule] on success, or a [DataError]
     */
    suspend fun sdfFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError>

    /**
     * Predicts 3D conformer via ETKDGv3 & MMFF94 field optimization of 2D molecule
     * @param [molecule] Selected Molecule from canvas.
     * @return [Result] containing a [Molecule] on success, or a [ChemistryError]
     */
    suspend fun generate3DConformer(molecule: Molecule): Result<Molecule, ChemistryError>

    /**
     * Calculates LogP, Molecular Weight, HBA, HBD and Rotatable bonds of 2D molecule
     * @param [molecule] Selected Molecule from canvas.
     * @return [Result] containing [MoleculeProperties] on success, or a [ChemistryError]
     */
    suspend fun calcProperties(molecule: Molecule): Result<MoleculeProperties, ChemistryError>

    /**
     * Adds a new chemical bond starting from a specified atom in the molecule.
     * @param [molecule] The target [Molecule] to modify.
     * @param [beginAtomIdx] The zero-based index of the starting atom for the bond.
     * @param [type] The bond type to create (e.g., SINGLE, DOUBLE, TRIPLE).
     * @param [dir] The stereochemical direction of the bond [Bond.BondDir] (BEGINWEDGE, BEGINDASH or NONE).
     * Defaults to [Bond.BondDir.NONE].
     * @return [Result] containing updated [Molecule] on success, or a [ChemistryError]
     */
    suspend fun addBond(
        molecule: Molecule,
        beginAtomIdx: Long,
        type: Bond.BondType,
        dir: Bond.BondDir = Bond.BondDir.NONE
    ): Result<Molecule, ChemistryError>

    /**
     * Replaces an existing atom in the molecule with a new atom.
     * @param molecule The target [Molecule] to modify.
     * @param atomIdx The zero-based index of the atom to be replaced.
     * @param newAtomSymbol The standard chemical element symbol for the replacement atom (e.g., "C", "N", "O", "Cl").
     * @return [Result] containing updated [Molecule] on success, or a [ChemistryError]
     */
    suspend fun replaceAtom(
        molecule: Molecule,
        atomIdx: Long,
        newAtomSymbol: String
    ): Result<Molecule, ChemistryError>
}
