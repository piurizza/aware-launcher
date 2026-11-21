package app.aware.launcher.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import app.aware.launcher.R
import app.aware.launcher.usage.DurationFormat
import app.aware.launcher.usage.Unused

/** Plain-text app list. Uses the framework ListView so there are no library dependencies. */
class AppAdapter(private val inflater: LayoutInflater) : BaseAdapter() {

    private var all: List<AppEntry> = emptyList()
    private var visible: List<AppEntry> = emptyList()
    private var query: String = ""
    private var pinned: Set<String> = emptySet()
    private var hidden: Set<String> = emptySet()

    /** Applies pins and hidden apps without reloading anything. */
    fun setMarks(pinned: Set<String>, hidden: Set<String>) {
        this.pinned = pinned
        this.hidden = hidden
        applyFilter()
    }

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
        holder.label.text = label(view, app)
        holder.usage.text = app.unusedDays?.let(Unused::label) ?: DurationFormat.forRow(app.usage)
        return view
    }

    private fun label(view: View, app: AppEntry): String {
        val name = if (app.isWorkProfile) view.context.getString(R.string.work_app_label, app.label) else app.label
        // Hidden apps only appear in search results; say so, so they can be found and unhidden.
        return if (app.key in hidden) view.context.getString(R.string.hidden_app_label, name) else name
    }

    private class Holder(view: View) {
        val label: TextView = view.findViewById(R.id.label)
        val usage: TextView = view.findViewById(R.id.usage)
    }
}
