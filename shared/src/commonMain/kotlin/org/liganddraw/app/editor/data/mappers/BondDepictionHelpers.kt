package org.liganddraw.app.editor.data.mappers

import org.RDKit.Bond
import org.RDKit.Conformer
import org.liganddraw.app.editor.domain.DoubleBondAlignment

/**Determines Sidedness of double bonds*/
fun Bond.doubleBondAlignment(
    conformer: Conformer
): DoubleBondAlignment {
    val isCentered = when {
        this.beginAtom.degree == 1L && this.endAtom.degree >= 3L -> true
        this.endAtom.degree == 1L && this.beginAtom.degree >= 3L -> true
        this.beginAtom.isLabelVisible() && this.endAtom.isLabelVisible() -> true
        else -> false
    }

    return if (isCentered) DoubleBondAlignment.CENTERED else
        this.determineAsymmetricDoubleBondSide(conformer)
}

private fun Bond.determineAsymmetricDoubleBondSide(
    conformer: Conformer
): DoubleBondAlignment {
    val mol = this.owningMol

    val startPos = conformer.getAtomPos(this.beginAtomIdx)
    val endPos = conformer.getAtomPos(this.endAtomIdx)

    // 1. Identify which rings contain this bond
    val bondRings = mol.ringInfo.bondRings()

    if (bondRings.isEmpty) return DoubleBondAlignment.POSITIVE

    val bondInRings = mutableListOf<Int>()

    for (i in 0 until bondRings.size()) {
        val ring = bondRings.get(i.toInt())
        for (j in 0 until ring.size()) {
            val ringBondIdx = ring.get(j.toInt())
            if (ringBondIdx == this.idx.toInt()) {
                bondInRings.add(i.toInt())
            }
        }
    }

    if (bondInRings.isEmpty()) return DoubleBondAlignment.POSITIVE

    // 2. Choose the ring to use
    val currentBond = mol.getBondWithIdx(this.idx)
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
    val beginAtom = mol.getAtomWithIdx(this.beginAtomIdx)
    val beginAtomBonds = mol.getAtomBonds(beginAtom)

    for (i in 0 until beginAtomBonds.size()) {
        val b = beginAtomBonds.get(i.toInt())
        if (b.idx == this.idx) continue
        if (ringBondSet.contains(b.idx.toInt())) {
            thirdAtomIdx =
                if (b.beginAtomIdx == this.beginAtomIdx) b.endAtomIdx else b.beginAtomIdx
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