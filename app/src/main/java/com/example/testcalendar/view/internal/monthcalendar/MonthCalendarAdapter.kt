package com.example.testcalendar.view.internal.monthcalendar

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.testcalendar.core.CalendarDay
import com.example.testcalendar.core.CalendarMonth
import com.example.testcalendar.core.DayPosition
import com.example.testcalendar.core.OutDateStyle
import com.example.testcalendar.core.nextMonth
import com.example.testcalendar.core.previousMonth
import com.example.testcalendar.core.yearMonth
import com.example.testcalendar.data.DataStore
import com.example.testcalendar.data.getCalendarMonthData
import com.example.testcalendar.data.getMonthIndex
import com.example.testcalendar.data.getMonthIndicesCount
import com.example.testcalendar.view.CalendarView
import com.example.testcalendar.view.MonthDayBinder
import com.example.testcalendar.view.MonthHeaderFooterBinder
import com.example.testcalendar.view.ViewContainer
import com.example.testcalendar.view.internal.NO_INDEX
import com.example.testcalendar.view.internal.dayTag
import com.example.testcalendar.view.internal.setupItemRoot
import java.time.DayOfWeek
import java.time.YearMonth

internal class MonthCalendarAdapter(
    private val calView: CalendarView,
    private var outDateStyle: OutDateStyle,
    private var startMonth: YearMonth,
    private var endMonth: YearMonth,
    private var firstDayOfWeek: DayOfWeek,
) : RecyclerView.Adapter<MonthViewHolder>() {

    @RequiresApi(Build.VERSION_CODES.O)
    private var itemCount = getMonthIndicesCount(startMonth, endMonth)

    @RequiresApi(Build.VERSION_CODES.O)
    private val dataStore = DataStore { offset ->
        getCalendarMonthData(startMonth, offset, firstDayOfWeek, outDateStyle).calendarMonth
    }

    init {
        setHasStableIds(true)
    }

    private val isAttached: Boolean
        get() = calView.adapter === this

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        calView.post { notifyMonthScrollListenerIfNeeded() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getItem(position: Int): CalendarMonth = dataStore[position]

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getItemId(position: Int): Long = getItem(position).yearMonth.hashCode().toLong()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getItemCount(): Int = itemCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val content = setupItemRoot(
            itemMargins = calView.monthMargins,
            daySize = calView.daySize,
            context = calView.context,
            dayViewResource = calView.dayViewResource,
            itemHeaderResource = calView.monthHeaderResource,
            itemFooterResource = calView.monthFooterResource,
            weekSize = 6,
            itemViewClass = calView.monthViewClass,
            dayBinder = calView.dayBinder as MonthDayBinder,
        )

        @Suppress("UNCHECKED_CAST")
        return MonthViewHolder(
            rootLayout = content.itemView,
            headerView = content.headerView,
            footerView = content.footerView,
            weekHolders = content.weekHolders,
            monthHeaderBinder = calView.monthHeaderBinder as MonthHeaderFooterBinder<ViewContainer>?,
            monthFooterBinder = calView.monthFooterBinder as MonthHeaderFooterBinder<ViewContainer>?,
        )
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach {
                holder.reloadDay(it as CalendarDay)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bindMonth(getItem(position))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadDay(vararg day: CalendarDay) {
        day.forEach { day ->
            val position = getAdapterPosition(day)
            if (position != NO_INDEX) {
                notifyItemChanged(position, day)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadMonth(month: YearMonth) {
        notifyItemChanged(getAdapterPosition(month))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadCalendar() {
        notifyItemRangeChanged(0, itemCount)
    }

    private var visibleMonth: CalendarMonth? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun notifyMonthScrollListenerIfNeeded() {
        // Guard for cv.post() calls and other callbacks which use this method.
        if (!isAttached) return

        if (calView.isAnimating) {
            // Fixes an issue where findFirstVisibleMonthPosition() returns
            // zero if called when the RecyclerView is animating. This can be
            // replicated in Example 1 when switching from week to month mode.
            // The property changes when switching modes in Example 1 cause
            // notifyDataSetChanged() to be called, hence the animation.
            calView.itemAnimator?.isRunning {
                notifyMonthScrollListenerIfNeeded()
            }
            return
        }
        val visibleItemPos = findFirstVisibleMonthPosition()
        if (visibleItemPos != RecyclerView.NO_POSITION) {
            val visibleMonth = dataStore[visibleItemPos]

            if (visibleMonth != this.visibleMonth) {
                this.visibleMonth = visibleMonth
                calView.monthScrollListener?.invoke(visibleMonth)

                // Fixes issue where the calendar does not resize its height when in horizontal, paged mode and
                // the `outDateStyle` is not `endOfGrid` hence the last row of a 5-row visible month is empty.
                // We set such week row's container visibility to GONE in the WeekHolder but it seems the
                // RecyclerView accounts for the items in the immediate previous and next indices when
                // calculating height and uses the tallest one of the three meaning that the current index's
                // view will end up having a blank space at the bottom unless the immediate previous and next
                // indices are also missing the last row. I think there should be a better way to fix this.
                // New: Also fixes issue where the calendar does not wrap each month's height when in vertical,
                // paged mode and just matches parent's height instead.
                // Only happens when the CalendarView wraps its height.
                if (calView.scrollPaged && calView.layoutParams.height == WRAP_CONTENT) {
                    val visibleVH =
                        calView.findViewHolderForAdapterPosition(visibleItemPos) ?: return
                    // Fixes #199, #266
                    visibleVH.itemView.requestLayout()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun getAdapterPosition(month: YearMonth): Int {
        return getMonthIndex(startMonth, month)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun getAdapterPosition(day: CalendarDay): Int {
        return getAdapterPosition(day.positionYearMonth)
    }

    private val layoutManager: MonthCalendarLayoutManager
        get() = calView.layoutManager as MonthCalendarLayoutManager

    private fun findFirstVisibleMonthPosition(): Int = layoutManager.findFirstVisibleItemPosition()

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    internal fun updateData(
        startMonth: YearMonth,
        endMonth: YearMonth,
        outDateStyle: OutDateStyle,
        firstDayOfWeek: DayOfWeek,
    ) {
        this.startMonth = startMonth
        this.endMonth = endMonth
        this.outDateStyle = outDateStyle
        this.firstDayOfWeek = firstDayOfWeek
        this.itemCount = getMonthIndicesCount(startMonth, endMonth)
        dataStore.clear()
        notifyDataSetChanged()
    }
}

// Find the actual month on the calendar where this date is shown.
internal val CalendarDay.positionYearMonth: YearMonth
    @RequiresApi(Build.VERSION_CODES.O)
    get() = when (position) {
        DayPosition.InDate -> date.yearMonth.nextMonth
        DayPosition.MonthDate -> date.yearMonth
        DayPosition.OutDate -> date.yearMonth.previousMonth
    }
