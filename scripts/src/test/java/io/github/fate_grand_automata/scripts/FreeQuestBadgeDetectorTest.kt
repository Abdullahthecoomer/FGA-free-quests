package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.modules.FreeQuestBadgeDetector
import java.awt.Color
import java.awt.image.BufferedImage
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
        val badges = detect(image)
        assertEquals(1, badges.size)
        assertTrue(badges.single().center.x in 36..46)
        assertTrue(badges.single().center.y in 36..46)
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
