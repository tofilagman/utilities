package org.r3al.utilities

import java.util.*

object DateHelper {
    const val APPLY_ON_FIRST_DAY = "FIRST"
    private const val APPLY_ON_LAST_DAY = "LAST"

    /**
     * Takes a calendar and eliminate the hours, minutes,
     * seconds and milliseconds by setting them to 0.
     *
     * @param cal - calendar to be truncated
     */
    fun truncateToDays(cal: Calendar?) {
        cal!![Calendar.MILLISECOND] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.HOUR_OF_DAY] = 0
    }

    /**
     * Formats a Calendar by setting the time to midnight
     *
     * @param dateIn
     * @return Calendar
     */
    fun getCalendarTruncatedToDay(dateIn: Date?): Calendar {
        val cal = calendar
        cal.time = dateIn
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal
    }
    /**
     * Truncate a date to midnight in the given timezone.
     *
     * @param d  A date, a null value will cause a null return.
     * @param tz A timezone, may be null to indicate the server timezone.
     * @return The truncated date, null only if null was given for a date.
     */
    /**
     * Transform a Date to Date.
     * Eliminate the hours, minutes, seconds and milliseconds by setting them to 0.
     *
     * @param d Date to be transformed
     * @return newly formatted Date
     */
    @JvmOverloads
    fun truncateToDays(d: Date, tz: TimeZone? = null): Date {
        val c = calendar
        if (tz != null) {
            c.timeZone = tz
        }
        c.time = d
        c[Calendar.MILLISECOND] = 0
        c[Calendar.SECOND] = 0
        c[Calendar.MINUTE] = 0
        c[Calendar.HOUR_OF_DAY] = 0
        return Date(c.timeInMillis)
    }

    val calendar: Calendar
        /**
         * Return a STALE Gregorian Calendar.
         * **Warning**: The returned Calendar does not reflect the current time (in general).
         */
        get() = CalCache.get()

    /**
     * Returns a date with the fields year, month, date, hour, and minute set.
     *
     * @param year  - value for the YEAR
     * @param month - value for the MONTH
     * @param day   - value for the DATE
     * @param hour  - value for the HOUR
     * @param min   - value for the MINUTE
     * @return
     */
    fun createDateTime(year: Int, month: Int, day: Int, hour: Int, min: Int): Date {
        val cal = Calendar.getInstance()
        cal.clear()
        cal[year, month, day, hour] = min
        return cal.time
    }

    /**
     * This method computes the end date of the month from the given date
     *
     * @param date - input date. Input date's month is considered for computation
     * @return end date of the month
     */
    fun getMonthEndDate(date: Date?): Date {
        val calendar = GregorianCalendar()
        calendar.time = date
        calendar[Calendar.DAY_OF_MONTH] = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return calendar.time
    }

    fun getLastDayOfYear(aDay: Date?): Date {
        return getUnitYear(APPLY_ON_LAST_DAY, false, aDay)
    }

    /**
     * returns the given applyOnValue day of the year for the given date
     * i.e. getUnitYear("10", false, 10/10/2002) will return 01/10/2002
     * i.e. getUnitYear("F", false, 10/10/2002) will return 01/01/2002
     *
     * @param applyOnValue what day of year, F,L or a number
     * @param previous     whether previous date is required
     * @param dateIn       date
     * @return Description of the Returned Value
     */
    fun getUnitYear(
        applyOnValue: String?, previous: Boolean,
        dateIn: Date?
    ): Date {
        val cal = getCalendarTruncatedToDay(dateIn)
        when (applyOnValue) {
            APPLY_ON_FIRST_DAY -> {
                setMinimum(cal, Calendar.MONTH)
                setMinimum(cal, Calendar.DAY_OF_MONTH)
            }
            APPLY_ON_LAST_DAY -> {
                setMaximum(cal, Calendar.MONTH)
                setMaximum(cal, Calendar.DAY_OF_MONTH)
            }
            else -> {
                setMinimum(cal, Calendar.MONTH)
                setMinimum(cal, Calendar.DAY_OF_MONTH)
                cal.add(Calendar.DAY_OF_YEAR, applyOnValue!!.toInt() - 1)
            }
        }
        if (previous) {
            cal.add(Calendar.YEAR, -1)
        }
        truncateToDays(cal)
        return Date(cal.timeInMillis)
    }

    /**
     * returns the given applyOnValue day of the quarter for the given date
     * i.e getUnitQtr("20", false, 10/10/2002) will return 10/20/2002
     * i.e getUnitQtr("F", false, 10/10/2002) will return 10/01/2002
     *
     * @param applyOnValue what day of quarter, F,L or a number
     * @param previous     whether previous date is required
     * @param dateIn       date
     * @return Description of the Returned Value
     */
    fun getUnitQtr(
        applyOnValue: String, previous: Boolean,
        dateIn: Date?
    ): Date {
        val cal = getCalendarTruncatedToDay(dateIn)
        if (previous) {
            cal.add(Calendar.MONTH, -3)
        }
        val month = cal[Calendar.MONTH]
        val qtr = month / 3
        when (applyOnValue) {
            APPLY_ON_FIRST_DAY -> {
                cal[Calendar.MONTH] = qtr * 3
                setMinimum(cal, Calendar.DAY_OF_MONTH)
            }
            APPLY_ON_LAST_DAY -> {
                setMinimum(cal, Calendar.DAY_OF_MONTH)
                cal[Calendar.MONTH] = (qtr + 1) * 3 - 1
                setMaximum(cal, Calendar.DAY_OF_MONTH)
            }
            else -> {
                cal[Calendar.MONTH] = qtr * 3
                setMinimum(cal, Calendar.DAY_OF_MONTH)
                cal.add(Calendar.DAY_OF_YEAR, applyOnValue.toInt() - 1)
            }
        }
        truncateToDays(cal)
        return Date(cal.timeInMillis)
    }

    /**
     * returns the given applyOnValue day of the month for the given date
     * i.e. getUnitMonth("20", false, 08/10/2002) will return 08/20/2002
     * i.e. getUnitMonth("F", false, 08/10/2002) will return 08/01/2002
     *
     * @param applyOnValue what day of month, F,L or a number
     * @param previous     whether previous date is required
     * @param dateIn       date
     * @return Description of the Returned Value
     */
    fun getUnitMonth(
        applyOnValue: String, previous: Boolean,
        dateIn: Date?
    ): Date {
        val cal = getCalendarTruncatedToDay(dateIn)
        if (previous) {
            cal.add(Calendar.MONTH, -1)
        }
        when (applyOnValue) {
            APPLY_ON_FIRST_DAY -> {
                setMinimum(cal, Calendar.DAY_OF_MONTH)
            }
            APPLY_ON_LAST_DAY -> {
                setMaximum(cal, Calendar.DAY_OF_MONTH)
            }
            else -> {
                setMinimum(cal, Calendar.DAY_OF_MONTH)
                cal.add(Calendar.DAY_OF_YEAR, applyOnValue.toInt() - 1)
            }
        }
        truncateToDays(cal)
        return Date(cal.timeInMillis)
    }

    /**
     * returns the given date, applyOnValue has no effect on this
     *
     * @param applyOnValue not used
     * @param previous     whether previous date is required
     * @param dateIn       date
     * @return Description of the Returned Value
     */
    fun getUnitDay(
        applyOnValue: String?, previous: Boolean,
        dateIn: Date?
    ): Date {
        val cal = getCalendarTruncatedToDay(dateIn)
        if (previous) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        truncateToDays(cal)
        return Date(cal.timeInMillis)
    }

    private fun setMinimum(cal: Calendar?, field: Int) {
        cal!![field] = cal.getActualMinimum(field)
    }

    private fun setMaximum(cal: Calendar?, field: Int) {
        cal!![field] = cal.getActualMaximum(field)
    }
}
