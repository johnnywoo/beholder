package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TimeFormatter(private val format: Format, private val dateSource: TemplateFormatter?) : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        val date: ZonedDateTime?
        if (dateSource == null) {
            date = ZonedDateTime.now()
        } else {
            val dateString = dateSource.formatMessage(message).toString()
            date = parseDate(dateString)
        }

        if (date == null) {
            return invalidValue
        }
        return FieldValue.fromString(format.format(date))
    }

    companion object {
        private val invalidValue = FieldValue.fromString("invalid-date")

        private val dateDecodeRegexps = mapOf(
            // ISO 8601 full datetime = 2018-06-12T11:26:12+00:00
            "^\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(Z|[+-]\\d\\d:\\d\\d)$".toRegex() to "yyyy-MM-dd'T'HH:mm:ssXXX",
            // nginx access log date = 06/Jun/2018:15:19:48 +0300
            "^\\d?\\d/[A-Za-z]{3}/\\d{4}:\\d\\d:\\d\\d:\\d\\d (Z|[+-]\\d{4})$".toRegex() to "dd/MMM/yyyy:HH:mm:ss Z",

            // some others just in case
            "^\\d{8}$".toRegex() to "yyyyMMdd",
            "^\\d{1,2}-\\d{1,2}-\\d{4}$".toRegex() to "dd-MM-yyyy",
            "^\\d{4}-\\d{1,2}-\\d{1,2}$".toRegex() to "yyyy-MM-dd",
            "^\\d{1,2}/\\d{1,2}/\\d{4}$".toRegex() to "MM/dd/yyyy",
            "^\\d{4}/\\d{1,2}/\\d{1,2}$".toRegex() to "yyyy/MM/dd",
            "^\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}$".toRegex() to "dd MMM yyyy",
            "^\\d{1,2}\\s[A-Za-z]{4,}\\s\\d{4}$".toRegex() to "dd MMMM yyyy",
            "^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$".toRegex() to "dd-MM-yyyy HH:mm",
            "^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$".toRegex() to "yyyy-MM-dd HH:mm",
            "^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$".toRegex() to "MM/dd/yyyy HH:mm",
            "^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$".toRegex() to "yyyy/MM/dd HH:mm",
            "^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$".toRegex() to "dd MMM yyyy HH:mm",
            "^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$".toRegex() to "dd MMMM yyyy HH:mm",
            "^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$".toRegex() to "dd-MM-yyyy HH:mm:ss",
            "^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$".toRegex() to "yyyy-MM-dd HH:mm:ss",
            "^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$".toRegex() to "MM/dd/yyyy HH:mm:ss",
            "^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$".toRegex() to "yyyy/MM/dd HH:mm:ss",
            "^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$".toRegex() to "dd MMM yyyy HH:mm:ss",
            "^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$".toRegex() to "dd MMMM yyyy HH:mm:ss"
        )

        private val supportedFormats = mapOf(
            "time" to Format("HH:mm:ss"),
            "date" to Format("yyyy-MM-dd"),
            "datetime" to StableDatetimeFormat(),
            "unixtime-seconds" to UnixtimeFormat(),
            "unixtime-milliseconds" to UnixtimeMillisFormat(),
            "unixtime-microseconds" to UnixtimeMicrosFormat(),
            "unixtime-nanoseconds" to UnixtimeNanosFormat()
        )

        val FORMAT_TIME = supportedFormats["time"]!!
        val FORMAT_DATE = supportedFormats["date"]!!
        val FORMAT_STABLE_DATETIME = supportedFormats["datetime"]!!

        fun getNamedFormat(formatName: String): Format? {
            if (supportedFormats.containsKey(formatName)) {
                return supportedFormats[formatName]
            }
            return null
        }

        fun getSimpleDateFormat(formatString: String)
            = Format(formatString)

        fun parseDate(dateString: String?): ZonedDateTime? {
            if (dateString == null) {
                return null
            }
            for ((regex, dateFormat) in dateDecodeRegexps) {
                if (dateString.matches(regex)) {
                    try {
                        return ZonedDateTime.parse(dateString, DateTimeFormatter.ofPattern(dateFormat))
                    } catch (e: DateTimeParseException) {
                        return null
                    }
                }
            }
            return null
        }
    }

    open class Format(dateFormat: String) {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat)

        open fun format(date: ZonedDateTime): String {
            return date.format(dateTimeFormatter)
        }

        fun parse(date: String): ZonedDateTime {
            return ZonedDateTime.parse(date, dateTimeFormatter)
        }
    }

    private class StableDatetimeFormat : Format("yyyy-MM-dd'T'HH:mm:ssXXX") {
        override fun format(date: ZonedDateTime): String {
            return super.format(date).replace("Z$".toRegex(), "+00:00")
        }
    }

    private class UnixtimeFormat : Format("") {
        override fun format(date: ZonedDateTime): String {
            return date.toEpochSecond().toString()
        }
    }

    private class UnixtimeMillisFormat : Format("") {
        override fun format(date: ZonedDateTime): String {
            return (date.toEpochSecond() * 1000 + date.nano / 1_000_000).toString()
        }
    }

    private class UnixtimeMicrosFormat : Format("") {
        override fun format(date: ZonedDateTime): String {
            return (date.toEpochSecond() * 1_000_000 + date.nano / 1_000).toString()
        }
    }

    private class UnixtimeNanosFormat : Format("") {
        override fun format(date: ZonedDateTime): String {
            return (date.toEpochSecond() * 1_000_000_000 + date.nano).toString()
        }
    }
}
