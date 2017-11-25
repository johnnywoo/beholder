package ru.agalkin.beholder

data class Message(
    val text: String,
    val tags: MutableMap<String, String> = mutableMapOf<String, String>()
) {
    fun getPayload(): String {
        if (tags.containsKey("payload")) {
            return tags["payload"]!!
        }
        return text
    }
}
