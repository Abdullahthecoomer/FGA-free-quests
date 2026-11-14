package io.github.fate_grand_automata.scripts.modules

import io.github.lib_automata.Region
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Finds orange, white-ringed counters in a 720p color map, independent of the terrain. */
object FreeQuestBadgeDetector {
    fun find(pixels: IntArray, width: Int, height: Int): List<Region> {
        require(pixels.size == width * height)
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        val badges = mutableListOf<Region>()

        fun orange(index: Int): Boolean {
            val color = pixels[index]
            val r = (color shr 16) and 255
            val g = (color shr 8) and 255
            val b = color and 255
            return r >= 210 && g in 65..195 && b <= 105 && r - g >= 55
        }

        fun white(x: Int, y: Int): Boolean {
            if (x !in 0 until width || y !in 0 until height) return false
            val color = pixels[y * width + x]
            val r = (color shr 16) and 255
            val g = (color shr 8) and 255
            val b = color and 255
            return minOf(r, g, b) >= 195 && maxOf(r, g, b) - minOf(r, g, b) <= 45
        }

        for (seed in pixels.indices) {
            if (visited[seed] || !orange(seed)) continue
            var head = 0
            var tail = 1
            queue[0] = seed
            visited[seed] = true
            var left = seed % width
            var right = left
            var top = seed / width
            var bottom = top
            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                left = minOf(left, x)
                right = maxOf(right, x)
                top = minOf(top, y)
                bottom = maxOf(bottom, y)
                fun add(next: Int) {
                    if (!visited[next] && orange(next)) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
                if (x > 0) add(index - 1)
                if (x + 1 < width) add(index + 1)
                if (y > 0) add(index - width)
                if (y + 1 < height) add(index + width)
            }
            val w = right - left + 1
            val h = bottom - top + 1
            // Ignore UI decorations, tiny terrain details, and large orange panels.
            if (w !in 18..56 || h !in 18..56 || w.toDouble() / h !in 0.80..1.25) continue
            if (tail.toDouble() / (w * h) !in 0.40..0.85) continue
            val cx = (left + right) / 2.0
            val cy = (top + bottom) / 2.0
            val ringSamples = (0 until 24).count { sample ->
                val angle = sample * Math.PI / 12
                // Shading can shrink the orange component several pixels inside the white rim.
                (0..5).any { offset ->
                    white(
                        (cx + (w / 2.0 + offset) * cos(angle)).roundToInt(),
                        (cy + (h / 2.0 + offset) * sin(angle)).roundToInt()
                    )
                }
            }
            if (ringSamples < 17) continue
            // A counter has a white digit as well as a white border.
            val hasDigit = (top + h / 4..bottom - h / 4).any { y ->
                (left + w / 4..right - w / 4).any { x -> white(x, y) }
            }
            if (hasDigit) badges += Region(left, top, w, h)
        }
        return badges.sorted()
    }
}
