package org.liganddraw.app.editor.domain

enum class ArrowHeadShape { FULL, HALF }
enum class ArrowHeadHalf { TOP, BOTTOM }
enum class ArrowHandle { START, END, CURVE }
enum class ReactionArrowType { FORWARD, RESONANCE, EQUILIBRIUM, ELECTRON_PAIR_PUSHING, SINGLE_ELECTRON_PUSHING }
sealed interface ReactionArrow {
    val id: String
    val startX: Float
    val startY: Float
    val endX: Float
    val endY: Float
    fun withPositions(startX: Float, startY: Float, endX: Float, endY: Float): ReactionArrow

    data class Forward(
        override val id: String,
        override val startX: Float,
        override val startY: Float,
        override val endX: Float,
        override val endY: Float,
    ) : ReactionArrow {
        override fun withPositions(startX: Float, startY: Float, endX: Float, endY: Float) =
            copy(startX = startX, startY = startY, endX = endX, endY = endY)
    }

    data class ElectronPushing(
        override val id: String,
        override val startX: Float,
        override val startY: Float,
        override val endX: Float,
        override val endY: Float,
        val curveBow: Float = -50f,
        val headShape: ArrowHeadShape = ArrowHeadShape.FULL
    ) : ReactionArrow {
        override fun withPositions(startX: Float, startY: Float, endX: Float, endY: Float) =
            copy(startX = startX, startY = startY, endX = endX, endY = endY)
    }

    data class Resonance(
        override val id: String,
        override val startX: Float,
        override val startY: Float,
        override val endX: Float,
        override val endY: Float,
    ) : ReactionArrow {
        override fun withPositions(startX: Float, startY: Float, endX: Float, endY: Float) =
            copy(startX = startX, startY = startY, endX = endX, endY = endY)
    }

    data class Equilibrium(
        override val id: String,
        override val startX: Float,
        override val startY: Float,
        override val endX: Float,
        override val endY: Float,
        val bias: Float = 0f,
        val topHeadShape: ArrowHeadShape = ArrowHeadShape.HALF,
        val bottomHeadShape: ArrowHeadShape = ArrowHeadShape.HALF
    ) : ReactionArrow {
        override fun withPositions(startX: Float, startY: Float, endX: Float, endY: Float) =
            copy(startX = startX, startY = startY, endX = endX, endY = endY)
    }
}