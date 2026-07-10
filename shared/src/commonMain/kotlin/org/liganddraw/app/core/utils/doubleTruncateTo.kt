package org.liganddraw.app.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.truncateTo(decimals: Int): Double {
    if (this.isNaN() || this.isInfinite()) return this
    return BigDecimal(this)
        .setScale(decimals, RoundingMode.DOWN)
        .toDouble()
}