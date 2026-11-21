package app.aware.launcher.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import app.aware.launcher.R
import app.aware.launcher.usage.DurationFormat

/** Plain-text app list. Uses the framework ListView so there are no library dependencies. */
class AppAdapter(private val inflater: LayoutInflater) : BaseAdapter() {

    private var all: List<AppEntry> = emptyList()
    private var visible: List<AppEntry> = emptyList()
    private var query: String = ""

    fun submit(apps: List<AppEntry>) {
        all = apps
        applyFilter()
    }

    fun filter(text: CharSequence?) {
        query = text?.toString()?.trim().orEmpty()
        applyFilter()
    }

    private fun applyFilter() {
        visible = if (query.isEmpty()) all else all.filter { it.label.contains(query, ignoreCase = true) }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = visible.size

    override fun getItem(position: Int): AppEntry = visible[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_app, parent, false).also { it.tag = Holder(it) }
        val holder = view.tag as Holder
        val app = visible[position]
        holder.label.text = if (app.isWorkProfile) {
            view.context.getString(R.string.work_app_label, app.label)
        } else {
            app.label
        }
        holder.usage.text = DurationFormat.forRow(app.usageMillis)
        return view
    }

    private class Holder(view: View) {
        val label: TextView = view.findViewById(R.id.label)
        val usage: TextView = view.findViewById(R.id.usage)
    }
}
