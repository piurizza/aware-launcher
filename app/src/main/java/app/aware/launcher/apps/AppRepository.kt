package app.aware.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import app.aware.launcher.usage.AppUsage
import app.aware.launcher.usage.UsageAccess
import app.aware.launcher.usage.UsageTracker
import java.text.Collator

data class AppEntry(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWorkProfile: Boolean,
    val usage: AppUsage = AppUsage()
)

data class HomeState(val apps: List<AppEntry>, val usageAccessGranted: Boolean, val totalUsageMillis: Long)

/** Builds the home screen's data. Called off the main thread. */
object AppRepository {

    /**
     * The launchable apps of every profile, sorted by label. Relatively expensive (labels are
     * loaded from each app), so callers cache it and reload only when packages change.
     */
    fun loadApps(context: Context): List<AppEntry> {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        val ownPackage = context.packageName
        val ownUser = Process.myUserHandle()
        val collator = Collator.getInstance()

        return launcherApps.profiles
            .flatMap { profile -> launcherApps.getActivityList(null, profile) }
            .filter { it.componentName.packageName != ownPackage }
            .map { info ->
                AppEntry(
                    label = info.label.toString(),
                    component = info.componentName,
                    user = info.user,
                    isWorkProfile = info.user != ownUser
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /**
     * Attaches today's usage to [apps]. Android keeps usage per profile and Aware can only read its
     * own, so apps in other profiles (such as a work profile) show no usage rather than a wrong one.
     */
    fun withUsage(context: Context, apps: List<AppEntry>): HomeState {
        val granted = UsageAccess.isGranted(context)
        val usage = if (granted) UsageTracker.today(context) else emptyMap()

        val withUsage = apps.map { app ->
            if (app.isWorkProfile) app else app.copy(usage = usage[app.component.packageName] ?: AppUsage())
        }
        // Count each package once, even if it exposes several launcher activities.
        val total = withUsage
            .filterNot { it.isWorkProfile }
            .map { it.component.packageName }
            .distinct()
            .sumOf { usage[it]?.foregroundMillis ?: 0L }
        return HomeState(withUsage, granted, total)
    }
}
