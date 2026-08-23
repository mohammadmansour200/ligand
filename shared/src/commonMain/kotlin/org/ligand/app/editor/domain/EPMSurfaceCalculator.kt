package org.ligand.app.editor.domain

import org.ligand.app.editor.domain.MoleculePaneConstants.ATOM_VAN_DER_WAALS_RADII_MAP
import org.ligand.app.editor.domain.MoleculePaneConstants.FALLBACK_ATOM_VAN_DER_WAALS_RADII
import org.ligand.app.editor.domain.MoleculePaneConstants.SURFACE_RADII_SCALE
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Molecular "density" field. Larger values = deeper inside the molecule.
 */
private fun densityFieldAt(
    x: Float,
    y: Float,
    z: Float,
    atoms: List<Atom>,
    vdwScale: Float = SURFACE_RADII_SCALE,
): Float {
    var sum = 0f
    for (a in atoms) {
        val r = (ATOM_VAN_DER_WAALS_RADII_MAP[a.symbol]
            ?: FALLBACK_ATOM_VAN_DER_WAALS_RADII)
        val sigma = r * vdwScale
        val dx = x - a.x.toFloat()
        val dy = y - a.y.toFloat()
        val dz = z - a.z.toFloat()
        val d2 = dx * dx + dy * dy + dz * dz
        sum += exp(-d2 / (2f * sigma * sigma))
    }
    return sum
}

/**
 * Electrostatic potential at a point, from per-atom Gasteiger partial charges.
 * Simple 1/r Coulomb-like sum (arbitrary units — fine for color mapping, not physically exact).
 */
private fun potentialAt(
    x: Float,
    y: Float,
    z: Float,
    atoms: List<Atom>,
    rMin: Float = 0.5f,
): Float {
    var v = 0f
    for (a in atoms) {
        val q = a.gasteigerCharge ?: continue
        val dx = x - a.x.toFloat()
        val dy = y - a.y.toFloat()
        val dz = z - a.z.toFloat()
        val r = sqrt(dx * dx + dy * dy + dz * dz)
        v += q / max(r, rMin)
    }
    return v
}

private data class VoxelGrid(
    val dimX: Int,
    val dimY: Int,
    val dimZ: Int,
    val originX: Float,
    val originY: Float,
    val originZ: Float,
    val voxelSize: Float,
    /** Signed field: negative = inside surface, positive = outside. Length = dimX*dimY*dimZ. */
    val signedField: FloatArray,
) {
    fun index(i: Int, j: Int, k: Int): Int = i + j * dimX + k * dimX * dimY

    /** Grid-local coordinate (integer OR fractional, e.g. an interpolated surface-net vertex) -> world space. */
    fun worldX(i: Float) = originX + i * voxelSize
    fun worldY(j: Float) = originY + j * voxelSize
    fun worldZ(k: Float) = originZ + k * voxelSize
}

/**
 * Samples [densityFieldAt] over a padded bounding box of [atoms] and converts it into a
 * signed field (isoLevel - density), which is what the Naive Surface Nets implementation below
 * expects (negative = inside).
 */
private fun buildVoxelGrid(
    atoms: List<Atom>,
    voxelSize: Float = 0.35f,
    padding: Float = 3.0f,
    isoLevel: Float = 0.55f,
): VoxelGrid {
    require(atoms.isNotEmpty()) { "atoms must not be empty" }

    var minX = Float.MAX_VALUE;
    var minY = Float.MAX_VALUE;
    var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE;
    var maxY = -Float.MAX_VALUE;
    var maxZ = -Float.MAX_VALUE
    for (a in atoms) {
        minX = minOf(minX, a.x.toFloat()); maxX = maxOf(maxX, a.x.toFloat())
        minY = minOf(minY, a.y.toFloat()); maxY = maxOf(maxY, a.y.toFloat())
        minZ = minOf(minZ, a.z.toFloat()); maxZ = maxOf(maxZ, a.z.toFloat())
    }
    minX -= padding; minY -= padding; minZ -= padding
    maxX += padding; maxY += padding; maxZ += padding

    val dimX = ((maxX - minX) / voxelSize).toInt() + 2
    val dimY = ((maxY - minY) / voxelSize).toInt() + 2
    val dimZ = ((maxZ - minZ) / voxelSize).toInt() + 2

    val field = FloatArray(dimX * dimY * dimZ)
    var n = 0
    for (k in 0 until dimZ) {
        val wz = minZ + k * voxelSize
        for (j in 0 until dimY) {
            val wy = minY + j * voxelSize
            for (i in 0 until dimX) {
                val wx = minX + i * voxelSize
                val density = densityFieldAt(wx, wy, wz, atoms)
                field[n++] = isoLevel - density // negative = inside
            }
        }
    }

    return VoxelGrid(dimX, dimY, dimZ, minX, minY, minZ, voxelSize, field)
}

/**
 * Port of Mikola Lysenko's public-domain "naive surface nets" algorithm.
 * Produces one vertex per active cell + quads stitched along sign-changing edges.*/

private object SurfaceNetsTables {
    val cubeEdges = IntArray(24)
    val edgeTable = IntArray(256)

    init {
        var k = 0
        for (i in 0 until 8) {
            var j = 1
            while (j <= 4) {
                val p = i xor j
                if (i <= p) {
                    cubeEdges[k++] = i
                    cubeEdges[k++] = p
                }
                j = j shl 1
            }
        }
        for (i in 0 until 256) {
            var em = 0
            var e = 0
            while (e < 24) {
                val a = (i and (1 shl cubeEdges[e])) != 0
                val b = (i and (1 shl cubeEdges[e + 1])) != 0
                if (a != b) em = em or (1 shl (e shr 1))
                e += 2
            }
            edgeTable[i] = em
        }
    }
}

private data class SurfaceMesh(
    val positions: FloatArray, // xyz triples, in the SAME coordinate space as the grid (world space)
    val faces: List<IntArray>, // quads (4 indices each), CCW/CW per mask sign
)

/**
 * Extracts an isosurface (value == 0 in the signed field) via Naive Surface Nets.
 * Returns raw vertices (grid-local, will be converted to world space by caller) and quad faces.
 */
private fun surfaceNets(grid: VoxelGrid): SurfaceMesh {
    val dims = intArrayOf(grid.dimX, grid.dimY, grid.dimZ)
    val data = grid.signedField

    val vertices = ArrayList<Float>() // flattened xyz, grid-local coordinates (not yet * voxelSize)
    val faces = ArrayList<IntArray>()

    val gridVals = FloatArray(8)
    val buffer = IntArray(dims[0] * dims[1] * dims[2]) { -1 }
    val r = intArrayOf(1, dims[0], dims[0] * dims[1])

    var n = 0
    val x = IntArray(3)
    x[2] = 0
    while (x[2] < dims[2] - 1) {
        x[1] = 0
        while (x[1] < dims[1] - 1) {
            x[0] = 0
            while (x[0] < dims[0] - 1) {
                n = grid.index(x[0], x[1], x[2])

                var mask = 0
                var g = 0
                for (kk in 0 until 2) {
                    for (jj in 0 until 2) {
                        for (ii in 0 until 2) {
                            val idx = grid.index(x[0] + ii, x[1] + jj, x[2] + kk)
                            val p = data[idx]
                            gridVals[g] = p
                            if (p < 0f) mask = mask or (1 shl g)
                            g++
                        }
                    }
                }

                if (mask != 0 && mask != 0xff) {
                    val edgeMask = SurfaceNetsTables.edgeTable[mask]
                    var vx = 0f;
                    var vy = 0f;
                    var vz = 0f
                    var eCount = 0

                    for (i in 0 until 12) {
                        if ((edgeMask and (1 shl i)) == 0) continue
                        eCount++

                        val e0 = SurfaceNetsTables.cubeEdges[i * 2]
                        val e1 = SurfaceNetsTables.cubeEdges[i * 2 + 1]
                        val g0 = gridVals[e0]
                        val g1 = gridVals[e1]
                        var t = g0 - g1
                        if (abs(t) <= 1e-6f) continue
                        t = g0 / t

                        var kbit = 1
                        for (j in 0 until 3) {
                            val a = (e0 and kbit) != 0
                            val b = (e1 and kbit) != 0
                            val contrib = if (a != b) {
                                if (a) 1.0f - t else t
                            } else {
                                if (a) 1.0f else 0f
                            }
                            when (j) {
                                0 -> vx += contrib
                                1 -> vy += contrib
                                2 -> vz += contrib
                            }
                            kbit = kbit shl 1
                        }
                    }

                    val s = 1.0f / eCount
                    val px = x[0] + s * vx
                    val py = x[1] + s * vy
                    val pz = x[2] + s * vz

                    buffer[n] = vertices.size / 3
                    vertices.add(px); vertices.add(py); vertices.add(pz)

                    for (i in 0 until 3) {
                        if ((edgeMask and (1 shl i)) == 0) continue
                        val iu = (i + 1) % 3
                        val iv = (i + 2) % 3
                        if (x[iu] == 0 || x[iv] == 0) continue

                        val du = r[iu]
                        val dv = r[iv]

                        val v0 = buffer[n]
                        val v1: Int;
                        val v2: Int;
                        val v3: Int
                        if ((mask and 1) != 0) {
                            v1 = buffer[n - du]
                            v2 = buffer[n - du - dv]
                            v3 = buffer[n - dv]
                        } else {
                            v1 = buffer[n - dv]
                            v2 = buffer[n - du - dv]
                            v3 = buffer[n - du]
                        }
                        if (v0 >= 0 && v1 >= 0 && v2 >= 0 && v3 >= 0) {
                            faces.add(intArrayOf(v0, v1, v2, v3))
                        }
                    }
                }
                x[0]++
            }
            x[1]++
        }
        x[2]++
    }

    return SurfaceMesh(vertices.toFloatArray(), faces)
}

private data class RenderableSurface(
    val positions: FloatArray,
    val normals: FloatArray,
    val indices: IntArray,
    val potentials: FloatArray, // one scalar per vertex, aligned with positions/normals
)

private fun buildEpmSurface(
    atoms: List<Atom>,
    voxelSize: Float = 0.35f,
    padding: Float = 3.0f,
    isoLevel: Float = 0.55f,
): RenderableSurface {
    val grid = buildVoxelGrid(atoms, voxelSize, padding, isoLevel)
    val raw = surfaceNets(grid)

    val vertexCount = raw.positions.size / 3
    val positions = FloatArray(vertexCount * 3)
    val normals = FloatArray(vertexCount * 3)
    val potentials = FloatArray(vertexCount)

    val h = voxelSize * 0.5f
    for (vi in 0 until vertexCount) {
        val gx = raw.positions[vi * 3]
        val gy = raw.positions[vi * 3 + 1]
        val gz = raw.positions[vi * 3 + 2]

        val wx = grid.worldX(gx)
        val wy = grid.worldY(gy)
        val wz = grid.worldZ(gz)

        positions[vi * 3] = wx
        positions[vi * 3 + 1] = wy
        positions[vi * 3 + 2] = wz

        // Central-difference gradient of the density field -> outward normal.
        val dX = densityFieldAt(wx + h, wy, wz, atoms) - densityFieldAt(wx - h, wy, wz, atoms)
        val dY = densityFieldAt(wx, wy + h, wz, atoms) - densityFieldAt(wx, wy - h, wz, atoms)
        val dZ = densityFieldAt(wx, wy, wz + h, atoms) - densityFieldAt(wx, wy, wz - h, atoms)
        // Density decreases outward, so the outward normal is the negative gradient.
        var nx = -dX;
        var ny = -dY;
        var nz = -dZ
        val len = sqrt(nx * nx + ny * ny + nz * nz).let { if (it < 1e-6f) 1f else it }
        nx /= len; ny /= len; nz /= len
        normals[vi * 3] = nx
        normals[vi * 3 + 1] = ny
        normals[vi * 3 + 2] = nz

        potentials[vi] = potentialAt(wx, wy, wz, atoms)
    }

    val indices = ArrayList<Int>(raw.faces.size * 6)
    for (q in raw.faces) {
        // quad (v0, v1, v2, v3) -> two triangles, preserving winding
        indices.add(q[0]); indices.add(q[1]); indices.add(q[2])
        indices.add(q[0]); indices.add(q[2]); indices.add(q[3])
    }

    return RenderableSurface(positions, normals, indices.toIntArray(), potentials)
}

/**
 * Maps potentials to u in [0,1]: u=1 -> red (electron-rich / negative potential),
 * u=0 -> blue (electron-poor / positive potential), u=0.5 -> green/neutral.
 */
private fun potentialsToUvs(
    potentials: FloatArray,
): FloatArray {
    if (potentials.isEmpty()) return FloatArray(0)

    val sortedAbs = potentials.map { abs(it) }.sorted()
    val idx = (sortedAbs.size - 1).coerceIn(0, sortedAbs.size - 1)
    val scale = sortedAbs[idx].coerceAtLeast(1e-4f)

    val uvs = FloatArray(potentials.size * 2)
    for (i in potentials.indices) {
        val ratio = (potentials[i] / scale).coerceIn(-1f, 1f)
        val boosted =
            sign(ratio) * abs(ratio).toDouble().toFloat()
        val t = (0.5f - 0.5f * boosted).coerceIn(0f, 1f)
        uvs[i * 2] = t
        uvs[i * 2 + 1] = 0.5f
    }
    return uvs
}

fun computeEpmMeshData(
    atoms: List<Atom>,
    voxelSize: Float = 0.35f,
    padding: Float = 3.0f,
    isoLevel: Float = 0.55f,
): EPMMeshData {
    val surface = buildEpmSurface(atoms, voxelSize, padding, isoLevel)
    val uvs = potentialsToUvs(surface.potentials)
    return EPMMeshData(
        positions = surface.positions,
        normals = surface.normals,
        uvs = uvs,
        indices = surface.indices,
    )
}