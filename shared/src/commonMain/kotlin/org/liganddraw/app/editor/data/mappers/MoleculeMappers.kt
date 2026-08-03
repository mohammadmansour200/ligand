package org.liganddraw.app.editor.data.mappers

import org.RDKit.Conformer
import org.RDKit.Point3D
import org.RDKit.RWMol
import org.liganddraw.app.core.domain.utils.safeValueOf
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.DoubleBondAlignment
import org.liganddraw.app.editor.domain.Molecule

/**Converts UI molecule to RDKit molecule - DOESN'T HANDLE CLEANUP and should invoke mol.delete()*/
fun Molecule.toRWMol(): RWMol {
    val mol = RWMol()

    // --- SET ATOMS ---
    val coordinates = mutableListOf<Point3D>()
    this.atoms.forEach { atom ->
        val rdkitAtom = org.RDKit.Atom(atom.symbol)
        rdkitAtom.formalCharge = atom.charge
        mol.addAtom(rdkitAtom)
        coordinates.add(Point3D(atom.x, atom.y, 0.0))
    }

    // --- SET BONDS ---
    this.bonds.forEach {
        val beginAtomIdx = it.beginAtomIndex
        val endAtomIdx = it.endAtomIndex
        val bondType = when (it) {
            is Bond.Double -> org.RDKit.Bond.BondType.DOUBLE
            is Bond.Hydrogen -> org.RDKit.Bond.BondType.HYDROGEN
            is Bond.Ionic -> org.RDKit.Bond.BondType.IONIC
            is Bond.Triple -> org.RDKit.Bond.BondType.TRIPLE
            else -> org.RDKit.Bond.BondType.SINGLE
        }

        mol.addBond(beginAtomIdx, endAtomIdx, bondType)

        // Set bond direction if available
        if (it is Bond.Single && it.direction != BondDir.NONE) {
            val rdkitBondDir =
                safeValueOf<org.RDKit.Bond.BondDir>(
                    it.direction.name,
                    org.RDKit.Bond.BondDir.NONE
                )
            val addedBond = mol.getBondBetweenAtoms(beginAtomIdx, endAtomIdx)
            addedBond.bondDir = rdkitBondDir
        }
    }

    // --- SET ATOM X, Y POSITIONS ---
    val conformer = Conformer(coordinates.size.toLong())
    coordinates.forEachIndexed { index, point3D ->
        conformer.setAtomPos(index.toLong(), point3D)
    }
    conformer.is3D = false
    mol.addConformer(
        conformer, true
    )

    mol.sanitizeMol()

    return mol
}

/**Converts RDKit molecule to UI molecule - DOESN'T HANDLE CLEANUP and should invoke mol.delete()*/
fun RWMol.toMolecule(includeDepictionMetadata: Boolean = true): Molecule {
    this.Kekulize()

    // Generate 2d conformer if none available
    if (this.numConformers == 0L) {
        this.compute2DCoords()
    }

    val conformer = this.conformer

    // --- SET ATOMS ---
    val atoms = mutableListOf<Atom>()
    for (i in 0 until this.numAtoms) {
        val atomPosition = conformer.getAtomPos(i)
        val atom = this.getAtomWithIdx(i)

        val gasteigerCharge =
            try {
                atom.getProp("_GasteigerCharge").toFloatOrNull()
            } catch (_: Exception) {
                null
            }

        atoms.add(
            Atom(
                x = atomPosition.x,
                y = atomPosition.y,
                z = atomPosition.z,
                symbol = atom.symbol,
                numImplicitHydrogen = atom.numImplicitHs,
                charge = atom.formalCharge,
                gasteigerCharge = gasteigerCharge,
                isLabelReversed = if (includeDepictionMetadata) atom.isLabelReversed(conformer) else false,
                isLabelVisible = if (includeDepictionMetadata) atom.isLabelVisible() else false,
            )
        )
    }

    // --- SET BONDS ---
    val bonds = mutableListOf<Bond>()
    for (i in 0 until this.numBonds) {
        val bond = this.getBondWithIdx(i)

        bonds.add(
            when (bond.bondType) {
                org.RDKit.Bond.BondType.DOUBLE ->
                    Bond.Double(
                        beginAtomIndex = bond.beginAtomIdx,
                        endAtomIndex = bond.endAtomIdx,
                        alignment = if (includeDepictionMetadata) bond.doubleBondAlignment(conformer) else DoubleBondAlignment.CENTERED
                    )


                org.RDKit.Bond.BondType.TRIPLE -> Bond.Triple(
                    beginAtomIndex = bond.beginAtomIdx,
                    endAtomIndex = bond.endAtomIdx
                )

                org.RDKit.Bond.BondType.IONIC -> Bond.Ionic(
                    beginAtomIndex = bond.beginAtomIdx,
                    endAtomIndex = bond.endAtomIdx
                )

                org.RDKit.Bond.BondType.HYDROGEN -> Bond.Hydrogen(
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
    return Molecule(atoms, bonds)
}

