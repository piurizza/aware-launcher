package app.aware.launcher.apps

/** How the home list is ordered and filtered. Pure, so it is unit tested without Android. */
object AppOrder {

    /**
     * Without a [query]: hidden apps are left out and pinned apps come first, both groups keeping
     * their incoming (alphabetical) order. With a query: every match, hidden ones included, so a
     * hidden app is always one search away.
     */
    fun <T> arrange(
        items: List<T>,
        query: String,
        label: (T) -> String,
        isPinned: (T) -> Boolean,
        isHidden: (T) -> Boolean
    ): List<T> {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) return items.filter { label(it).contains(trimmed, ignoreCase = true) }
        val (pinned, rest) = items.filterNot(isHidden).partition(isPinned)
        return pinned + rest
    }
}
