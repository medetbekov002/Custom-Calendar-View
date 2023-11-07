package com.example.testcalendar.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.testcalendar.core.CalendarDay
import com.example.testcalendar.core.CalendarMonth
import com.example.testcalendar.core.DayPosition
import com.example.testcalendar.core.OutDateStyle
import com.example.testcalendar.core.atStartOfMonth
import com.example.testcalendar.core.nextMonth
import com.example.testcalendar.core.previousMonth
import com.example.testcalendar.core.yearMonth
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class MonthData internal constructor(
    private val month: YearMonth,
    private val inDays: Int,
    private val outDays: Int,
) {

    @RequiresApi(Build.VERSION_CODES.O)
    private val totalDays = inDays + month.lengthOfMonth() + outDays

    @RequiresApi(Build.VERSION_CODES.O)
    private val firstDay = month.atStartOfMonth().minusDays(inDays.toLong())

    @RequiresApi(Build.VERSION_CODES.O)
    private val rows = (0 until totalDays).chunked(7)

    @RequiresApi(Build.VERSION_CODES.O)
    private val previousMonth = month.previousMonth

    @RequiresApi(Build.VERSION_CODES.O)
    private val nextMonth = month.nextMonth

    @RequiresApi(Build.VERSION_CODES.O)
    val calendarMonth =
        CalendarMonth(month, rows.map { week -> week.map { dayOffset -> getDay(dayOffset) } })

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDay(dayOffset: Int): CalendarDay {
        val date = firstDay.plusDays(dayOffset.toLong())
        val position = when (date.yearMonth) {
            month -> DayPosition.MonthDate
            previousMonth -> DayPosition.InDate
            nextMonth -> DayPosition.OutDate
            else -> throw IllegalArgumentException("Invalid date: $date in month: $month")
        }
        return CalendarDay(date, position)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getCalendarMonthData(
    startMonth: YearMonth,
    offset: Int,
    firstDayOfWeek: DayOfWeek,
    outDateStyle: OutDateStyle,
): MonthData {
    val month = startMonth.plusMonths(offset.toLong())
    val firstDay = month.atStartOfMonth()
    val inDays = firstDayOfWeek.daysUntil(firstDay.dayOfWeek)
    val outDays = (inDays + month.lengthOfMonth()).let { inAndMonthDays ->
        val endOfRowDays = if (inAndMonthDays % 7 != 0) 7 - (inAndMonthDays % 7) else 0
        val endOfGridDays = if (outDateStyle == OutDateStyle.EndOfRow) 0 else run {
            val weeksInMonth = (inAndMonthDays + endOfRowDays) / 7
            return@run (6 - weeksInMonth) * 7
        }
        return@let endOfRowDays + endOfGridDays
    }
    return MonthData(month, inDays, outDays)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getMonthIndex(startMonth: YearMonth, targetMonth: YearMonth): Int {
    return ChronoUnit.MONTHS.between(startMonth, targetMonth).toInt()
}

@RequiresApi(Build.VERSION_CODES.O)
fun getMonthIndicesCount(startMonth: YearMonth, endMonth: YearMonth): Int {
    // Add one to include the start month itself!
    return getMonthIndex(startMonth, endMonth) + 1
}
