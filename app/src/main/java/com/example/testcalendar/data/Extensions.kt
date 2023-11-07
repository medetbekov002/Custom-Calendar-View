package com.example.testcalendar.data

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek

// E.g DayOfWeek.SATURDAY.daysUntil(DayOfWeek.TUESDAY) = 3
@RequiresApi(Build.VERSION_CODES.O)
fun DayOfWeek.daysUntil(other: DayOfWeek) = (7 + (other.value - value)) % 7
