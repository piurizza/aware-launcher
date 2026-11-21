package app.aware.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import app.aware.launcher.usage.AppUsage
import app.aware.launcher.usage.DayTotal
import app.aware.launcher.usage.Unused
import app.aware.launcher.usage.UsageAccess
import app.aware.launcher.usage.UsageCalculator
import app.aware.launcher.usage.UsageTracker
import java.text.Collator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppEntry(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWorkProfile: Boolean,
    val installedAt: Long,
    val usage: AppUsage = AppUsage(),
    /** Days since last opened, when that is at least [Unused.THRESHOLD_DAYS]; null otherwise. */
    val unusedDays: Int? = null
) {
    /** Stable identity for per-app settings; work-profile copies are kept apart. */
    val key: String get() = (if (isWorkProfile) "work/" else "") + component.flattenToShortString()
}

data class HomeState(
    val apps: List<AppEntry>,
    val usageAccessGranted: Boolean,
    val totalUsageMillis: Long,
    /** Yesterday's total up to the same time of day, or null when Android kept no data for it. */
    val yesterdaySoFarMillis: Long?
)

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
                    isWorkProfile = info.user != ownUser,
                    installedAt = info.firstInstallTime
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /**
     * Attaches today's usage to [apps]. Android keeps usage per profile and Aware can only read its
     * own, so apps in other profiles (such as a work profile) show no usage rather than a wrong one.
     */
    fun withUsage(context: Context, apps: List<AppEntry>, now: Long = System.currentTimeMillis()): HomeState {
        val granted = UsageAccess.isGranted(context)
        if (!granted) return HomeState(apps, usageAccessGranted = false, 0L, null)

        val usage = UsageTracker.today(context, now)
        val lastVisible = UsageTracker.lastVisible(context, now)
        val withUsage = apps.map { app ->
            if (app.isWorkProfile) return@map app
            val pkg = app.component.packageName
            val today = usage[pkg] ?: AppUsage()
            val usedToday = today.opens > 0 || today.foregroundMillis > 0
            app.copy(usage = today, unusedDays = Unused.days(lastVisible[pkg], app.installedAt, now, usedToday))
        }
        val packages = ownPackages(apps)

        val zone = ZoneId.systemDefault()
        val todayStart = UsageCalculator.startOfDay(now, zone)
        val yesterdayStart = UsageCalculator.startOfDay(now, zone, daysAgo = 1)
        val yesterday = UsageTracker.range(context, yesterdayStart, yesterdayStart + (now - todayStart))

        return HomeState(
            apps = withUsage,
            usageAccessGranted = true,
            totalUsageMillis = total(usage, packages),
            yesterdaySoFarMillis = yesterday?.let { total(it, packages) }
        )
    }

    /** Screen time in [apps] for today and the six days before it, newest first. */
    fun week(context: Context, apps: List<AppEntry>, now: Long = System.currentTimeMillis()): List<DayTotal> {
        val zone = ZoneId.systemDefault()
        val packages = ownPackages(apps)
        val formatter = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())
        return (0L until DAYS_IN_WEEK).map { daysAgo ->
            val start = UsageCalculator.startOfDay(now, zone, daysAgo)
            val end = if (daysAgo == 0L) now else start + TimeUnit.DAYS.toMillis(1)
            val label = formatter.format(Instant.ofEpochMilli(start).atZone(zone))
            DayTotal(label, UsageTracker.range(context, start, minOf(end, now))?.let { total(it, packages) })
        }
    }

    // Android keeps usage per profile and Aware can only read its own, so apps in other profiles
    // (such as a work profile) show no usage rather than a wrong one.
    private fun ownPackages(apps: List<AppEntry>): Set<String> =
        apps.filterNot { it.isWorkProfile }.map { it.component.packageName }.toSet()

    // Each package counts once, even if it exposes several launcher activities.
    private fun total(usage: Map<String, AppUsage>, packages: Set<String>): Long =
        packages.sumOf { usage[it]?.foregroundMillis ?: 0L }

    private const val DAYS_IN_WEEK = 7L
}
