package com.example.testcalendar.view.internal.weekcalendar

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.testcalendar.core.Week
import com.example.testcalendar.core.WeekDay
import com.example.testcalendar.data.DataStore
import com.example.testcalendar.data.getWeekCalendarAdjustedRange
import com.example.testcalendar.data.getWeekCalendarData
import com.example.testcalendar.data.getWeekIndex
import com.example.testcalendar.data.getWeekIndicesCount
import com.example.testcalendar.view.ViewContainer
import com.example.testcalendar.view.WeekCalendarView
import com.example.testcalendar.view.WeekDayBinder
import com.example.testcalendar.view.WeekHeaderFooterBinder
import com.example.testcalendar.view.internal.NO_INDEX
import com.example.testcalendar.view.internal.dayTag
import com.example.testcalendar.view.internal.setupItemRoot
import java.time.DayOfWeek
import java.time.LocalDate

internal class WeekCalendarAdapter(
    private val calView: WeekCalendarView,
    private var startDate: LocalDate,
    private var endDate: LocalDate,
    private var firstDayOfWeek: DayOfWeek,
) : RecyclerView.Adapter<WeekViewHolder>() {

    @RequiresApi(Build.VERSION_CODES.O)
    private var adjustedData = getWeekCalendarAdjustedRange(startDate, endDate, firstDayOfWeek)
    private val startDateAdjusted: LocalDate
        @RequiresApi(Build.VERSION_CODES.O)
        get() = adjustedData.startDateAdjusted
    private val endDateAdjusted: LocalDate
        @RequiresApi(Build.VERSION_CODES.O)
        get() = adjustedData.endDateAdjusted

    @RequiresApi(Build.VERSION_CODES.O)
    private var itemCount =
        getWeekIndicesCount(adjustedData.startDateAdjusted, adjustedData.endDateAdjusted)

    @RequiresApi(Build.VERSION_CODES.O)
    private val dataStore = DataStore { offset ->
        getWeekCalendarData(startDateAdjusted, offset, startDate, endDate).week
    }

    init {
        setHasStableIds(true)
    }

    private val isAttached: Boolean
        get() = calView.adapter === this

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        calView.post { notifyWeekScrollListenerIfNeeded() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getItem(position: Int): Week = dataStore[position]

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getItemId(position: Int): Long =
        getItem(position).days.first().date.hashCode().toLong()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getItemCount(): Int = itemCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val content = setupItemRoot(
            itemMargins = calView.weekMargins,
            daySize = calView.daySize,
            context = calView.context,
            dayViewResource = calView.dayViewResource,
            itemHeaderResource = calView.weekHeaderResource,
            itemFooterResource = calView.weekFooterResource,
            weekSize = 1,
            itemViewClass = calView.weekViewClass,
            dayBinder = calView.dayBinder as WeekDayBinder,
        )

        @Suppress("UNCHECKED_CAST")
        return WeekViewHolder(
            rootLayout = content.itemView,
            headerView = content.headerView,
            footerView = content.footerView,
            weekHolder = content.weekHolders.first(),
            weekHeaderBinder = calView.weekHeaderBinder as WeekHeaderFooterBinder<ViewContainer>?,
            weekFooterBinder = calView.weekFooterBinder as WeekHeaderFooterBinder<ViewContainer>?,
        )
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach {
                holder.reloadDay(it as WeekDay)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.bindWeek(getItem(position))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadDay(date: LocalDate) {
        val position = getAdapterPosition(date)
        if (position != NO_INDEX) {
            notifyItemChanged(position, dataStore[position].days.first { it.date == date })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadWeek(date: LocalDate) {
        notifyItemChanged(getAdapterPosition(date))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reloadCalendar() {
        notifyItemRangeChanged(0, itemCount)
    }

    private var visibleWeek: Week? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun notifyWeekScrollListenerIfNeeded() {
        if (!isAttached) return

        if (calView.isAnimating) {
            calView.itemAnimator?.isRunning {
                notifyWeekScrollListenerIfNeeded()
            }
            return
        }
        val visibleItemPos = findFirstVisibleWeekPosition()
        if (visibleItemPos != RecyclerView.NO_POSITION) {
            val visibleWeek = dataStore[visibleItemPos]

            if (visibleWeek != this.visibleWeek) {
                this.visibleWeek = visibleWeek
                calView.weekScrollListener?.invoke(visibleWeek)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun getAdapterPosition(date: LocalDate): Int {
        return getWeekIndex(startDateAdjusted, date)
    }

    private val layoutManager: WeekCalendarLayoutManager
        get() = calView.layoutManager as WeekCalendarLayoutManager

    @RequiresApi(Build.VERSION_CODES.O)
    fun findFirstVisibleWeek(): Week? {
        val index = findFirstVisibleWeekPosition()
        return if (index == NO_INDEX) null else dataStore[index]
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun findLastVisibleWeek(): Week? {
        val index = findLastVisibleWeekPosition()
        return if (index == NO_INDEX) null else dataStore[index]
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun findFirstVisibleDay(): WeekDay? = findVisibleDay(true)

    @RequiresApi(Build.VERSION_CODES.O)
    fun findLastVisibleDay(): WeekDay? = findVisibleDay(false)

    private fun findFirstVisibleWeekPosition(): Int = layoutManager.findFirstVisibleItemPosition()

    private fun findLastVisibleWeekPosition(): Int = layoutManager.findLastVisibleItemPosition()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun findVisibleDay(isFirst: Boolean): WeekDay? {
        val visibleIndex =
            if (isFirst) findFirstVisibleWeekPosition() else findLastVisibleWeekPosition()
        if (visibleIndex == NO_INDEX) return null

        val visibleItemView = layoutManager.findViewByPosition(visibleIndex) ?: return null
        val weekRect = Rect()
        visibleItemView.getGlobalVisibleRect(weekRect)

        val dayRect = Rect()
        return dataStore[visibleIndex].days
            .run { if (isFirst) this else reversed() }
            .firstOrNull {
                val dayView = visibleItemView.findViewWithTag<View>(dayTag(it.date))
                    ?: return@firstOrNull false
                dayView.getGlobalVisibleRect(dayRect)
                dayRect.intersect(weekRect)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    internal fun updateData(
        startDate: LocalDate,
        endDate: LocalDate,
        firstDayOfWeek: DayOfWeek,
    ) {
        this.startDate = startDate
        this.endDate = endDate
        this.firstDayOfWeek = firstDayOfWeek
        this.adjustedData = getWeekCalendarAdjustedRange(startDate, endDate, firstDayOfWeek)
        this.itemCount = getWeekIndicesCount(startDateAdjusted, endDateAdjusted)
        dataStore.clear()
        notifyDataSetChanged()
    }
}
