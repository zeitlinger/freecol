package org.freecol.core

import net.sf.freecol.client.gui.ImageLibrary
import net.sf.freecol.client.gui.RoadPainter
import net.sf.freecol.common.model.Direction
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow


fun main() {
    RoadGenerator.generate()
}

object RoadGenerator {
    val dest = "data/rules/classic/resources/images/road"

    fun generate() {
        val size = ImageLibrary.TILE_SIZE
        val roadPainter = RoadPainter(size)

        val directions = Direction.values()

        for (i in 0 until 2f.pow(directions.size).toInt()) {
            val dirs = directions.filterIndexed { index, _ ->
                val mask = 1 shl index
                i and mask == mask
            }

            val image = BufferedImage(
                size.width, size.height,
                BufferedImage.TYPE_4BYTE_ABGR
            )

            val g2d = image.createGraphics()
            roadPainter.displayRoad(g2d, dirs)
            g2d.dispose()

            ImageIO.write(image, "png", File("$dest/road$i.png"))
        }
    }
}
