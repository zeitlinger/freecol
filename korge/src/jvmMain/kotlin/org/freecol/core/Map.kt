package org.freecol.core


import com.soywiz.korge.Korge
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.camera
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBAf
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.line
import net.sf.freecol.client.gui.ImageLibrary
import net.sf.freecol.client.gui.MapViewer
import net.sf.freecol.common.model.Direction
import net.sf.freecol.common.model.LostCityRumour
import net.sf.freecol.common.model.Resource
import net.sf.freecol.common.model.Tile
import net.sf.freecol.common.model.TileImprovement
import net.sf.freecol.common.model.TileItem
import net.sf.freecol.common.model.Unit
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
const val borderId = "border"
val baseId = LayerId("base")
val settlementId = LayerId("settlement")
val unitLayer = LayerId("unit")
const val improvement = "improvement"
const val unknown = "unknown"

val tileWidth = ImageLibrary.TILE_SIZE.width / 2
val textureCache = mutableMapOf<File, Bitmap>()

@ExperimentalPathApi
class Map(val stage: Stage, val width: Int, val height: Int, val tiles: List<Tile>) {

    private val maxY = tiles.maxOf { it.y } / 2
    val rootFs = localVfs("/home/gzeitlinger/source/freecol")

    val activeUnit: Unit? = null

//    private val logger = KotlinLogging.logger {}

    suspend fun display() {
        val w = width.toFloat()
        val h = height.toFloat()

        addLayer(w, h, baseId)
        addLayer(w, h, beachEdge)
        addLayer(w, h, beachCorner)
        for (i in 0 until Direction.values().size * 2) {
            addLayer(w, h, LayerId(borderId + i))
        }
        for (i in Direction.values().indices) {
            addLayer(w, h, LayerId(unknown + i))
        }
        for (i in 0..10) {
            addLayer(w, h, LayerId(improvement + i))
        }
        addLayer(w, h, settlementId)
        addLayer(w, h, unitLayer)

//        require(tiledMap.layers.map { it.name }.containsAll(LayerId.all))

        stage.fixedSizeContainer(width = width, height = height, clip = true) {
//            camera {
                tiles.forEach { tile -> display(tile) }
//            }
        }
    }

    suspend fun Container.display(tile: Tile) {
        val tileType = tile.type
        addImageResource(tile, baseId, ImageLibrary.getTerrainImageKey(tileType, tile.x, tile.y))

        displayTileWithBeachAndBorder(tile)
        displayUnknownTileBorder(tile)

        //todo: fog of war
        // Apply fog of war to flat parts of all tiles
//        val fow: RescaleOp? = null

        val overlayKey = ImageLibrary.getOverlayImageInternalKey(tile.type, tile.id, ImageLibrary.TILE_OVERLAY_SIZE)
//        val rop = if (player == null || player.canSee(t)) null else fow
        displayTileItems(tile, overlayKey)
        displaySettlement(tile)
        findUnitInFront(tile)?.let { displayUnit(tile, it) }
    }

    private suspend fun Container.displayTileWithBeachAndBorder(tile: Tile) {
        val x = tile.x
        val y = tile.y

        if (!tile.isExplored) {
            return
        }
        val style = tile.style
        if (!tile.isLand && style > 0) {
            val edgeStyle = style shr 4
            if (edgeStyle > 0) {
                addImageResource(tile, beachEdge, ImageLibrary.getEvenImageKey(edgeStyle, x, y, beachEdge.key))
            }
            val cornerStyle = style and 15
            if (cornerStyle > 0) {
                addImageResource(tile, beachCorner, ImageLibrary.getEvenImageKey(cornerStyle, x, y, beachCorner.key))
            }
        }

        val imageBorders = Direction.values().flatMap { imageBorders(tile, it) }
        imageBorders.sortedByDescending { it.first }.forEachIndexed { index, b ->
            addImageResource(tile, LayerId(borderId + index), b.second)
        }
    }

    private suspend fun Container.displayUnknownTileBorder(tile: Tile) {
        if (!tile.isExplored) return
        for (direction in Direction.values()) {
            val borderingTile = tile.getNeighbourOrNull(direction)
            if (borderingTile != null && !borderingTile.isExplored) {
                addImageResource(
                    tile,
                    LayerId(unknown + direction.ordinal),
                    ImageLibrary.getBorderImageKey(null, direction, tile.x, tile.y),
                )
            }
        }
    }

    private suspend fun Container.displayTileItems(tile: Tile, overlayImage: String?) {
        var index = 0
        val items = tile.completeItems.sortedBy { it.zIndex }

        fun layerId() = LayerId(improvement + index).also { index++ }

        suspend fun display(pred: (TileItem) -> Boolean) {
            items.filter(pred).forEach {
                displayTileItem(tile, it, layerId())
            }
        }
        display { it.zIndex < Tile.OVERLAY_ZINDEX }

        // Tile Overlays (eg. hills and mountains)
        if (overlayImage != null) {
            addImageResource(tile, layerId(), overlayImage)
        }

        display { it.zIndex in Tile.OVERLAY_ZINDEX..Tile.FOREST_ZINDEX }

        if (tile.isForested) {
            addImageResource(
                tile,
                layerId(),
                ImageLibrary.getForestImageKey(tile.type, tile.riverStyle, ImageLibrary.TILE_FOREST_SIZE)
            )
        }

        display { it.zIndex > Tile.FOREST_ZINDEX }
    }

    private suspend fun Container.displayTileItem(tile: Tile, item: TileItem, layerId: LayerId) {
        when (item) {
            is TileImprovement -> {
                when {
                    item.isRoad -> {
                        addFile(tile, layerId, getRoad(tile))
                    }
                    item.isRiver -> {
                        val style = item.style
                        if (style == null) { // This is all too common with broken maps

                            Korge.logger.error { "Null river style for $tile" }
                        } else {
                            addImageResource(tile, layerId, ImageLibrary.getRiverStyleKey(style.string))
                        }
                    }
                    else -> {
                        val key = "image.tile." + item.type.id
                        if (ResourceManager.getImageResource(key, false) != null) {
                            addImageResource(tile, layerId, key)
                        }
                    }
                }
            }
            is LostCityRumour -> addImageResource(tile, layerId, ImageLibrary.LOST_CITY_RUMOUR, center = true)
            is Resource -> addImageResource(tile, layerId, ImageLibrary.getResourceTypeKey(item.type), center = true)
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Settlements and Lost
     * City Rumours will be shown.
     *
     * @param tile The Tile to draw.
     */
    private suspend fun Container.displaySettlement(tile: Tile) {
        val settlement = tile.settlement ?: return

        val key = ImageLibrary.getSettlementKey(settlement)
        addImageResource(tile, settlementId, key, center = true)
    }

    /**
     * Gets the unit that should be displayed on the given tile.
     *
     * Used mostly by displayMap, but public for SwingGUI.clickAt.
     *
     * @param unitTile The `Tile` to check.
     * @return The `Unit` to display or null if none found.
     */
    fun findUnitInFront(unitTile: Tile?): Unit? {
        return if (unitTile == null || unitTile.isEmpty) {
            null
        } else if (this.activeUnit != null && this.activeUnit.tile === unitTile) {
            this.activeUnit
        } else if (unitTile.hasSettlement()) {
            null
        } else if (this.activeUnit != null && this.activeUnit.isOffensiveUnit) {
            unitTile.getDefendingUnit(this.activeUnit)
        } else {
            // Find the unit with the most moves left, preferring active units.
            unitTile.unitList.maxByOrNull { u ->
                u.movesLeft + if (u.state == Unit.UnitState.ACTIVE) 10000 else 0
            }
        }
    }

    /**
     * Displays the given Unit onto the given Graphics2D object at the
     * location specified by the coordinates.
     *
     * @param unit The Unit to draw.
     */
    private fun displayUnit(tile: Tile, unit: Unit) {
        val key =
            ImageLibrary.getUnitTypeImageKey(unit.type, unit.owner, unit.role.id, unit.hasNativeEthnicity())
//        val texture = texture(imageFile(key))
//        addTexture(tile, texture, unitLayer, offset = calculateUnitImagePositionInTile(texture))
    }

    private fun calculateUnitImagePositionInTile(texture: Bitmap): Point {
        val tile = ImageLibrary.TILE_SIZE

        val unitX = (tile.width - texture.width) / 2
        val unitY = (tile.height - texture.height) / 2 + MapViewer.UNIT_OFFSET
        return Point(unitX.toFloat(), unitY.toFloat())
    }

//    fun displaySettlementLabels(
//        settlement: Settlement,
//        player: Player,
//        batch: SpriteBatch,
//        font: BitmapFont,
//    ) {
//        var text = Messages.message(settlement.getLocationLabelFor(player)) ?: return
//        if (settlement is Colony) {
//            if (player.owns(settlement)) {
//                val bonusProduction = settlement.productionBonus
//                val bonus =
//                    if (bonusProduction > 0) "+$bonusProduction" else bonusProduction.toString()
//
//                text += " (${settlement.apparentUnitCount}, $bonus/${settlement.unitsToAdd})"
//
//                val buildable = settlement.currentlyBuilding
//                if (buildable != null) {
//                    val turnsText = Turn.getTurnsText(settlement.getTurnsToComplete(buildable))
//                    text += "\n${Messages.getName(buildable)} $turnsText"
//                }
//            } else {
//                text += " (${settlement.apparentUnitCount})"
//            }
//        }
//        val l = GlyphLayout(font, text, 0, text.length, font.color, 0f, Align.center, false, null)
//
//        val origin = getTileOrigin(settlement.tile).absolute
//
//        font.draw(
//            batch, l, origin.x + (ImageLibrary.TILE_SIZE.width) / 2,
//            origin.y + (ImageLibrary.TILE_SIZE.height + l.height) / 2
//        )
//    }

    private suspend fun Container.addImageResource(
        tile: Tile,
        layerId: LayerId,
        imageKey: String,
        center: Boolean = false,
    ) {
        addFile(tile, layerId, imageFile(imageKey), center)
    }

    private fun imageFile(imageKey: String): File {
        val resource = ResourceManager.getImageResource(imageKey, true)!!
        return resource.resourceLocator.toPath().toFile()
    }

    private suspend fun getBitmap(p: File): Bitmap {
        textureCache[p]?.let { return it }
        return rootFs[p.path].readBitmap().also { textureCache[p] = it }
    }

    private suspend fun Container.addFile(
        tile: Tile,
        layerId: LayerId,
        file: File,
        center: Boolean = false,
    ) {
        addTexture(tile, getBitmap(file), layerId, center)
    }

    data class TileOrigin(val cellX: Int, val cellY: Int, val offset: Point) {
        val absolute: Point = Point(cellX.toFloat() * tileWidth, cellY.toFloat() * tileWidth) + offset
    }

    private fun getTileOrigin(
        tile: Tile,
        offset: Point? = null
    ): TileOrigin {
        val size = ImageLibrary.TILE_SIZE
        var posX = tile.x * 2
        val posY = (maxY - tile.y / 2)
        val o = Point()

        if (tile.y.rem(2) == 1) {
            posX++
            o.y -= size.height / 2
        }

        return TileOrigin(posX, posY, offset?.let { o.add(offset) } ?: o)
    }

    private fun Container.addTexture(
        tile: Tile,
        texture: Bitmap,
        layerId: LayerId,
        center: Boolean = false,
        offset: Point? = null,
    ) {
        val p = if (center) {
            val size = ImageLibrary.TILE_SIZE

            Point(
                (size.width - texture.width).toFloat() / 2,
                (size.height - texture.height).toFloat() / 2
            )
        } else {
            offset
        }

        val origin = getTileOrigin(tile, p).absolute

        image(texture) {
            position(origin)
        }
    }

    private fun addLayer(
        w: Float,
        h: Float,
        name: LayerId,
    ) {
//        val layer = TiledMapTileLayer(w.toInt(), h.toInt(), tileWidth, tileWidth)
//        layer.name = name.key
//        tiledMap.layers.add(layer)
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


fun java.awt.Color.toColor() =
    RGBAf(red.toFloat(), green.toFloat(), blue.toFloat(), alpha.toFloat())

