package com.example.radarcamera.navigation

class NavigationStack(initial: CoachingDestination) {
    private val destinations = mutableListOf(initial)

    val entries: List<CoachingDestination>
        get() = destinations.toList()

    val current: CoachingDestination
        get() = destinations.last()

    fun push(destination: CoachingDestination) {
        destinations += destination
    }

    fun replace(destination: CoachingDestination) {
        destinations[destinations.lastIndex] = destination
    }

    fun pop(): CoachingDestination {
        if (destinations.size > 1) destinations.removeAt(destinations.lastIndex)
        return current
    }
}
