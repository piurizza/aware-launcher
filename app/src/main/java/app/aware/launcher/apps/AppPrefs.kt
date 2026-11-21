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

    fun setPinned(app: AppEntry, pinned: Boolean) = write(PINNED, app.key, pinned)

    fun setHidden(app: AppEntry, hidden: Boolean) = write(HIDDEN, app.key, hidden)

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
    }
}
