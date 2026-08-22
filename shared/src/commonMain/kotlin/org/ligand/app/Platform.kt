package org.ligand.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform