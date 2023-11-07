package com.example.testcalendar.view.internal.weekcalendar

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.testcalendar.view.MarginValues
import com.example.testcalendar.view.WeekCalendarView
import com.example.testcalendar.view.internal.CalendarLayoutManager
import com.example.testcalendar.view.internal.dayTag
import com.example.testcalendar.view.internal.weekcalendar.WeekCalendarAdapter
import java.time.LocalDate

internal class WeekCalendarLayoutManager(private val calView: WeekCalendarView) :
    CalendarLayoutManager<LocalDate, LocalDate>(calView, HORIZONTAL) {

    private val adapter: WeekCalendarAdapter
        get() = calView.adapter as WeekCalendarAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getaItemAdapterPosition(data: LocalDate): Int = adapter.getAdapterPosition(data)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun getaDayAdapterPosition(data: LocalDate): Int = adapter.getAdapterPosition(data)
    override fun getDayTag(data: LocalDate): Int = dayTag(data)
    override fun getItemMargins(): MarginValues = calView.weekMargins
    override fun scrollPaged(): Boolean = calView.scrollPaged
    @RequiresApi(Build.VERSION_CODES.O)
    override fun notifyScrollListenerIfNeeded() = adapter.notifyWeekScrollListenerIfNeeded()
}
