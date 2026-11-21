package app.aware.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import app.aware.launcher.apps.AppAdapter
import app.aware.launcher.apps.AppEntry
import app.aware.launcher.apps.AppMenu
import app.aware.launcher.apps.AppPrefs
import app.aware.launcher.apps.AppRepository
import app.aware.launcher.apps.HomeState
import app.aware.launcher.usage.Comparison
import app.aware.launcher.usage.DurationFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The whole launcher: a clock, today's total screen time, a search field and a text list of apps
 * with the time spent in each today.
 */
class MainActivity : Activity() {

    private val loader: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var launcherApps: LauncherApps
    private lateinit var adapter: AppAdapter
    private lateinit var prefs: AppPrefs
    private lateinit var menu: AppMenu
    private lateinit var list: ListView
    private lateinit var search: EditText
    private lateinit var summary: TextView
    private var usageAccessGranted = true

    // The app list is only reloaded when packages change; usage is re-read on every return home.
    private val appsStale = AtomicBoolean(true)

    // Touched only on the loader thread.
    private var cachedApps: List<AppEntry> = emptyList()

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = onPackagesChanged()
        override fun onPackageAdded(packageName: String, user: UserHandle) = onPackagesChanged()
        override fun onPackageChanged(packageName: String, user: UserHandle) = onPackagesChanged()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            onPackagesChanged()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            onPackagesChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        launcherApps = getSystemService(LauncherApps::class.java)

        prefs = AppPrefs(this)
        adapter = AppAdapter(layoutInflater)
        menu = AppMenu(prefs, onChanged = ::applyMarks, onAppInfo = ::openAppInfo)
        list = findViewById(R.id.app_list)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ -> launch(adapter.getItem(position)) }
        list.setOnItemLongClickListener { _, view, position, _ ->
            menu.show(view, adapter.getItem(position))
            true
        }

        summary = findViewById(R.id.summary)
        summary.setOnClickListener {
            if (usageAccessGranted) showWeek() else startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        search = findViewById(R.id.search)
        search.addTextChangedListener(SearchWatcher())
        search.setOnEditorActionListener { _, actionId, _ ->
            val launchFirst = actionId == EditorInfo.IME_ACTION_GO && adapter.count > 0
            if (launchFirst) launch(adapter.getItem(0))
            launchFirst
        }

        launcherApps.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
    }

    override fun onResume() {
        super.onResume()
        resetSearch()
        refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed while already home: back to a clean list.
        resetSearch()
        list.setSelection(0)
    }

    override fun onDestroy() {
        launcherApps.unregisterCallback(packageCallback)
        loader.shutdownNow()
        super.onDestroy()
    }

    private fun onPackagesChanged() {
        appsStale.set(true)
        refresh()
    }

    private fun refresh() {
        if (loader.isShutdown) return
        loader.execute {
            if (appsStale.getAndSet(false)) cachedApps = AppRepository.loadApps(applicationContext)
            val state = AppRepository.withUsage(applicationContext, cachedApps)
            runOnUiThread { if (!isDestroyed) render(state) }
        }
    }

    private fun showWeek() {
        if (loader.isShutdown) return
        loader.execute {
            val days = AppRepository.week(applicationContext, cachedApps)
            runOnUiThread { if (!isDestroyed) WeekDialog.show(this, days) }
        }
    }

    private fun render(state: HomeState) {
        usageAccessGranted = state.usageAccessGranted
        summary.text = if (state.usageAccessGranted) summaryText(state) else getString(R.string.usage_access_prompt)
        adapter.submit(state.apps)
        applyMarks()
    }

    private fun applyMarks() {
        adapter.setMarks(prefs.pinned, prefs.hidden)
    }

    private fun summaryText(state: HomeState): String {
        val today = DurationFormat.format(state.totalUsageMillis)
        val yesterday = state.yesterdaySoFarMillis ?: return getString(R.string.summary_today, today)
        val comparison = Comparison.of(state.totalUsageMillis, yesterday)
        val difference = DurationFormat.format(comparison.differenceMillis)
        return when (comparison.direction) {
            Comparison.Direction.LESS -> getString(R.string.summary_less, today, difference)
            Comparison.Direction.MORE -> getString(R.string.summary_more, today, difference)
            Comparison.Direction.SAME -> getString(R.string.summary_same, today)
        }
    }

    private fun resetSearch() {
        search.text.clear()
        search.clearFocus()
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(search.windowToken, 0)
    }

    private fun launch(app: AppEntry) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "App no longer launchable: ${app.component}", e)
            onPackagesChanged()
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to launch ${app.component}", e)
        }
    }

    private fun openAppInfo(app: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app info screen for ${app.component}", e)
        }
    }

    private inner class SearchWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Only the final text matters.
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            adapter.filter(s)
        }

        override fun afterTextChanged(s: Editable?) {
            // Only the final text matters.
        }
    }

    private companion object {
        const val TAG = "Aware"
    }
}
