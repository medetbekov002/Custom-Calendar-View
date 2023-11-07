package com.example.testcalendar.view.internal.monthcalendar

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.testcalendar.core.CalendarDay
import com.example.testcalendar.view.CalendarView
import com.example.testcalendar.view.MarginValues
import com.example.testcalendar.view.internal.CalendarLayoutManager
import com.example.testcalendar.view.internal.dayTag
import java.time.YearMonth

internal class MonthCalendarLayoutManager(private val calView: CalendarView) :
    CalendarLayoutManager<YearMonth, CalendarDay>(calView, calView.orientation) {

    private val adapter: MonthCalendarAdapter
        get() = calView.adapter as MonthCalendarAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getaItemAdapterPosition(data: YearMonth): Int = adapter.getAdapterPosition(data)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getaDayAdapterPosition(data: CalendarDay): Int = adapter.getAdapterPosition(data)
    override fun getDayTag(data: CalendarDay): Int = dayTag(data.date)
    override fun getItemMargins(): MarginValues = calView.monthMargins
    override fun scrollPaged(): Boolean = calView.scrollPaged

    @RequiresApi(Build.VERSION_CODES.O)
    override fun notifyScrollListenerIfNeeded() = adapter.notifyMonthScrollListenerIfNeeded()
}
