package org.freecol.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import net.sf.freecol.client.gui.ImageLibrary
import net.sf.freecol.common.model.Direction
import net.sf.freecol.common.model.Tile
import net.sf.freecol.common.resources.ResourceManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.toPath

const val beachEdge = "image.tile.model.tile.beach.edge"
const val beachCorner = "image.tile.model.tile.beach.corner"
const val border = "border"
const val riverLayer = "river"


@ExperimentalPathApi
class Map(val tiledMap: TiledMap, private val maxY: Int) {

    init {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        addLayer(w, h, "base")
        addLayer(w, h, beachEdge)
        addLayer(w, h, beachCorner)
        addLayer(w, h, riverLayer)
        for (i in 0 until Direction.values().size * 2) {
            addLayer(w, h, border + i)
        }
    }

    fun displayTileWithBeachAndBorder(tile: Tile) {
        val x = tile.x;
        val y = tile.y;

        if (!tile.isExplored) return;
        val style = tile.style
        if (!tile.isLand && style > 0) {
            val edgeStyle = style shr 4
            if (edgeStyle > 0) {
                addCell(ImageLibrary.getEvenImageKey(edgeStyle, x, y, beachEdge), beachEdge, x, y)
            }
            val cornerStyle = style and 15;
            if (cornerStyle > 0) {
                addCell(ImageLibrary.getEvenImageKey(cornerStyle, x, y, beachCorner), beachCorner, x, y)
            }
        }

        val imageBorders = Direction.values().flatMap { imageBorders(tile, it) }
        imageBorders.sortedByDescending { it.first }.forEachIndexed { index, b ->
            addCell(b.second, border + index, x, y)
        }
    }

    @ExperimentalPathApi
    fun addCell(
        imageKey: String,
        layerId: String,
        x: Int,
        y: Int
    ) {
        val posx = x * 4 + if (y.rem(2) == 1) 2 else 0
        val posy = maxY - y

        val resource = ResourceManager.getImageResource(imageKey, true)!!

        val cell = TiledMapTileLayer.Cell()
        val file = resource.resourceLocator.toPath().toFile()
        cell.tile =
            StaticTiledMapTile(TextureRegion(Texture(FileHandle(file))))
        (tiledMap.layers.get(layerId) as TiledMapTileLayer).setCell(posx, posy, cell)
    }

    fun addLayer(
        w: Float,
        h: Float,
        name: String,
    ) {
        val tileWidth = 32
        val layer = TiledMapTileLayer(w.toInt(), h.toInt(), tileWidth, tileWidth)
        layer.name = name
        tiledMap.layers.add(layer)
    }
}

private fun imageBorders(
    tile: Tile,
    direction: Direction,
): List<Pair<Int, String>> {
    val tileType = tile.type
    val x = tile.x;
    val y = tile.y;

    val borderingTile = tile.getNeighbourOrNull(direction)
    if (borderingTile == null || !borderingTile.isExplored) return emptyList()

    val borderingTileType = borderingTile.type
    if (borderingTileType == tileType) return emptyList()

    return when {
        !tile.isLand && borderingTile.isLand -> {
            // If there is a Coast image (eg. beach) defined, use
            // it, otherwise skip Draw the grass from the
            // neighboring tile, spilling over on the side of this tile
            val pair = borderingTileType.index to ImageLibrary.getBorderImageKey(borderingTileType, direction, x, y)
            val river = borderingTile.river
            if (river != null) {
                val dr = direction.reverseDirection
                val magnitude = river.getRiverConnection(dr)
                if (magnitude > 0) {
                    listOf(pair, -1 to ImageLibrary.getRiverMouthImageKey(direction, magnitude))
                } else {
                    listOf(pair)
                }
            }
            listOf(pair)
        }
        !tile.isLand || borderingTile.isLand -> {
            val bTIndex = borderingTileType.index
            val tik1 = ImageLibrary
                .getTerrainImageKey(tileType, 0, 0)
            val tik2 = ImageLibrary
                .getTerrainImageKey(borderingTileType, 0, 0)
            if (bTIndex < tileType.index && tik1 != tik2) {
                // Draw land terrain with bordering land type, or
                // ocean/high seas limit, if the tiles do not
                // share same graphics (ocean & great river)

                listOf(bTIndex to ImageLibrary.getBorderImageKey(borderingTileType, direction, x, y))
            } else {
                emptyList()
            }
        }
        else -> emptyList()
    }
}

