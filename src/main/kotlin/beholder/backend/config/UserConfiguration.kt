package beholder.backend.config

class UserConfiguration {
    transient var userName: String = ""
    var password: String = ""
    transient var apiKey: String = "" // do not de/serialize apiKey
}
