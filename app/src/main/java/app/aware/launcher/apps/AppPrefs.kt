package app.aware.launcher.apps

import android.content.Context
import android.content.SharedPreferences

/**
 * Per-app choices the user makes from the long-press menu, kept in a tiny SharedPreferences file.
 * Apps are identified by [AppEntry.key], so a work-profile copy of an app is set separately.
 */
class AppPrefs(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val pinned: Set<String> get() = read(PINNED)
    val hidden: Set<String> get() = read(HIDDEN)
    val distracting: Set<String> get() = read(DISTRACTING)

    fun setPinned(app: AppEntry, pinned: Boolean) = write(PINNED, app.key, pinned)

    fun setHidden(app: AppEntry, hidden: Boolean) = write(HIDDEN, app.key, hidden)

    fun setDistracting(app: AppEntry, distracting: Boolean) = write(DISTRACTING, app.key, distracting)

    /** Daily limits in minutes, by [AppEntry.key]. */
    val limits: Map<String, Int>
        get() = prefs.all.mapNotNull { (name, value) ->
            if (name.startsWith(LIMIT_PREFIX) && value is Int) name.removePrefix(LIMIT_PREFIX) to value else null
        }.toMap()

    /** Sets the daily limit; 0 removes it. */
    fun setLimit(app: AppEntry, minutes: Int) {
        val editor = prefs.edit()
        if (minutes > 0) editor.putInt(LIMIT_PREFIX + app.key, minutes) else editor.remove(LIMIT_PREFIX + app.key)
        editor.apply()
    }

    // Copy: the set returned by getStringSet must not be modified.
    private fun read(name: String): Set<String> = prefs.getStringSet(name, null)?.toSet().orEmpty()

    private fun write(name: String, key: String, add: Boolean) {
        val updated = if (add) read(name) + key else read(name) - key
        prefs.edit().putStringSet(name, updated).apply()
    }

    private companion object {
        const val FILE = "aware"
        const val PINNED = "pinned"
        const val HIDDEN = "hidden"
        const val DISTRACTING = "distracting"
        const val LIMIT_PREFIX = "limit:"
    }
}
