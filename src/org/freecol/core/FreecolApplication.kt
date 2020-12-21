package org.freecol.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import net.sf.freecol.FreeCol
import net.sf.freecol.client.FreeColClient
import net.sf.freecol.client.gui.ImageLibrary
import net.sf.freecol.common.io.FreeColSavegameFile
import net.sf.freecol.common.io.FreeColTcFile
import net.sf.freecol.common.resources.ImageCache
import net.sf.freecol.server.FreeColServer
import java.io.File
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class FreecolApplication : ApplicationAdapter() {
    private lateinit var camera: OrthographicCamera
    private lateinit var tiledMapRenderer: TiledMapRenderer
    private val server: FreeColServer
    private val client: FreeColClient

    init {
        FreeCol.setHeadless(true)

        FreeColTcFile.loadTCs()
        val specification = FreeColTcFile.getFreeColTcFile("freecol").specification
        val filename =
            "/home/gzeitlinger/.local/share/freecol/save/autosave/Autosave-866aa34f_Spanish_1643_2_Autumn.fsg"
        val out = File(filename)
        val savegame = FreeColSavegameFile(out)
        server = FreeColServer(savegame, specification, FreeCol.getServerPort(), "mapTransformer")

        client = FreeColClient(
            null, null, FreeCol.GUI_SCALE_DEFAULT, null,
            null, false, false, out, specification
        )
    }


    override fun create() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        camera = OrthographicCamera()
        camera.setToOrtho(false, w, h)
        camera.update()

        val tiledMap = Map(server.game.map.tileList)

        val imageCache = ImageCache()
        val fixedImageLibrary = ImageLibrary(1f, imageCache)

        tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap.tiledMap)

        val gestureDetector = GestureDetector(object : GestureDetector.GestureAdapter() {
            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom)
                return true
            }
        })
        val key = object : InputAdapter() {
            override fun keyUp(keycode: Int): Boolean {
                if (keycode == Input.Keys.LEFT)
                    camera.translate(-32f, 0f)
                if (keycode == Input.Keys.RIGHT)
                    camera.translate(32f, 0f)
                if (keycode == Input.Keys.UP)
                    camera.translate(0f, -32f)
                if (keycode == Input.Keys.DOWN)
                    camera.translate(0f, 32f)
                if (keycode == Input.Keys.NUM_1)
                    tiledMap.tiledMap.layers.get(0).isVisible = !tiledMap.tiledMap.layers.get(0).isVisible
                if (keycode == Input.Keys.NUM_2)
                    tiledMap.tiledMap.layers.get(1).isVisible = !tiledMap.tiledMap.layers.get(1).isVisible
                return false
            }

            override fun scrolled(amount: Int): Boolean {
                camera.zoom = (camera.zoom + amount).coerceAtLeast(1f)
                return true
            }
        }
        Gdx.input.inputProcessor = InputMultiplexer(gestureDetector, key)
    }

    override fun render() {
        Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()
    }


}
