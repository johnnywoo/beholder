package beholder.backend.api

open class Message(val action: String)

class EchoMessage(val data: String) : Message("echo")

class LoginMessage(val apiKey: String) : Message("login")
