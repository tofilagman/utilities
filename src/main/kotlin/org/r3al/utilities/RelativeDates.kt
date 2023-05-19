package org.r3al.utilities

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.zone.ZoneRules
import java.util.*
import java.util.regex.Pattern

/**
 * Contains a set of static methods to get different representations of a date
 * that is relative to a base date. Special syntax is being used to specify offset.
 * This syntax is somewhat similar to a Jira/Bugzilla relative date format.<br></br>
 *
 * Offset is a string:
 *
 * `<offset><datepart><inner offset>[ <hh>:<mm>]`
 *
 * where:
 *  * offset - number of dateparts to offset
 *  * detepart - min,d,w,m,y or a short name of a week day (Sun, Mon, Tue, etc.)
 *  * inner offset - number of days within current datepart
 *  * hh:mm - specific time to be attached to a date
 *
 *
 * Examples:
 *  * +60min - now + 60 minutes
 *  * +2d - today + 2
 *  * -1y - today a year back
 *  * -1Mon - last monday
 *  * +1m15 - the 15 of next month
 *  * -1mL - last of last month
 *  * +1d 17:00 - today + 1 at 17:00
 *  * +1SDST - the start DST date of next year
 *  * +1EDST - the end DST date of next year
 *
 */
object RelativeDates {
    private val daysOfWeek: MutableMap<String, Int> = HashMap()

    init {
        daysOfWeek["Sun"] = Calendar.SUNDAY
        daysOfWeek["Mon"] = Calendar.MONDAY
        daysOfWeek["Tue"] = Calendar.TUESDAY
        daysOfWeek["Wed"] = Calendar.WEDNESDAY
        daysOfWeek["Thu"] = Calendar.THURSDAY
        daysOfWeek["Fri"] = Calendar.FRIDAY
        daysOfWeek["Sat"] = Calendar.SATURDAY
    }

    private val dateClassMap: MutableMap<String, RelDate> = HashMap()

    init {
        dateClassMap["min"] = RelDateMin()
        dateClassMap["d"] = RelDateDay()
        dateClassMap["w"] = RelDateWeek()
        dateClassMap["m"] = RelDateMonth()
        dateClassMap["y"] = RelDateYear()
    }

    private var baseDate: Date = DateHelper.truncateToDays(Date())
    private const val TODAY = "today"
    private const val relDateRegex =
        "^([+-]\\d+)([dwmy]|Sun|Mon|Tue|Wed|Thu|Fri|Sat|SDST|EDST|min)([+-]\\d+[d])?(\\d{0,2}|[L]?)?\\s?(\\d{1,2}:\\d{2})?$"
    private val relDatePattern = Pattern.compile(relDateRegex)

    /**
     * This method should not be used in page objects or step implementations. It is implemented for solving some midnight issues only.
     */
    fun resetBaseDate() {
        baseDate = DateHelper.truncateToDays(Date())
    }

    /**
     * Converts a string representation of a date to a java.utils.Date. By default this returns a clean date. A time parameter can also be added to set a specific time.
     * @param strDate string representation of a date
     *
     * possible formats:
     *  * 20151231
     *  * 2015-12-31
     *  * relative date offset
     * @return parsed date
     */
    @Throws(ParseException::class)
    fun parseDate(strDate: String): Date {
        return if (strDate.matches("^\\d{8}$".toRegex())) {
            SimpleDateFormat("yyyyMMdd").parse(strDate)
        } else if (strDate.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$".toRegex())) {
            SimpleDateFormat("yyyy-MM-dd").parse(strDate)
        } else if (strDate.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$".toRegex())) {
            SimpleDateFormat("MM/dd/yyyy").parse(strDate)
        } else if (strDate.equals(TODAY, ignoreCase = true)) {
            baseDate
        } else if (relDatePattern.matcher(strDate).matches()) {
            parseDateFrom(strDate, null)
        } else if (strDate.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$".toRegex())) {
            SimpleDateFormat("yyyy-MM-dd HH:mm").parse(strDate)
        } else {
            throw IllegalArgumentException("The string $strDate could not be parsed to a date.")
        }
    }

    /**
     * Returns a java.util.Date object based on a base date and a relative date string.
     * @param strDate relative date offset
     * @param date base date. If null, base date provided by [RelDate.getBaseDate] will be used
     * @return parse date
     */
    fun parseDateFrom(strDate: String, date: Date?): Date {
        val match = relDatePattern.matcher(strDate)
        if (match.find()) {
            val offset = match.group(1)
            return if (match.group(2).matches("Sun|Mon|Tue|Wed|Thu|Fri|Sat".toRegex())) {
                val rd: RelDate = RelDateWeek()
                rd.getRelativeDate(
                    getCalendar(date, rd),
                    offset,
                    daysOfWeek[match.group(2)].toString(),
                    match.group(3),
                    match.group(5)
                )
            } else if (match.group(2).matches("SDST|EDST".toRegex())) {
                val rd: RelDate = RelDateDST()
                rd.getRelativeDate(getCalendar(date, rd), offset, match.group(2), match.group(3), match.group(5))
            } else {
                val rd = dateClassMap[match.group(2)]
                rd!!.getRelativeDate(
                    getCalendar(date, rd),
                    offset,
                    match.group(4),
                    match.group(3),
                    match.group(5)
                )
            }
        }
        throw IllegalArgumentException("The string $strDate could not be parsed to a date.")
    }

    private fun getCalendar(date: Date?, relDate: RelDate?): Calendar {
        val cal = DateHelper.calendar
        if (date != null) {
            cal.time = date
        } else {
            cal.time = relDate!!.getBaseDate()
        }
        return cal
    }

    fun isRelativeDate(strDate: String?): Boolean {
        return strDate?.let { relDatePattern.matcher(it).find() } ?: false
    }

    /**
     * Returns a base date for the current run.
     * Should be used instead of new Date() to have a consistent results for the duration of a run
     * @return Date when this class was loaded
     */
    fun today(): Date {
        return baseDate
    }

    private abstract class RelDate {
        abstract fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date

        fun setTime(cal: Calendar, time: String): Date {
            return DateHelper.createDateTime(
                cal[Calendar.YEAR],
                cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH],
                time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].toInt(),
                time.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].toInt())
        }

        open fun getBaseDate(): Date {
            return baseDate
        }
    }

    private class RelDateMin : RelDate() {
        override fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date {
            cal!!.add(Calendar.MINUTE, initOffset.toInt())
            return cal.time
        }

        override fun getBaseDate(): Date {
            return Date()
        }
    }

    private class RelDateDay : RelDate() {
        override fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date {
            cal!!.add(Calendar.DATE, initOffset.toInt())
            return if (timeOffset != null) {
                setTime(cal, timeOffset)
            } else cal.time
        }
    }

    private class RelDateWeek : RelDate() {
        override fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date {
            cal!!.add(Calendar.WEEK_OF_YEAR, initOffset.toInt())
            if (secOffset.matches("[1-7]".toRegex())) {
                cal[Calendar.DAY_OF_WEEK] = secOffset.toInt()
            }
            return if (timeOffset != null) setTime(cal, timeOffset) else cal.time
        }
    }

    private class RelDateMonth : RelDate() {
        override fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date {
            cal!!.add(Calendar.MONTH, initOffset.toInt())
            if (secOffset.equals("L", ignoreCase = true)) cal.time = DateHelper.getMonthEndDate(cal.time)
            if (secOffset.matches("\\d+".toRegex())) cal.time = DateHelper.getUnitMonth(secOffset, false, cal.time)
            return if (timeOffset != null) setTime(cal, timeOffset) else cal.time
        }
    }

    private class RelDateYear : RelDate() {
        override fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date {
            cal!!.add(Calendar.YEAR, initOffset.toInt())
            if (secOffset.equals("L", ignoreCase = true)) cal.time = DateHelper.getLastDayOfYear(cal.time)
            if (secOffset.matches("\\d+".toRegex())) cal.time = DateHelper.getUnitYear(secOffset, false, cal.time)
            return if (timeOffset != null) setTime(cal, timeOffset) else cal.time
        }
    }

    private class RelDateDST : RelDate() {
        override fun getRelativeDate(
            cal: Calendar?,
            initOffset: String,
            secOffset: String,
            relDateOffset: String?,
            timeOffset: String?
        ): Date {
            val relativeDate = parseDateFrom(initOffset + "y", cal!!.time)
            val localDateTime: LocalDateTime = when (secOffset) {
                "SDST" -> {
                    val beginDate = DateHelper.getUnitYear(DateHelper.APPLY_ON_FIRST_DAY, false, relativeDate)
                    getZoneRules(beginDate).nextTransition(beginDate.toInstant()).dateTimeAfter
                }

                "EDST" -> {
                    val endDate = DateHelper.getLastDayOfYear(relativeDate)
                    getZoneRules(endDate).previousTransition(endDate.toInstant()).dateTimeAfter
                }

                else ->  // Should never reach this point
                    throw IllegalArgumentException(String.format("The value '%s' is invalid.", secOffset))
            }
            cal.time = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
            val timeOffsetValue = timeOffset ?: "00:00"
            val relativeDateOffset = relDateOffset?.substring(0, relDateOffset.length - 1)?.toInt() ?: 0
            cal.add(Calendar.DATE, relativeDateOffset)
            return setTime(cal, timeOffsetValue)
        }

        private fun getZoneRules(baseDate: Date?): ZoneRules {
            val input = ZonedDateTime.ofInstant(
                baseDate!!.toInstant(),
                ZoneId.systemDefault()
            )
            return input.zone.rules
        }
    }
}