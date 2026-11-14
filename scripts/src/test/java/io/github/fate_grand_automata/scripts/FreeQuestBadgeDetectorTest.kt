package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.modules.FreeQuestBadgeDetector
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FreeQuestBadgeDetectorTest {
    private fun detect(image: BufferedImage) = FreeQuestBadgeDetector.find(
        image.getRGB(0, 0, image.width, image.height, null, 0, image.width), image.width, image.height
    )

    @Test
    fun detectsCounterFromGameScreenshot() {
        val image = ImageIO.read(javaClass.getResource("/free-quests/badge-jp-map.png"))
        for (scale in listOf(0.65, 0.8, 1.0)) {
            for (offset in listOf(0.0, 0.5)) {
                val resized = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
                val g = resized.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.drawImage(image, AffineTransform(scale, 0.0, 0.0, scale, offset, offset), null)
                g.dispose()
                val badges = detect(resized)
                assertEquals(1, badges.size, "scale=$scale offset=$offset")
                val expectedCenter = (41 * scale + offset).toInt()
                assertTrue(badges.single().center.x in expectedCenter - 4..expectedCenter + 4)
                assertTrue(badges.single().center.y in expectedCenter - 4..expectedCenter + 4)
            }
        }
    }

    private fun counter(diameter: Int, digit: String, ring: Boolean = true): BufferedImage {
        val image = BufferedImage(120, 100, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(42, 95, 54)
        g.fillRect(0, 0, 120, 100)
        if (ring) {
            g.color = Color.WHITE
            g.fillOval(40, 20, diameter, diameter)
        }
        g.color = Color(255, 128, 40)
        g.fillOval(42, 22, diameter - 4, diameter - 4)
        g.color = Color.WHITE
        g.font = g.font.deriveFont(diameter * 0.65f)
        g.drawString(digit, 40 + diameter / 3, 20 + diameter * 3 / 4)
        g.dispose()
        return image
    }

    @Test
    fun detectsDifferentCounterSizesAndDigits() {
        for (size in listOf(26, 32, 40, 52)) {
            for (digit in listOf("1", "2", "3")) {
                assertEquals(1, detect(counter(size, digit)).size, "size=$size digit=$digit")
            }
        }
    }

    @Test
    fun rejectsOrangeTerrainWithoutWhiteRing() {
        assertTrue(detect(counter(32, "1", ring = false)).isEmpty())
    }

    @Test
    fun rejectsWhiteRingWithoutNumber() {
        assertTrue(detect(counter(32, "")).isEmpty())
    }
}
