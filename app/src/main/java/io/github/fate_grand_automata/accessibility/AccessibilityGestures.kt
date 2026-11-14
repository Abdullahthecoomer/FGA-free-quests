package io.github.fate_grand_automata.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.fate_grand_automata.scripts.prefs.IGesturesPreferences
import io.github.lib_automata.GestureService
import io.github.lib_automata.Location
import io.github.lib_automata.Waiter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.*

/**
 * Class to perform gestures using Android's [AccessibilityService].
 */
class AccessibilityGestures @Inject constructor(
    private val gesturePrefs: IGesturesPreferences,
    private val wait: Waiter
) : GestureService {
    fun Path.moveTo(location: Location) = apply {
        moveTo(location.x.toFloat(), location.y.toFloat())
    }

    fun Path.lineTo(location: Location) = apply {
        lineTo(location.x.toFloat(), location.y.toFloat())
    }

    /**
     * On Android 7, swipe is like a flick.
     * If the swipe distance is too long, FGO won't detect it correctly and have occasional weird behaviour like sudden jumps
     */
    suspend fun swipe7(start: Location, end: Location) {
        val swipePath = Path()
            .moveTo(start)
            .lineTo(end)

        val swipeStroke = GestureDescription.StrokeDescription(
            swipePath,
            0,
            gesturePrefs.swipeDuration.inWholeMilliseconds
        )
        performGesture(swipeStroke)

        wait(gesturePrefs.swipeWaitTime)
    }

    /**
     * Android 8+ swipe is precise due to use of continued gestures.
     *
     * Instead of swiping the whole distance as a single gesture,
     * it is split into multiple small swipes, which is similar to how events are sent if a real human is doing it.
     * There is a finger down delay, followed by multiple small swipe events, followed by a finger lift delay.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun swipe8(start: Location, end: Location) {
        val xDiff = (end.x - start.x).toFloat()
        val yDiff = (end.y - start.y).toFloat()
        val direction = atan2(xDiff, yDiff)
        var distanceLeft = sqrt(xDiff.pow(2) + yDiff.pow(2))

        val swipeDelay = 1L
        val swipeDuration = 1L

        val timesToSwipe = gesturePrefs.swipeDuration.inWholeMilliseconds / (swipeDelay + swipeDuration)
        val thresholdDistance = distanceLeft / timesToSwipe

        var from = start
        val mouseDownPath = Path().moveTo(start)

        var lastStroke = GestureDescription.StrokeDescription(
            mouseDownPath,
            0,
            200L,
            true
        ).also {
            performGesture(it)
        }

        while (distanceLeft > 0) {
            val distanceToScroll = minOf(thresholdDistance, distanceLeft)

            val x = (from.x + distanceToScroll * sin(direction)).roundToInt()
            val y = (from.y + distanceToScroll * cos(direction)).roundToInt()
            val to = Location(x, y)

            val swipePath = Path()
                .moveTo(from)
                .lineTo(to)

            lastStroke = lastStroke.continueStroke(
                swipePath,
                swipeDelay,
                swipeDuration,
                true
            ).also {
                performGesture(it)
            }

            from = to
            distanceLeft -= distanceToScroll
        }

        val mouseUpPath = Path().moveTo(from)

        lastStroke.continueStroke(
            mouseUpPath,
            1,
            400L,
            false
        ).also {
            performGesture(it)
        }
    }

    override fun swipe(start: Location, end: Location) = runBlocking {
        Timber.d("swipe $start, $end")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            swipe8(start, end)
        } else swipe7(start, end)
    }

    override fun click(location: Location, times: Int) = runBlocking {
        val swipePath = Path().moveTo(location)

        val stroke = GestureDescription.StrokeDescription(
            swipePath,
            gesturePrefs.clickDelay.inWholeMilliseconds,
            gesturePrefs.clickDuration.inWholeMilliseconds
        )

        Timber.d("click $location x$times")

        repeat(times) {
            performGesture(stroke)
        }

        wait(gesturePrefs.clickWaitTime)
    }

    override fun pinch(firstStart: Location, firstEnd: Location, secondStart: Location, secondEnd: Location) = runBlocking {
        val duration = maxOf(500L, gesturePrefs.swipeDuration.inWholeMilliseconds)
        val first = GestureDescription.StrokeDescription(Path().moveTo(firstStart).lineTo(firstEnd), 0, duration)
        val second = GestureDescription.StrokeDescription(Path().moveTo(secondStart).lineTo(secondEnd), 0, duration)
        check(performGesture(first, second)) { "Map zoom gesture was cancelled" }
        wait(gesturePrefs.swipeWaitTime)
    }

    private suspend fun performGesture(vararg strokes: GestureDescription.StrokeDescription): Boolean = suspendCancellableCoroutine {
        val builder = GestureDescription.Builder()
        strokes.forEach { builder.addStroke(it) }
        val gestureDesc = builder.build()

        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                it.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                it.resume(false)
            }
        }

        TapperService.instance?.dispatchGesture(gestureDesc, callback, null)
    }

    override fun close() {}
}
