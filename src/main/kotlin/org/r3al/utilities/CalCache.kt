package org.r3al.utilities

import java.time.ZoneOffset
import java.util.*

object CalCache {
    private val calCache = ThreadLocal.withInitial { Calendar.getInstance() }
    private val calUtcCache = ThreadLocal.withInitial { Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)) }
    fun get(): Calendar {
        return calCache.get().clone() as Calendar
    }

    val uTC: Calendar
        get() = calUtcCache.get().clone() as Calendar
}
