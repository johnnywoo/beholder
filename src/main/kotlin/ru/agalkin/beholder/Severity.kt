package ru.agalkin.beholder

enum class Severity(private val value: Int) {
    EMERGENCY(0),
    ALERT(1),
    CRITICAL(2),
    ERROR(3),
    WARNING(4),
    NOTICE(5),
    INFO(6),
    DEBUG(7);

    fun getNumberAsString()
        = value.toString()

    fun isMoreUrgentThan(x: Severity?)
        = x != null && x.value > value

    companion object {
        fun fromString(str: String?)
            = Severity.values().firstOrNull { it.getNumberAsString() == str }
    }
}
