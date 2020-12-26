package org.freecol.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.maps.tiled.TiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import net.sf.freecol.FreeCol
import net.sf.freecol.client.FreeColClient
import net.sf.freecol.common.io.FreeColSavegameFile
import net.sf.freecol.common.io.FreeColTcFile
import net.sf.freecol.server.FreeColServer
import java.io.File
import kotlin.io.path.ExperimentalPathApi


@ExperimentalPathApi
class FreecolApplication : ApplicationAdapter() {
    private lateinit var tiledMap: Map
    private lateinit var camera: OrthographicCamera
    private lateinit var tiledMapRenderer: TiledMapRenderer
    private val server: FreeColServer
    private val client: FreeColClient
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

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

        tiledMap = Map(server.game.map.tileList, client)

        tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap.tiledMap)
        batch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.WHITE

        val gestureDetector = GestureDetector(object : GestureDetector.GestureAdapter() {
            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom)
                return true
            }
        })
        val key = object : InputAdapter() {
            override fun keyUp(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT -> {
                        camera.translate(-32f, 0f)
                    }
                    Input.Keys.RIGHT -> {
                        camera.translate(32f, 0f)
                    }
                    Input.Keys.UP -> {
                        camera.translate(0f, -32f)
                    }
                    Input.Keys.DOWN -> {
                        camera.translate(0f, 32f)
                    }
                    Input.Keys.NUM_1 -> {
                        tiledMap.tiledMap.layers.get(0).isVisible = !tiledMap.tiledMap.layers.get(0).isVisible
                    }
                    Input.Keys.NUM_2 -> {
                        tiledMap.tiledMap.layers.get(1).isVisible = !tiledMap.tiledMap.layers.get(1).isVisible
                    }
                }
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

        batch.projectionMatrix = camera.combined
        batch.begin();
        client.game.map.tileList.mapNotNull { it.settlement }.forEach {
            tiledMap.displaySettlementLabels(it, client.myPlayer, batch, font)
        }
        batch.end();
    }

    override fun dispose() {
        batch.dispose();
        font.dispose();
    }
}
