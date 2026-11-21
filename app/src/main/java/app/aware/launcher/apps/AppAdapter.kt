package app.aware.launcher.apps

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import app.aware.launcher.R
import app.aware.launcher.usage.DurationFormat
import app.aware.launcher.usage.Limit

/** Plain-text app list. Uses the framework ListView so there are no library dependencies. */
class AppAdapter(private val inflater: LayoutInflater) : BaseAdapter() {

    private var all: List<AppEntry> = emptyList()
    private var visible: List<AppEntry> = emptyList()
    private var query: String = ""
    private var pinned: Set<String> = emptySet()
    private var hidden: Set<String> = emptySet()
    private var limits: Map<String, Int> = emptyMap()

    /** Applies pins, hidden apps and limits without reloading anything. */
    fun setMarks(pinned: Set<String>, hidden: Set<String>, limits: Map<String, Int>) {
        this.pinned = pinned
        this.hidden = hidden
        this.limits = limits
        applyFilter()
    }

    /** Today's usage against the app's daily limit, or null when it has none. */
    fun limitFor(app: AppEntry): Limit? = limits[app.key]
        ?.takeUnless { app.isWorkProfile }
        ?.let { Limit(Limit.minutesToMillis(it), app.usage.foregroundMillis) }

    fun submit(apps: List<AppEntry>) {
        all = apps
        applyFilter()
    }

    fun filter(text: CharSequence?) {
        query = text?.toString()?.trim().orEmpty()
        applyFilter()
    }

    private fun applyFilter() {
        visible = AppOrder.arrange(
            all,
            query,
            label = { it.label },
            isPinned = { it.key in pinned },
            isHidden = { it.key in hidden }
        )
        notifyDataSetChanged()
    }

    override fun getCount(): Int = visible.size

    override fun getItem(position: Int): AppEntry = visible[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_app, parent, false).also { it.tag = Holder(it) }
        val holder = view.tag as Holder
        val app = visible[position]
        holder.label.text = withRunningDot(view, label(view, app), app.isRunningInBackground)
        holder.label.contentDescription = if (app.isRunningInBackground) {
            view.context.getString(R.string.running_description, label(view, app))
        } else {
            null
        }
        // Apps not opened for weeks are dimmed: a quiet hint, with details in the long-press menu.
        val nameColor = if (app.unusedDays != null) R.color.text_secondary else R.color.text_primary
        holder.label.setTextColor(view.context.getColor(nameColor))

        val limit = limitFor(app)
        holder.usage.text = limit?.rowLabel() ?: DurationFormat.forRow(app.usage.foregroundMillis)
        val usageColor = if (limit?.isReached == true) R.color.over_limit else R.color.text_secondary
        holder.usage.setTextColor(view.context.getColor(usageColor))
        return view
    }

    private fun label(view: View, app: AppEntry): String {
        val name = if (app.isWorkProfile) view.context.getString(R.string.work_app_label, app.label) else app.label
        // Hidden apps only appear in search results; say so, so they can be found and unhidden.
        return if (app.key in hidden) view.context.getString(R.string.hidden_app_label, name) else name
    }

    /** A small coloured dot after the name of an app that keeps running in the background. */
    private fun withRunningDot(view: View, name: String, running: Boolean): CharSequence {
        if (!running) return name
        val text = SpannableStringBuilder(name).append("  ")
        val start = text.length
        text.append(view.context.getString(R.string.running_dot))
        val color = ForegroundColorSpan(view.context.getColor(R.color.running))
        text.setSpan(color, start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }

    private class Holder(view: View) {
        val label: TextView = view.findViewById(R.id.label)
        val usage: TextView = view.findViewById(R.id.usage)
    }
}
