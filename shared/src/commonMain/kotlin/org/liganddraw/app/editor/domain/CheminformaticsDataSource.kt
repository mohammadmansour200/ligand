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
     * Attaches a bond to an atom in the molecule.
     * @param [molecule] The target [Molecule] to modify.
     * @param [targetAtomIdx] The zero-based index of the starting atom for the bond.
     * @param [type] The bond type to create (e.g., SINGLE, DOUBLE, TRIPLE).
     * @param [dir] The stereochemical direction of the bond [Bond.BondDir] (BEGINWEDGE, BEGINDASH or NONE). Defaults to [Bond.BondDir.NONE].
     * @return [Molecule] containing updated molecule
     */
    suspend fun attachBondToAtom(
        molecule: Molecule,
        targetAtomIdx: Long,
        type: Bond.BondType,
        dir: Bond.BondDir = Bond.BondDir.NONE
    ): Molecule

    /**
     * Fuses a template (e.g. benzene, cyclohexane) to a bond in the molecule.
     * @param molecule The target [Molecule] to modify.
     * @param targetBondIdx The zero-based index of the bond to be fused to.
     * @param templateSmiles The SMILES representation of the molecule (e.g., "CC" for ethane, "c1ccccc1" for benzene).
     * @return [Molecule] containing updated molecule
     */
    suspend fun fuseTemplateToBond(
        molecule: Molecule,
        targetBondIdx: Long,
        templateSmiles: String,
    ): Molecule

    /**
     * Erases a bond between two atoms.
     * If removing the bond disconnects the molecular graph, the result will contain one
     * [Molecule] per resulting fragment. If it does not, the result will contain a single [Molecule].
     * @param molecule The target [Molecule] to modify.
     * @param targetBondIdx The bond to be erased.
     * @return Resulting fragment(s) as a list of [Molecule]
     */
    suspend fun eraseBond(
        molecule: Molecule,
        targetBondIdx: Long,
    ): List<Molecule>

    /**
     * Erases an atom.
     * If removing the atom disconnects the molecular graph, the result will contain one
     * [Molecule] per resulting fragment. If it does not, the result will contain a single [Molecule].
     * @param molecule The target [Molecule] to modify.
     * @param targetAtomIdx The atom to be erased.
     * @return Resulting fragment(s) as a list of [Molecule]
     */
    suspend fun eraseAtom(
        molecule: Molecule,
        targetAtomIdx: Long,
    ): List<Molecule>

    /**
     * Replaces an existing atom in the molecule with a new atom.
     * @param molecule The target [Molecule] to modify.
     * @param targetAtomIdx The zero-based index of the atom to be replaced.
     * @param newAtomSymbol The standard chemical element symbol for the replacement atom (e.g., "C", "N", "O", "Cl").
     * @return [Molecule] containing updated molecule
     */
    suspend fun replaceAtomWithAtom(
        molecule: Molecule,
        targetAtomIdx: Long,
        newAtomSymbol: String
    ): Molecule

    /**
     * Replaces an existing atom in the molecule with a template (e.g. benzene, cyclohexane).
     * @param molecule The target [Molecule] to modify.
     * @param targetAtomIdx The zero-based index of the atom to be replaced.
     * @param templateSmiles The SMILES representation of the molecule (e.g., "CC" for ethane, "c1ccccc1" for benzene).
     * @return [Molecule] containing updated molecule
     */
    suspend fun replaceAtomWithTemplate(
        molecule: Molecule,
        targetAtomIdx: Long,
        templateSmiles: String
    ): Molecule

    /**
     * Creates a new molecule from a SMILES string at a specified canvas coordinate.
     * @param smiles The SMILES representation of the molecule (e.g., "CC" for ethane, "c1ccccc1" for benzene).
     * @param x The target x-coordinate in angstroms.
     * @param y The target  y-coordinate in angstroms.
     * @return [Molecule]
     */
    suspend fun createMoleculeFromSmiles(
        smiles: String,
        x: Double,
        y: Double
    ): Molecule

    /**
     * Creates a new molecule from an atom at a specified canvas coordinate.
     * @param symbol The atom symbol (e.g., "H" for hydrogen, "C" for carbon).
     * @param x The target x-coordinate in angstroms.
     * @param y The target  y-coordinate in angstroms.
     * @return [Molecule]
     */
    suspend fun createMoleculeFromAtom(
        symbol: String,
        x: Double,
        y: Double
    ): Molecule

    /**
     * Cycles the bond type of the specified bond through single, double, and triple,
     * wrapping back to single after triple (single -> double -> triple -> single).
     * @param molecule The target [Molecule] to modify.
     * @param targetBondIdx The zero-based index of the bond whose type should be cycled.
     * @return [Molecule] containing updated molecule
     */
    suspend fun cycleBondType(
        molecule: Molecule,
        targetBondIdx: Long
    ): Molecule

    /**
     * Changes the bond type and direction of the specified bond
     * @param molecule The target [Molecule] to modify.
     * @param targetBondIdx The zero-based index of the bond whose type should be cycled.
     * @param [type] The bond type to create (e.g., SINGLE, DOUBLE, TRIPLE).
     * @param [dir] The stereochemical direction of the bond [Bond.BondDir] (BEGINWEDGE, BEGINDASH or NONE).
     * @return [Molecule] containing updated molecule
     */
    suspend fun setBondType(
        molecule: Molecule,
        targetBondIdx: Long,
        type: Bond.BondType,
        dir: Bond.BondDir = Bond.BondDir.NONE
    ): Molecule

    /**
     * Changes atom formal charge.
     * @param molecule The target [Molecule] to modify.
     * @param targetAtomIdx The zero-based index of the atom.
     * @param delta Either +1 to increase charge, or -1 to decrease charge
     * @return [Molecule] containing updated molecule
     */
    suspend fun changeFormalCharge(
        molecule: Molecule,
        targetAtomIdx: Long,
        delta: Int,
    ): Molecule
}
