package app.aware.launcher.apps

import android.app.AlertDialog
import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import app.aware.launcher.R
import app.aware.launcher.usage.DurationFormat
import app.aware.launcher.usage.Limit
import app.aware.launcher.usage.Unused

/** The long-press menu on an app row. */
class AppMenu(
    private val prefs: AppPrefs,
    private val onChanged: () -> Unit,
    private val onAppInfo: (AppEntry) -> Unit
) {

    fun show(anchor: View, app: AppEntry) {
        val isPinned = app.key in prefs.pinned
        val isHidden = app.key in prefs.hidden
        val isDistracting = app.key in prefs.distracting
        val popup = PopupMenu(anchor.context, anchor)
        info(anchor.context, app)?.let { popup.menu.add(Menu.NONE, INFO, Menu.NONE, it).isEnabled = false }
        popup.menu.add(Menu.NONE, PIN, Menu.NONE, if (isPinned) R.string.menu_unpin else R.string.menu_pin)
        popup.menu.add(Menu.NONE, HIDE, Menu.NONE, if (isHidden) R.string.menu_unhide else R.string.menu_hide)
        popup.menu.add(
            Menu.NONE,
            DISTRACTING,
            Menu.NONE,
            if (isDistracting) R.string.menu_not_distracting else R.string.menu_distracting
        )
        // Android keeps work-profile usage separately, so limits only apply to this profile's apps.
        if (!app.isWorkProfile) popup.menu.add(Menu.NONE, LIMIT, Menu.NONE, limitTitle(anchor.context, app))
        // Android lets no app stop another, so this opens App info, where Force stop is one tap away.
        if (app.isRunningInBackground) popup.menu.add(Menu.NONE, STOP, Menu.NONE, R.string.menu_stop)
        popup.menu.add(Menu.NONE, APP_INFO, Menu.NONE, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                PIN -> prefs.setPinned(app, !isPinned)
                HIDE -> prefs.setHidden(app, !isHidden)
                DISTRACTING -> prefs.setDistracting(app, !isDistracting)
                LIMIT -> showLimitPicker(anchor.context, app)
                APP_INFO, STOP -> onAppInfo(app)
            }
            if (item.itemId in PREFERENCE_ITEMS) onChanged()
            true
        }
        popup.show()
    }

    /** Today's details for the top of the menu; none for work apps, whose usage Aware can't read. */
    private fun info(context: Context, app: AppEntry): String? {
        val days = app.unusedDays
        val res = context.resources
        return when {
            app.isWorkProfile -> null
            days != null && Unused.isOverAYear(days) -> context.getString(R.string.info_unopened_year)
            days != null -> res.getQuantityString(R.plurals.info_unopened_days, days, days)
            app.usage.opens == 0 -> context.getString(R.string.info_not_today)
            else -> res.getQuantityString(
                R.plurals.info_today,
                app.usage.opens,
                DurationFormat.format(app.usage.foregroundMillis),
                app.usage.opens
            )
        }
    }

    private fun limitTitle(context: Context, app: AppEntry): String {
        val minutes = prefs.limits[app.key]
        return if (minutes == null) {
            context.getString(R.string.menu_limit)
        } else {
            context.getString(R.string.menu_limit_set, DurationFormat.format(Limit.minutesToMillis(minutes)))
        }
    }

    private fun showLimitPicker(context: Context, app: AppEntry) {
        val choices = Limit.CHOICES_MINUTES
        val labels = choices.map { minutes ->
            if (minutes ==
                0
            ) {
                context.getString(R.string.limit_none)
            } else {
                DurationFormat.format(Limit.minutesToMillis(minutes))
            }
        }
        val current = choices.indexOf(prefs.limits[app.key] ?: 0).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.limit_title, app.label))
            .setSingleChoiceItems(labels.toTypedArray(), current) { dialog, which ->
                prefs.setLimit(app, choices[which])
                onChanged()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private companion object {
        const val PIN = 1
        const val HIDE = 2
        const val APP_INFO = 3
        const val DISTRACTING = 4
        const val LIMIT = 5
        const val STOP = 6
        const val INFO = 7
        val PREFERENCE_ITEMS = setOf(PIN, HIDE, DISTRACTING)
    }
}
