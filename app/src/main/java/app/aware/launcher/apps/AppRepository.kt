package app.aware.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import app.aware.launcher.usage.UsageAccess
import app.aware.launcher.usage.UsageTracker
import java.text.Collator

data class AppEntry(val label: String, val component: ComponentName, val user: UserHandle, val usageMillis: Long)

data class HomeState(val apps: List<AppEntry>, val usageAccessGranted: Boolean, val totalUsageMillis: Long)

/** Builds the home screen's data. Called off the main thread. */
object AppRepository {

    fun load(context: Context): HomeState {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
            ?: return HomeState(emptyList(), usageAccessGranted = false, totalUsageMillis = 0L)
        val granted = UsageAccess.isGranted(context)
        val usage = if (granted) UsageTracker.today(context) else emptyMap()
        val ownPackage = context.packageName
        val collator = Collator.getInstance()

        val apps = launcherApps.profiles
            .flatMap { profile -> launcherApps.getActivityList(null, profile) }
            .filter { it.componentName.packageName != ownPackage }
            .map { info ->
                AppEntry(
                    label = info.label.toString(),
                    component = info.componentName,
                    user = info.user,
                    usageMillis = usage[info.componentName.packageName] ?: 0L
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }

        // Count each package once, even if it exposes several launcher activities.
        val total = apps.map { it.component.packageName }.distinct().sumOf { usage[it] ?: 0L }
        return HomeState(apps, granted, total)
    }
}
