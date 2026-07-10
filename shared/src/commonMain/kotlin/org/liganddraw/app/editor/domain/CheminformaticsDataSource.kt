package org.liganddraw.app.editor.domain

import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result

interface CheminformaticsDataSource {
    /**
     * This suspend function performs non-blocking I/O to read .mol file at the specified
     * absolute path and converts the chemical structure data into one [Molecule].
     * Even if the file contains only one structure, the result is wrapped in a list.
     *
     * @param absolutePath The full file system path to the `.mol` file.
     * @return [Result] containing a [List] of [Molecule] on success, or a [DataError.Local]
     */
    suspend fun molFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local>

    /**
     * This suspend function performs non-blocking I/O to read .sdf file at the specified
     * absolute path and converts the chemical structure data into list of [Molecule].
     *
     * @param absolutePath The full file system path to the `.sdf` file.
     * @return [Result] containing a [List] of [Molecule] on success, or a [DataError.Local]
     */
    suspend fun sdfFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local>

    /**
     * This suspend function performs non-blocking I/O to read .cdx file at the specified
     * absolute path and converts the chemical structure data into list of [Molecule].
     *
     * @param absolutePath The full file system path to the `.cdx` file.
     * @return [Result] containing a [List] of [Molecule] on success, or a [DataError.Local]
     */
    suspend fun cdxFileToMolecule(absolutePath: String): Result<List<Molecule>, DataError.Local>

    /**
     * Predicts 3D conformer via ETKDGv3 & MMFF94 field optimization of 2D molecule
     * @param [molecule] Selected Molecule from canvas.
     * @return [Result] containing a [Molecule] on success, or a [DataError.Local]
     */
    suspend fun generate3DConformer(molecule: Molecule): Result<Molecule, DataError.Local>

    /**
     * Calculates LogP, Molecular Weight, HBA, HBD and Rotatable bonds of 2D molecule
     * @param [molecule] Selected Molecule from canvas.
     * @return [Result] containing [MoleculeProperties] on success, or a [DataError.Local]
     */
    suspend fun calcProperties(molecule: Molecule): Result<MoleculeProperties, DataError.Local>
}
