package org.freecol

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import net.sf.freecol.client.FreeColClient
import net.sf.freecol.client.gui.SwingGUI
import net.sf.freecol.common.model.Tile
import net.sf.freecol.common.model.Unit
import org.freecol.core.FreecolApplication
import kotlin.io.path.ExperimentalPathApi


@OptIn(ExperimentalPathApi::class)
class GdxGUI(client: FreeColClient, scale: Float) : SwingGUI(client, scale) {

    override fun reconnectGUI(active: Unit?, tile: Tile?) {
        super.reconnectGUI(active, tile)
        LwjglApplication(FreecolApplication(freeColClient), LwjglApplicationConfiguration().apply {
            title = "Freecol"
            width = 1580
            height = 900
//            useGL30 = true
            vSyncEnabled = false
            foregroundFPS = 10
            backgroundFPS = -1
        })
    }

    override fun changeView(unit: Unit?) {
//        application.tiledMap.activeUnit = unit
    }
}
