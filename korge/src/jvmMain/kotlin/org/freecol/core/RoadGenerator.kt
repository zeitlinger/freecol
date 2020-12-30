//package org.freecol.core
//
//import net.sf.freecol.client.gui.ImageLibrary
//import net.sf.freecol.common.model.Direction
//import net.sf.freecol.common.resources.ColorResource
//import java.awt.BasicStroke
//import java.awt.Dimension
//import java.awt.Graphics2D
//import java.awt.RenderingHints
//import java.awt.Stroke
//import java.awt.geom.GeneralPath
//import java.awt.geom.Point2D
//import java.awt.image.BufferedImage
//import java.io.File
//import java.net.URI
//import java.net.URISyntaxException
//import java.util.EnumMap
//import javax.imageio.ImageIO
//import kotlin.math.pow
//
//
//fun main() {
//    RoadGenerator.generate()
//}
//
//object RoadGenerator {
//    val dest = "data/rules/classic/resources/images/road"
//
//    fun generate() {
//        val size = ImageLibrary.TILE_SIZE
//        val roadPainter = RoadPainter(size)
//
//        val directions = Direction.values()
//
//        for (i in 0 until 2f.pow(directions.size).toInt()) {
//            val dirs = directions.filterIndexed { index, _ ->
//                val mask = 1 shl index
//                i and mask == mask
//            }
//
//            val image = BufferedImage(
//                size.width, size.height,
//                BufferedImage.TYPE_4BYTE_ABGR
//            )
//
//            val g2d = image.createGraphics()
//            roadPainter.displayRoad(g2d, dirs)
//            g2d.dispose()
//
//            ImageIO.write(image, "png", File("$dest/road$i.png"))
//        }
//    }
//}
//
///**
// * This class is responsible for drawing the Roads on a tile.
// */
//class RoadPainter(tileSize: Dimension) {
//    /** Helper variables for displaying the map.  */
//    private val tileHeight = tileSize.height
//    private val tileWidth = tileSize.width
//    private val halfHeight = tileHeight / 2
//    private val halfWidth = tileWidth / 2
//    private val corners = EnumMap<Direction, Point2D.Float>(
//        Direction::class.java
//    )
//    private val prohibitedRoads = EnumMap<Direction, List<Direction>>(
//        Direction::class.java
//    )
//    private var roadStroke: Stroke = BasicStroke(2f)
//    fun displayRoad(g: Graphics2D, directions: List<Direction>) {
//        val oldColor = g.color
//        g.color = try {
//            ColorResource(URI("urn:color:0x804000")).color
//        } catch (e: URISyntaxException) {
//            throw RuntimeException(e)
//        }
//        g.stroke = roadStroke
//        g.setRenderingHint(
//            RenderingHints.KEY_ANTIALIASING,
//            RenderingHints.VALUE_ANTIALIAS_ON
//        )
//        val points = directions.map { corners[it] }
//        val path = GeneralPath()
//        when (points.size) {
//            0 -> {
//                path.moveTo(0.35f * tileWidth, 0.35f * tileHeight)
//                path.lineTo(0.65f * tileWidth, 0.65f * tileHeight)
//                path.moveTo(0.35f * tileWidth, 0.65f * tileHeight)
//                path.lineTo(0.65f * tileWidth, 0.35f * tileHeight)
//            }
//            1 -> {
//                val p = points[0]!!
//                path.moveTo(halfWidth.toFloat(), halfHeight.toFloat())
//                path.lineTo(p.getX(), p.getY())
//            }
//            2 -> {
//                val p = points[0]!!
//                val p2 = points[1]!!
//                path.moveTo(p.getX(), p.getY())
//                path.quadTo(
//                    halfWidth.toDouble(), halfHeight.toDouble(),
//                    p2.getX(), p2.getY()
//                )
//            }
//            3, 4 -> {
//                var pen = directions[directions.size - 1]
//                var pt = corners[pen]!!
//                path.moveTo(pt.x, pt.y)
//                for (d in directions) {
//                    pt = corners[d]!!
//                    if (prohibitedRoads[pen]!!.contains(d)) {
//                        path.moveTo(pt.getX(), pt.getY())
//                    } else {
//                        path.quadTo(halfWidth.toDouble(), halfHeight.toDouble(), pt!!.getX(), pt.getY())
//                    }
//                    pen = d
//                }
//            }
//            else -> for (p in points) {
//                path.moveTo(halfWidth.toFloat(), halfHeight.toFloat())
//                path.lineTo(p!!.x, p.y)
//            }
//        }
//        g.draw(path)
//        g.color = oldColor
//        g.setRenderingHint(
//            RenderingHints.KEY_ANTIALIASING,
//            RenderingHints.VALUE_ANTIALIAS_OFF
//        )
//    }
//
//    /**
//     * Create a new road painter for a given tile size.
//     *
//     * @param tileSize The tile size as a `Dimension`.
//     */
//    init {
//        val dy = tileHeight / 16
//        roadStroke = BasicStroke(dy / 2.0f)
//
//        // Corners
//        corners[Direction.N] = Point2D.Float(halfWidth.toFloat(), 0f)
//        corners[Direction.NE] = Point2D.Float(
//            0.75f * tileWidth,
//            0.25f * tileHeight
//        )
//        corners[Direction.E] = Point2D.Float(
//            tileWidth.toFloat(),
//            halfHeight.toFloat()
//        )
//        corners[Direction.SE] = Point2D.Float(
//            0.75f * tileWidth,
//            0.75f * tileHeight
//        )
//        corners[Direction.S] = Point2D.Float(
//            halfWidth.toFloat(),
//            tileHeight.toFloat()
//        )
//        corners[Direction.SW] = Point2D.Float(
//            0.25f * tileWidth,
//            0.75f * tileHeight
//        )
//        corners[Direction.W] = Point2D.Float(0f, halfHeight.toFloat())
//        corners[Direction.NW] = Point2D.Float(
//            0.25f * tileWidth,
//            0.25f * tileHeight
//        )
//
//        // Road pairs to skip drawing when doing 3 or 4 exit point tiles.
//        // Don't put more than two directions in each list, otherwise
//        // a 3-point tile may not draw any roads at all!
//        prohibitedRoads[Direction.N] = listOf(Direction.NW, Direction.NE)
//        prohibitedRoads[Direction.NE] = listOf(Direction.N, Direction.E)
//        prohibitedRoads[Direction.E] = listOf(Direction.NE, Direction.SE)
//        prohibitedRoads[Direction.SE] = listOf(Direction.E, Direction.S)
//        prohibitedRoads[Direction.S] = listOf(Direction.SE, Direction.SW)
//        prohibitedRoads[Direction.SW] = listOf(Direction.S, Direction.W)
//        prohibitedRoads[Direction.W] = listOf(Direction.SW, Direction.NW)
//        prohibitedRoads[Direction.NW] = listOf(Direction.W, Direction.N)
//    }
//}
