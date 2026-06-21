package org.liganddraw.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform