package org.liganddraw.app.editor.data.mappers

import org.RDKit.Conformer
import org.RDKit.Point3D
import org.RDKit.ROMol
import org.RDKit.RWMol
import org.liganddraw.app.core.domain.utils.safeValueOf
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.Molecule

fun Molecule.toRdkitMol(): ROMol {
    val writableRdkitMol = RWMol()

    // --- SET ATOMS ---
    val coordinates = mutableListOf<Point3D>()
    this.atoms.forEach { atom ->
        val rdkitAtom = org.RDKit.Atom(atom.symbol)
        rdkitAtom.formalCharge = atom.charge
        writableRdkitMol.addAtom(rdkitAtom)
        coordinates.add(Point3D(atom.x, atom.y, 0.0))
    }

    // --- SET BONDS ---
    this.bonds.forEach {
        val beginAtomIdx = it.beginAtomIndex
        val endAtomIdx = it.endAtomIndex
        val bondType =
            safeValueOf<org.RDKit.Bond.BondType>(
                it.type.name,
                org.RDKit.Bond.BondType.SINGLE
            )

        writableRdkitMol.addBond(beginAtomIdx, endAtomIdx, bondType)

        // Set bond direction if available
        if (it.direction != BondDir.NONE) {
            val rdkitBondDir =
                safeValueOf<org.RDKit.Bond.BondDir>(
                    it.direction.name,
                    org.RDKit.Bond.BondDir.NONE
                )
            val addedBond = writableRdkitMol.getBondBetweenAtoms(beginAtomIdx, endAtomIdx)
            addedBond.bondDir = rdkitBondDir
        }
    }

    // --- SET ATOM X, Y POSITIONS ---
    val conformer = Conformer(coordinates.size.toLong())
    coordinates.forEachIndexed { index, point3D ->
        conformer.setAtomPos(index.toLong(), point3D)
    }
    conformer.is3D = false
    writableRdkitMol.addConformer(
        conformer, true
    )

    writableRdkitMol.sanitizeMol()

    val rdkitMol = ROMol(writableRdkitMol)
    writableRdkitMol.delete()

    return rdkitMol
}

