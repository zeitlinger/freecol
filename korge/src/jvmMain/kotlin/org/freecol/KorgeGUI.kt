package org.freecol

import com.soywiz.klock.milliseconds
import com.soywiz.kmem.clamp
import com.soywiz.korev.Key
import com.soywiz.korge.view.Camera
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.camera
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.line
import net.sf.freecol.client.FreeColClient
import net.sf.freecol.client.gui.SwingGUI
import org.freecol.core.Map
import kotlin.io.path.ExperimentalPathApi
import kotlin.math.pow

var globalClient: FreeColClient? = null
val rootFs = localVfs("/home/gzeitlinger/source/freecol")

class KorgeGUI(client: FreeColClient, scale: Float) : SwingGUI(client, scale) {
    init {
        globalClient = client
    }
}

@OptIn(ExperimentalPathApi::class)
suspend fun Stage.start() {


    Map(this, width = 900, height = 512, globalClient!!.freeColServer.game.map.tileList).display()
    println("done")
}


private suspend fun Camera.img(vfs: VfsFile, p: String) {
    image(vfs[p].readBitmap()) {
        //		anchor(.5, .5)
        //		scale(.8)
        position(128, 128)
    }
}

