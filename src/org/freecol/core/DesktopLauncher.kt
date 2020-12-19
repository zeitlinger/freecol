package org.freecol.core

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        LwjglApplication(FreecolApplication(), LwjglApplicationConfiguration().apply {
            title = "Freecol"
            width = 1580
            height = 900
        })
    }
}
