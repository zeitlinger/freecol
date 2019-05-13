package org.freecol.core

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        LwjglApplication(FreecolApplication(), LwjglApplicationConfiguration().apply {
            title = "Freecol"
            width = 480
            height = 800
        })
    }
}
