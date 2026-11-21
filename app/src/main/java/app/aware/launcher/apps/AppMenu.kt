package app.aware.launcher.apps

import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import app.aware.launcher.R

/** The long-press menu on an app row. */
class AppMenu(
    private val prefs: AppPrefs,
    private val onChanged: () -> Unit,
    private val onAppInfo: (AppEntry) -> Unit
) {

    fun show(anchor: View, app: AppEntry) {
        val isPinned = app.key in prefs.pinned
        val isHidden = app.key in prefs.hidden
        val popup = PopupMenu(anchor.context, anchor)
        popup.menu.add(Menu.NONE, PIN, Menu.NONE, if (isPinned) R.string.menu_unpin else R.string.menu_pin)
        popup.menu.add(Menu.NONE, HIDE, Menu.NONE, if (isHidden) R.string.menu_unhide else R.string.menu_hide)
        popup.menu.add(Menu.NONE, APP_INFO, Menu.NONE, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                PIN -> prefs.setPinned(app, !isPinned)
                HIDE -> prefs.setHidden(app, !isHidden)
                APP_INFO -> onAppInfo(app)
            }
            if (item.itemId != APP_INFO) onChanged()
            true
        }
        popup.show()
    }

    private companion object {
        const val PIN = 1
        const val HIDE = 2
        const val APP_INFO = 3
    }
}
