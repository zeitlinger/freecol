package org.freecol.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import mu.KotlinLogging
import net.sf.freecol.client.gui.ImageLibrary
import net.sf.freecol.common.model.Direction
import net.sf.freecol.common.model.LostCityRumour
import net.sf.freecol.common.model.Resource
import net.sf.freecol.common.model.Tile
import net.sf.freecol.common.model.TileImprovement
import net.sf.freecol.common.model.TileItem
import net.sf.freecol.common.resources.ResourceManager
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.toPath

data class LayerId(val key: String) {
    init {
        all.add(key)
    }

    companion object {
        val all = mutableListOf<String>()
    }
}


val beachEdge = LayerId("image.tile.model.tile.beach.edge")
val beachCorner = LayerId("image.tile.model.tile.beach.corner")
const val border = "border"
val base = LayerId("base")
const val improvement = "improvement"
const val unknown = "unknown"


@ExperimentalPathApi
class Map(tiles: List<Tile>) {

    private val maxY = tiles.maxOf { it.y } / 2
    val tiledMap = TiledMap()

    private val logger = KotlinLogging.logger {}

    init {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        addLayer(w, h, base)
        addLayer(w, h, beachEdge)
        addLayer(w, h, beachCorner)
        for (i in 0 until Direction.values().size * 2) {
            addLayer(w, h, LayerId(border + i))
        }
        for (i in Direction.values().indices) {
            addLayer(w, h, LayerId(unknown + i))
        }
        for (i in 0..10) {
            addLayer(w, h, LayerId(improvement + i))
        }

        require(tiledMap.layers.map { it.name }.containsAll(LayerId.all))

        tiles.forEach { tile ->
            display(tile)
        }
    }

    fun display(tile: Tile) {
        val tileType = tile.type
        addCell(tile, base, ImageLibrary.getTerrainImageKey(tileType, tile.x, tile.y))

        displayTileWithBeachAndBorder(tile)
        displayUnknownTileBorder(tile)

        //todo: fog of war
        // Apply fog of war to flat parts of all tiles
//        val fow: RescaleOp? = null

        val overlayKey = ImageLibrary.getOverlayImageInternalKey(tile.type, tile.id, ImageLibrary.TILE_OVERLAY_SIZE)
//        val rop = if (player == null || player.canSee(t)) null else fow
        displayTileItems(tile, overlayKey)
    }

    private fun displayTileWithBeachAndBorder(tile: Tile) {
        val x = tile.x
        val y = tile.y

        if (!tile.isExplored) {
            return
        }
        val style = tile.style
        if (!tile.isLand && style > 0) {
            val edgeStyle = style shr 4
            if (edgeStyle > 0) {
                addCell(tile, beachEdge, ImageLibrary.getEvenImageKey(edgeStyle, x, y, beachEdge.key))
            }
            val cornerStyle = style and 15
            if (cornerStyle > 0) {
                addCell(tile, beachCorner, ImageLibrary.getEvenImageKey(cornerStyle, x, y, beachCorner.key))
            }
        }

        val imageBorders = Direction.values().flatMap { imageBorders(tile, it) }
        imageBorders.sortedByDescending { it.first }.forEachIndexed { index, b ->
            addCell(tile, LayerId(border + index), b.second)
        }
    }

    private fun displayUnknownTileBorder(tile: Tile) {
        if (!tile.isExplored) return
        for (direction in Direction.values()) {
            val borderingTile = tile.getNeighbourOrNull(direction)
            if (borderingTile != null && !borderingTile.isExplored) {
                addCell(
                    tile,
                    LayerId(unknown + direction.ordinal),
                    ImageLibrary.getBorderImageKey(null, direction, tile.x, tile.y),
                )
            }
        }
    }

    private fun displayTileItems(tile: Tile, overlayImage: String?) {
        var index = 0
        val items = tile.completeItems.sortedBy { it.zIndex }

        fun layerId() = LayerId(improvement + index).also { index++ }

        fun display(pred: (TileItem) -> Boolean) {
            items.filter(pred).forEach {
                displayTileItem(tile, it, layerId())
            }
        }
        display { it.zIndex < Tile.OVERLAY_ZINDEX }

        // Tile Overlays (eg. hills and mountains)
        if (overlayImage != null) {
            addCell(tile, layerId(), overlayImage)
        }

        display { it.zIndex in Tile.OVERLAY_ZINDEX..Tile.FOREST_ZINDEX }

        if (tile.isForested) {
            addCell(
                tile,
                layerId(),
                ImageLibrary.getForestImageKey(tile.type, tile.riverStyle, ImageLibrary.TILE_FOREST_SIZE)
            )
        }

        display { it.zIndex > Tile.FOREST_ZINDEX }
    }

    private fun displayTileItem(tile: Tile, item: TileItem, layerId: LayerId) {
        when (item) {
            is TileImprovement -> {
                when {
                    item.isRoad -> {
                        addFile(tile, layerId, getRoad(tile))
                    }
                    item.isRiver -> {
                        val style = item.style
                        if (style == null) { // This is all too common with broken maps
                            logger.error("Null river style for $tile")
                        } else {
                            addCell(tile, layerId, ImageLibrary.getRiverStyleKey(style.string))
                        }
                    }
                    else -> {
                        val key = "image.tile." + item.type.id
                        if (ResourceManager.getImageResource(key, false) != null) {
                            addCell(tile, layerId, key)
                        }
                    }
                }
            }
            is LostCityRumour -> addCell(tile, layerId, ImageLibrary.LOST_CITY_RUMOUR, center = true)
            is Resource -> addCell(tile, layerId, ImageLibrary.getResourceTypeKey(item.type), center = true)
        }
    }

    private fun addCell(
        tile: Tile,
        layerId: LayerId,
        imageKey: String,
        center: Boolean = false,
    ) {
        val resource = ResourceManager.getImageResource(imageKey, true)!!
        val file = resource.resourceLocator.toPath().toFile()
        addFile(tile, layerId, file, center)
    }

    private fun addFile(
        tile: Tile,
        layerId: LayerId,
        file: File,
        center: Boolean = false
    ) {
        addTexture(tile, Texture(FileHandle(file)), layerId, center)
    }

    private fun addTexture(
        tile: Tile,
        texture: Texture,
        layerId: LayerId,
        center: Boolean = false,
    ) {
        val size = ImageLibrary.TILE_SIZE
        var posX = tile.x * 2
        val posY = (maxY - tile.y / 2)
        var offsetX = 0f
        var offsetY = 0f

        if (tile.y.rem(2) == 1) {
            posX++
            offsetY -= size.height / 2
        }

        if (center) {
            offsetX += (size.width - texture.width) / 2
            offsetY += (size.height - texture.height) / 2
        }

        val cell = TiledMapTileLayer.Cell().apply {
            this.tile = StaticTiledMapTile(TextureRegion(texture)).apply {
                this.offsetX = offsetX
                this.offsetY = offsetY
            }
        }

        (tiledMap.layers.get(layerId.key) as TiledMapTileLayer).setCell(posX, posY, cell)
    }

    private fun addLayer(
        w: Float,
        h: Float,
        name: LayerId,
    ) {
        val tileWidth = ImageLibrary.TILE_SIZE.width / 2
        val layer = TiledMapTileLayer(w.toInt(), h.toInt(), tileWidth, tileWidth)
        layer.name = name.key
        tiledMap.layers.add(layer)
    }
}

fun getRoad(tile: Tile): File {
    val map = tile.map
    val x = tile.x
    val y = tile.y

    val num: Int = Direction.allDirections
        .filter { d -> map.getTile(d.step(x, y))?.road?.isComplete ?: false }
        .fold(0) { acc, direction -> (1 shl direction.ordinal) + acc }
    return File("data/rules/classic/resources/images/road/road$num.png")
}

private fun imageBorders(
    tile: Tile,
    direction: Direction,
): List<Pair<Int, String>> {
    val tileType = tile.type
    val x = tile.x
    val y = tile.y

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
            } else {
                listOf(pair)
            }
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

