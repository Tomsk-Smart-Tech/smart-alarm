package org.tst.smartalarm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform