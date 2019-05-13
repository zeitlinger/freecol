package org.freecol.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapRenderer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile

class FreecolApplication : ApplicationAdapter() {
    private lateinit var camera: OrthographicCamera
    private lateinit var tiledMapRenderer: TiledMapRenderer

    override fun create() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        camera = OrthographicCamera()
        camera.setToOrtho(false, w, h)
        camera.update()

        val tiledMap = TiledMap()

        val width = 500
        val height = 500
        val tileWidth = 32
        val tileHeight = 32
        val layer1 = TiledMapTileLayer(width, height, tileWidth, tileHeight)
        val cell = TiledMapTileLayer.Cell()

        cell.tile = StaticTiledMapTile(TextureRegion(Texture("data/rules/classic/resources/images/terrain/grassland/center0.png")))
        layer1.setCell(0, 0, cell)
        layer1.setCell(2, 1, cell)

        tiledMap.layers.add(layer1)

        tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap)
        Gdx.input.inputProcessor = object : InputAdapter() {
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
                    tiledMap.layers.get(0).isVisible = !tiledMap.layers.get(0).isVisible
                if (keycode == Input.Keys.NUM_2)
                    tiledMap.layers.get(1).isVisible = !tiledMap.layers.get(1).isVisible
                return false
            }
        }
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
