package io.github.lib_automata

/**
 * Interface for classes which can perform gestures.
 */
interface GestureService : AutoCloseable {
    /**
     * Swipes from one [Location] to another [Location].
     *
     * @param start the [Location] where the swipe should start
     * @param end the [Location] where the swipe should end
     */
    fun swipe(start: Location, end: Location)

    /** Moves two fingers simultaneously, in physical screen coordinates. */
    fun pinch(firstStart: Location, firstEnd: Location, secondStart: Location, secondEnd: Location)

    /**
     * Clicks on a given [location].
     *
     * @param location the location to click on
     * @param times the number of times to click
     */
    fun click(location: Location, times: Int = 1)
}
